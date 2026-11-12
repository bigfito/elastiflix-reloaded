"""Publicación del catálogo por cambio atómico de alias, con vuelta atrás (WP-00 T-10).

Uso real (turno 21, C-01 T-07):
    python data-quality/scripts/publicar.py
    python data-quality/scripts/publicar.py --revertir <índice anterior>
    python data-quality/scripts/publicar.py --borrar-anterior <índice>

Reglas (PLAN.md, WP-00):
- El modo real exige `es.aprobada("H-2")`; sin la aprobación termina sin hacer nada (ni lecturas de más).
- Antes de publicar comprueba que el índice detrás de `elastiflix-dq-staging` tenga 6 899 películas, que su mapping
  coincida con `elasticsearch/elastiflix-reloaded-mapping.json` (salvo `element_type: bfloat16`, que agrega
  Serverless) y que los tres vectores (`plot_256`, `plot_512`, `plot_1024`) estén en las 6 899.
- La publicación es **una** llamada atómica `POST /_aliases`: quita `elastiflix-reloaded` del índice actual, se lo
  pone al índice de staging y le quita a ese índice el alias `elastiflix-dq-staging`, para que una carga posterior
  en staging no escriba en producción.
- Escribe el nombre del índice anterior, que no se borra, en la consola y en la bitácora (§8, diario del plan).
- `--revertir <índice anterior>` deshace el cambio con la misma llamada atómica en sentido inverso.
- `--borrar-anterior <índice>` borra el índice anterior. Exige H-2 y que ningún alias apunte a ese índice. Es la
  única forma prevista de borrar un índice que no sea de staging.

Ensayo (este turno), sin tocar producción ni pedir H-2:
    python data-quality/scripts/publicar.py --ensayo-preparar
    python data-quality/scripts/publicar.py --ensayo
    python data-quality/scripts/publicar.py --ensayo --revertir <índice tiny>
    python data-quality/scripts/publicar.py --ensayo --borrar-anterior <índice tiny>
    python data-quality/scripts/publicar.py --ensayo-limpiar
  El ensayo usa los alias `elastiflix-dq-staging-prueba-a` (hace las veces de staging, sobre el índice real de
  staging, así que las comprobaciones de las 6 899 sí corren) y `elastiflix-dq-staging-prueba-b` (hace las veces
  de producción, sobre un índice tiny creado por `--ensayo-preparar`). Los alias de ensayo deben empezar con
  `elastiflix-dq-staging-prueba-` y todo índice involucrado con `elastiflix-dq-staging`: así el ensayo jamás puede
  tocar `elastiflix-reloaded`. El ensayo no escribe en la bitácora.

`es.py` rechaza `/_aliases` por diseño; este script hace sus propias llamadas con la configuración de `es.py` y con
las guardas de arriba. Nunca imprime la URL ni la API key.
"""
import argparse
import json
import pathlib
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone

sys.stdout.reconfigure(encoding="utf-8")
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import es
from paths import REPO

PRODUCCION = "elastiflix-reloaded"
STAGING = "elastiflix-dq-staging"
PRUEBA_A = "elastiflix-dq-staging-prueba-a"   # hace las veces de staging en el ensayo
PRUEBA_B = "elastiflix-dq-staging-prueba-b"   # hace las veces de producción en el ensayo
PREFIJO_PRUEBA = "elastiflix-dq-staging-prueba-"
PELICULAS = 6899
VECTORES = ("plot_256", "plot_512", "plot_1024")
MAPPING_PATH = REPO / "elasticsearch" / "elastiflix-reloaded-mapping.json"
BITACORA = REPO / "data-quality" / "BITACORA.md"


def llamar(metodo, path, body=None):
    """HTTP directo contra Elasticsearch (configuración de es.py), para lo que es.py rechaza por diseño."""
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(es.URL + path, data=data, method=metodo,
                                 headers={"Authorization": "ApiKey " + es.KEY,
                                          "Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, context=es.CTX, timeout=120) as r:
            return json.loads(r.read() or b"null")
    except urllib.error.HTTPError as e:
        return {"__error__": e.code, "body": e.read().decode()[:2000]}


def indice_detras(alias):
    """Índice único detrás de un alias; detiene el script si no hay exactamente uno."""
    r = es.es("GET", f"/_alias/{alias}")
    if isinstance(r, dict) and r.get("__error__"):
        sys.exit(f"El alias «{alias}» no existe ({r['__error__']}).")
    indices = list(r.keys())
    if len(indices) != 1:
        sys.exit(f"El alias «{alias}» apunta a {len(indices)} índices ({indices}); se esperaba exactamente uno.")
    return indices[0]


def conteo(objetivo):
    r = es.es("GET", f"/{objetivo}/_count")
    if isinstance(r, dict) and r.get("__error__"):
        sys.exit(f"No pude contar «{objetivo}» ({r['__error__']}): {r.get('body', '')[:200]}")
    return r["count"]


def normalizar_mapping(valor):
    """Normaliza un mapping para comparar: quita `element_type` (Serverless agrega `bfloat16` y el mapping del
    repo no lo lleva) y `"type": "object"` (Elasticsearch lo omite al devolver el mapping: es el valor por
    defecto de un objeto)."""
    if isinstance(valor, dict):
        return {k: normalizar_mapping(v) for k, v in valor.items()
                if k != "element_type" and not (k == "type" and v == "object")}
    if isinstance(valor, list):
        return [normalizar_mapping(v) for v in valor]
    return valor


def primera_diferencia(a, b, camino=""):
    """Primer camino donde dos estructuras difieren, para el mensaje de error del mapping."""
    if isinstance(a, dict) and isinstance(b, dict):
        for k in sorted(set(a) | set(b)):
            if k not in a:
                return f"{camino}.{k} (solo en el índice)"
            if k not in b:
                return f"{camino}.{k} (solo en el mapping del repo)"
            d = primera_diferencia(a[k], b[k], f"{camino}.{k}")
            if d:
                return d
        return None
    if a != b:
        return f"{camino} (repo={a!r} índice={b!r})"
    return None


def comprobar_staging(alias_origen):
    """Las tres comprobaciones de la especificación sobre el índice detrás del alias de origen."""
    indice = indice_detras(alias_origen)
    n = conteo(alias_origen)
    if n != PELICULAS:
        sys.exit(f"«{alias_origen}» tiene {n} películas, no {PELICULAS}. No se publica.")
    r = es.es("GET", f"/{indice}/_mapping")
    mapping_indice = normalizar_mapping(r[indice]["mappings"])
    mapping_repo = normalizar_mapping(json.loads(MAPPING_PATH.read_text(encoding="utf-8"))["mappings"])
    if mapping_indice != mapping_repo:
        sys.exit("El mapping del índice de staging no coincide con "
                 f"elasticsearch/elastiflix-reloaded-mapping.json: {primera_diferencia(mapping_repo, mapping_indice)}")
    for v in VECTORES:
        c = es.es("POST", f"/{alias_origen}/_count", {"query": {"exists": {"field": v}}})["count"]
        if c != PELICULAS:
            sys.exit(f"El vector {v} está en {c} de {PELICULAS} películas. No se publica.")
    print(f"Comprobaciones en verde: {PELICULAS} películas, mapping fiel (salvo element_type: bfloat16) "
          f"y los tres vectores en todas.")
    return indice


def cambio_atomico(acciones):
    """La única escritura del modo real/ensayo: una sola llamada POST /_aliases."""
    r = llamar("POST", "/_aliases", {"actions": acciones})
    if not (isinstance(r, dict) and r.get("acknowledged")):
        sys.exit(f"POST /_aliases no fue reconocido: {str(r)[:400]}. Revisa los alias antes de reintentar.")


def publicar(alias_origen, alias_destino, ensayo):
    indice_origen = comprobar_staging(alias_origen)
    indice_anterior = indice_detras(alias_destino)
    if indice_anterior == indice_origen:
        sys.exit(f"«{alias_destino}» ya apunta al índice de origen ({indice_origen}); nada que hacer.")
    cambio_atomico([
        {"remove": {"index": indice_anterior, "alias": alias_destino}},
        {"add": {"index": indice_origen, "alias": alias_destino}},
        {"remove": {"index": indice_origen, "alias": alias_origen}},
    ])
    if indice_detras(alias_destino) != indice_origen:
        sys.exit(f"La verificación posterior falló: «{alias_destino}» no apunta a {indice_origen}.")
    print(f"Publicado: «{alias_destino}» ahora apunta a {indice_origen} (antes {indice_anterior}).")
    print(f"Índice anterior (no se borra): {indice_anterior}")
    if not ensayo:
        fecha = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M")
        with BITACORA.open("a", encoding="utf-8", newline="\n") as f:
            f.write(f"\n### {fecha} UTC · publicar.py · publicación\n"
                    f"- `{PRODUCCION}` apuntaba a `{indice_anterior}` y ahora apunta a `{indice_origen}`. "
                    f"El índice anterior no se borra: se conserva 7 días y después se elimina con "
                    f"`publicar.py --borrar-anterior {indice_anterior}`.\n")
        print("El índice anterior quedó anotado en BITACORA.md §8.")
    return indice_anterior


def revertir(alias_origen, alias_destino, indice_anterior, ensayo):
    indice_actual = indice_detras(alias_destino)
    cambio_atomico([
        {"remove": {"index": indice_actual, "alias": alias_destino}},
        {"add": {"index": indice_anterior, "alias": alias_destino}},
        {"add": {"index": indice_actual, "alias": alias_origen}},
    ])
    if indice_detras(alias_destino) != indice_anterior:
        sys.exit(f"La verificación posterior falló: «{alias_destino}» no apunta a {indice_anterior}.")
    print(f"Revertido: «{alias_destino}» vuelve a {indice_anterior} y «{alias_origen}» a {indice_actual}.")
    if not ensayo:
        print("Anota la vuelta atrás en BITACORA.md §8 y abre un bloqueo para la dueña del proyecto.")


def borrar_anterior(indice, ensayo):
    aliases = es.es("GET", f"/{indice}/_alias")
    if isinstance(aliases, dict) and aliases.get("__error__"):
        sys.exit(f"El índice «{indice}» no existe ({aliases['__error__']}).")
    activos = list(aliases.get(indice, {}).get("aliases", {}).keys())
    if activos:
        sys.exit(f"No se borra «{indice}»: los alias {activos} apuntan a él.")
    r = llamar("DELETE", f"/{indice}")
    if not (isinstance(r, dict) and r.get("acknowledged")):
        sys.exit(f"DELETE /{indice} no fue reconocido: {str(r)[:400]}")
    print(f"Borrado el índice anterior {indice}.")


def ensayo_preparar():
    """Crea el índice tiny de `prueba-b` y pone `prueba-a` sobre el índice real de staging."""
    indice_staging = indice_detras(STAGING)
    tiny = f"{PRUEBA_B}-{datetime.now(timezone.utc).strftime('%Y%m%d-%H%M%S')}"
    r = llamar("PUT", f"/{tiny}", {"mappings": {"properties": {"id": {"type": "long"}}}})
    if not (isinstance(r, dict) and r.get("acknowledged")):
        sys.exit(f"No pude crear el índice de ensayo {tiny}: {str(r)[:400]}")
    cambio_atomico([
        {"add": {"index": indice_staging, "alias": PRUEBA_A}},
        {"add": {"index": tiny, "alias": PRUEBA_B}},
    ])
    print(f"Ensayo listo: {PRUEBA_A} → {indice_staging} (staging real) y {PRUEBA_B} → {tiny} (índice tiny).")
    print(f"El «índice anterior» del ensayo es {tiny}.")


def ensayo_limpiar():
    """Quita los alias de ensayo de donde estén y borra todo índice elastiflix-dq-staging-prueba-*."""
    acciones = []
    for alias in (PRUEBA_A, PRUEBA_B):
        r = es.es("GET", f"/_alias/{alias}")
        if isinstance(r, dict) and r.get("__error__"):
            continue
        for indice in r.keys():
            acciones.append({"remove": {"index": indice, "alias": alias}})
    if acciones:
        cambio_atomico(acciones)
    r = es.es("GET", f"/{PREFIJO_PRUEBA}*")
    borrados = []
    if isinstance(r, dict) and not r.get("__error__"):
        for indice in r.keys():
            llamar("DELETE", f"/{indice}")
            borrados.append(indice)
    print(f"Ensayo limpio: alias de ensayo quitados; índices borrados: {borrados or 'ninguno'}.")


def main():
    p = argparse.ArgumentParser(
        description="Publica el catálogo por cambio atómico de alias, con vuelta atrás (exige H-2 salvo ensayo).")
    p.add_argument("--revertir", metavar="ÍNDICE", help="deshace la publicación devolviendo el alias al índice")
    p.add_argument("--borrar-anterior", metavar="ÍNDICE",
                   help="borra el índice anterior si ningún alias apunta a él")
    p.add_argument("--ensayo", action="store_true",
                   help="usa los alias elastiflix-dq-staging-prueba-a/b; nunca producción ni H-2 ni bitácora")
    p.add_argument("--ensayo-preparar", action="store_true",
                   help="crea el índice tiny de prueba-b y apunta prueba-a al índice real de staging")
    p.add_argument("--ensayo-limpiar", action="store_true",
                   help="quita los alias de ensayo y borra los índices elastiflix-dq-staging-prueba-*")
    a = p.parse_args()
    acciones = sum(bool(x) for x in (a.revertir, a.borrar_anterior, a.ensayo_preparar, a.ensayo_limpiar))
    if acciones > 1:
        sys.exit("Elige una sola acción: publicar (sin opciones), --revertir, --borrar-anterior, "
                 "--ensayo-preparar o --ensayo-limpiar.")
    if a.ensayo_preparar or a.ensayo_limpiar:
        if a.ensayo:
            sys.exit("--ensayo-preparar y --ensayo-limpiar ya son de ensayo; no combines con --ensayo.")
        if a.ensayo_preparar:
            ensayo_preparar()
        else:
            ensayo_limpiar()
        return

    ensayo = a.ensayo
    alias_origen = PRUEBA_A if ensayo else STAGING
    alias_destino = PRUEBA_B if ensayo else PRODUCCION
    if ensayo:
        for nombre in (alias_origen, alias_destino, a.revertir or "", a.borrar_anterior or ""):
            if nombre and not nombre.startswith(PREFIJO_PRUEBA.rstrip("-")):
                sys.exit(f"En modo ensayo todo alias o índice debe empezar con «{PREFIJO_PRUEBA.rstrip('-')}»: {nombre}.")
    elif not es.aprobada("H-2"):
        sys.exit("H-2 no está aprobada. publicar.py termina sin hacer nada (AGENTS.md §3).")

    if a.revertir:
        revertir(alias_origen, alias_destino, a.revertir, ensayo)
    elif a.borrar_anterior:
        borrar_anterior(a.borrar_anterior, ensayo)
    else:
        publicar(alias_origen, alias_destino, ensayo)


if __name__ == "__main__":
    main()
