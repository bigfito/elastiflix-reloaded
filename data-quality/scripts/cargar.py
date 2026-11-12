"""Subida e importación de las partes del catálogo contra la aplicación (WP-00 T-06).

Uso:
    python data-quality/scripts/cargar.py --alias elastiflix-dq-staging --modo bulk data/elastiflix-reloaded_01.json
    python data-quality/scripts/cargar.py --alias elastiflix-dq-staging --modo append data/elastiflix-reloaded_02.json ...

Flujo (PLAN.md, WP-00):
- Antes de subir nada, `GET /actuator/health` de la instancia (`--base`, por defecto http://localhost:8081). El
  alias real está en `components.elastiflixCatalog.details.alias`. La respuesta puede ser 503 (`DOWN`) si el alias
  todavía no existe; aun así se lee el alias de ahí.
- Se detiene si el alias de la instancia no es `--alias` o si es `elastiflix-reloaded`: este script **nunca**
  carga producción (AGENTS.md §3).
- Por archivo: `POST /cargar/archivo` (multipart `archivo`, `modo`; un `.csv` además lleva `encabezado=true` y
  `separador=,`) devuelve `jobId`; `GET /cargar/trabajos/{jobId}` cada 5 s hasta `job.status = valido`;
  `POST /cargar/trabajos/{jobId}/importar`; espera `publicado` (bulk) o `agregado` (append). Termina con error en
  `invalido`, `fallido` o `cancelado`, a los 10 minutos de validación o a los 90 de importación.
- Al final escribe en la consola el alias, el índice, el conteo y el commit de `data/` cargado, para copiarlos a la
  línea «Staging» de la bitácora.
"""
import argparse
import json
import pathlib
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid

sys.stdout.reconfigure(encoding="utf-8")
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from paths import REPO

PRODUCCION = "elastiflix-reloaded"
ESPERA_S = 5          # segundos entre consultas de estado
T_VALIDACION_S = 600  # 10 minutos
T_IMPORTACION_S = 5400  # 90 minutos
MALOS = {"invalido", "fallido", "cancelado"}


def peticion(metodo, url, data=None, cabeceras=None, timeout=120):
    """HTTP mínimo: devuelve (código, cuerpo); un 4xx/5xx no lanza."""
    req = urllib.request.Request(url, data=data, method=metodo, headers=cabeceras or {})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()


def salud(base):
    """`GET /actuator/health`; valen 200 (UP) y 503 (DOWN, si el alias todavía no existe)."""
    try:
        code, body = peticion("GET", base + "/actuator/health", timeout=30)
    except urllib.error.URLError as e:
        sys.exit(f"No responde {base}/actuator/health ({e.reason}). "
                 "¿La app está arriba con ELASTIFLIX_INDEX_ALIAS y SERVER_PORT correctos?")
    if code not in (200, 503):
        sys.exit(f"GET /actuator/health devolvió {code}; se esperaba 200 o 503.")
    try:
        return json.loads(body)
    except json.JSONDecodeError:
        sys.exit("La respuesta de /actuator/health no es JSON.")


def detalles(h):
    try:
        return h["components"]["elastiflixCatalog"]["details"]
    except (KeyError, TypeError):
        sys.exit("No encontré components.elastiflixCatalog.details.alias en /actuator/health.")


def comprobar_alias(base, alias):
    """Detiene el script si la instancia no es la pedida o si cualquiera de los dos alias es producción."""
    real = detalles(salud(base)).get("alias")
    if real == PRODUCCION or alias == PRODUCCION:
        sys.exit(f"Alto: el alias «{real if real == PRODUCCION else alias}» es producción. "
                 "cargar.py nunca carga producción (AGENTS.md §3).")
    if real != alias:
        sys.exit(f"Alto: la instancia en {base} usa el alias «{real}» y pediste «{alias}». "
                 f"Levanta la app con ELASTIFLIX_INDEX_ALIAS={alias} y SERVER_PORT explícitos.")


def multipart(archivo, modo):
    """Cuerpo multipart/form-data con `archivo`, `modo` y, en CSV, `encabezado=true` y `separador=,`."""
    boundary = "dq-cargar-" + uuid.uuid4().hex
    es_csv = archivo.suffix.lower() == ".csv"
    campos = [("modo", modo)]
    if es_csv:
        campos += [("encabezado", "true"), ("separador", ",")]
    partes = []
    for nombre, valor in campos:
        partes += [f"--{boundary}",
                   f'Content-Disposition: form-data; name="{nombre}"',
                   "",
                   valor]
    partes += [f"--{boundary}",
               f'Content-Disposition: form-data; name="archivo"; filename="{archivo.name}"',
               f"Content-Type: {'text/csv' if es_csv else 'application/json'}",
               ""]
    cuerpo = "\r\n".join(partes).encode("utf-8") + b"\r\n" + archivo.read_bytes()
    cuerpo += f"\r\n--{boundary}--\r\n".encode("utf-8")
    return cuerpo, boundary


def subir(base, archivo, modo):
    """`POST /cargar/archivo`; devuelve el jobId o detiene el script con el código de rechazo."""
    cuerpo, boundary = multipart(archivo, modo)
    code, body = peticion("POST", base + "/cargar/archivo", data=cuerpo,
                          cabeceras={"Content-Type": f"multipart/form-data; boundary={boundary}"},
                          timeout=600)
    if code != 200:
        sys.exit(f"La subida de {archivo.name} fue rechazada ({code}): {body.decode('utf-8', 'replace')[:500]}")
    return json.loads(body)["jobId"]


def trabajo(base, job_id):
    code, body = peticion("GET", f"{base}/cargar/trabajos/{job_id}", timeout=30)
    if code != 200:
        sys.exit(f"GET /cargar/trabajos/{job_id} devolvió {code}: {body.decode('utf-8', 'replace')[:300]}")
    return json.loads(body)["job"]


def esperar(base, job_id, objetivo, limite_s, fase):
    """Consulta el trabajo cada 5 s hasta el estado `objetivo`; error en un estado malo o al agotar el tiempo."""
    inicio = time.monotonic()
    ultimo = None
    while True:
        job = trabajo(base, job_id)
        status = job["status"]
        if status == objetivo:
            return job
        if status in MALOS:
            detalle = job.get("failureMessage") or ""
            sys.exit(f"El trabajo {job_id} terminó en «{status}» durante la {fase}. {detalle} "
                     f"Informe: GET {base}/cargar/trabajos/{job_id}/informe.csv")
        if time.monotonic() - inicio > limite_s:
            sys.exit(f"Tiempo agotado ({limite_s // 60} min) esperando «{objetivo}» en el trabajo {job_id}; "
                     f"último estado «{status}».")
        avance = (f"{status}: lote {job['batch']}/{job['batches']}, "
                  f"vectorizados {job['vectorized']}, escritos {job['written']}")
        if avance != ultimo:
            print(f"  … {avance}", flush=True)
            ultimo = avance
        time.sleep(ESPERA_S)


def commit_data():
    """Commit corto del checkout: identifica la versión de `data/` que se cargó."""
    try:
        r = subprocess.run(["git", "-C", str(REPO), "rev-parse", "--short", "HEAD"],
                           capture_output=True, text=True, timeout=30)
        return r.stdout.strip() if r.returncode == 0 else "desconocido"
    except (OSError, subprocess.SubprocessError):
        return "desconocido"


def main():
    p = argparse.ArgumentParser(
        description="Sube partes del catálogo a la aplicación y dispara la importación (nunca a producción).")
    p.add_argument("--base", default="http://localhost:8081",
                   help="URL de la instancia (por defecto http://localhost:8081)")
    p.add_argument("--alias", required=True,
                   help="alias que la instancia debe tener configurado; sin valor por defecto")
    p.add_argument("--modo", choices=["bulk", "append"], required=True,
                   help="bulk reemplaza el catálogo; append agrega (o reemplaza ids)")
    p.add_argument("archivos", nargs="+", type=pathlib.Path, help="partes .json o .csv, en orden")
    a = p.parse_args()
    if a.alias == PRODUCCION:
        sys.exit(f"Alto: «{PRODUCCION}» es producción. cargar.py nunca carga producción (AGENTS.md §3).")
    base = a.base.rstrip("/")
    for f in a.archivos:
        if not f.is_file():
            sys.exit(f"No existe el archivo {f}.")
        if f.suffix.lower() not in (".json", ".csv"):
            sys.exit(f"Solo se aceptan partes .json o .csv: {f}.")
    comprobar_alias(base, a.alias)
    for f in a.archivos:
        print(f"Subiendo {f.name} ({f.stat().st_size / 1e6:.1f} MB, modo {a.modo})…", flush=True)
        job_id = subir(base, f, a.modo)
        print(f"  trabajo {job_id}: validando…", flush=True)
        esperar(base, job_id, "valido", T_VALIDACION_S, "validación")
        code, body = peticion("POST", f"{base}/cargar/trabajos/{job_id}/importar", timeout=60)
        if code != 200:
            sys.exit(f"No se pudo importar el trabajo {job_id} ({code}): {body.decode('utf-8', 'replace')[:300]}")
        objetivo = "publicado" if a.modo == "bulk" else "agregado"
        print(f"  importando (hasta «{objetivo}»)…", flush=True)
        job = esperar(base, job_id, objetivo, T_IMPORTACION_S, "importación")
        print(f"  {objetivo}: {job['validCount']} registros válidos de {job['fileCount']}.", flush=True)
    d = detalles(salud(base))
    print()
    print(f"Carga completa: alias={a.alias} índice={d.get('index')} "
          f"documentos={d.get('documents')} commit={commit_data()}")


if __name__ == "__main__":
    main()
