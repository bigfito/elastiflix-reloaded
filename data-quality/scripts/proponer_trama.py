"""Propone el siguiente lote de tramas según la norma de sinopsis (WP-00 T-09).

No modifica `data/` y no aplica el lote. Escribe `lote-NNN.jsonl` y las filas
`propuesto`, `sin fuente` u `omitido` de `estado.tsv`. El hash `antes_sha256`
sale de `catalogo.sha()` y el valor actual de `catalogo.obtener_campo()`; cada
valor nuevo pasa por `catalogo.validar_cambio()` antes de escribirse.

Ejemplo de prueba (no toca `data/`):

    python data-quality/scripts/proponer_trama.py --wp WP-02 ^
        --lista data-quality/hallazgos/listas/wp02-ensayos.tsv ^
        --prioridad data-quality/hallazgos/listas/wp02-prioridad.tsv ^
        --tam 5 --destino data-quality/work/prueba_trama ^
        --agente "Cursor (Grok 4.7)"

En un turno de datos, sin `--destino`, el lote queda en `data-quality/cambios/<WP>/`.
Quien ejecute ese turno pasa `--agente` con su nombre o define `DQ_AGENTE`; sin
ninguno de los dos, el script no escribe nada (revisión R-02).

En WP-05, una película sin trama de Wikipedia queda `omitido` con el motivo
«pendiente de limpiar_enciclopedico.py»: ese script (turno 10) toma esas filas.
"""
import argparse
import csv
import json
import os
import pathlib
import re
import sys
from datetime import datetime, timezone
from typing import Any, NamedTuple

sys.stdout.reconfigure(encoding="utf-8", line_buffering=True)
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import catalogo
import wiki_plot
from aplicar_cambios import CAMPOS_LOTE
from paths import DQ, REPO, WORK

COLUMNAS_ESTADO = ["id", "lote", "resultado", "motivo"]
WPS_TMDB = frozenset({"WP-01", "WP-02"})
MOTIVOS = {
    "WP-01": "respuesta de asistente o dato inventado",
    "WP-02": "ensayo generado",
    "WP-05": "plot enciclopédico",
    "WP-06": "sinopsis corta",
}


class Decision(NamedTuple):
    resultado: str
    motivo: str
    plot: str | None
    etiqueta: str | None
    fuente: str | None


def _exigir(condicion: bool, mensaje: str) -> None:
    if not condicion:
        raise AssertionError(mensaje)


def _motivo(wp: str) -> str:
    return MOTIVOS.get(wp, "reemplazo de plot según NS-1")


def _largo(valor: Any) -> int:
    return len(valor) if isinstance(valor, str) else 0


def decidir(
    plot_actual: Any,
    trama_wiki: str | None,
    fuente_wiki: str | None,
    overview: Any,
    *,
    wp: str,
    solo_si_mas_largo: bool,
    fecha: str,
) -> Decision:
    """Elige Wikipedia, el overview de TMDB o ninguna fuente, según el WP."""
    if trama_wiki:
        if (solo_si_mas_largo or wp == "WP-06") and len(trama_wiki) <= _largo(plot_actual):
            return Decision("omitido", "la trama de Wikipedia no es más larga que el plot actual", None, None, None)
        return Decision("propuesto", _motivo(wp), trama_wiki, f"wikipedia_plot_{fecha}", fuente_wiki)

    if wp == "WP-05" and not solo_si_mas_largo:
        return Decision("omitido", "sin trama de Wikipedia; pendiente de limpiar_enciclopedico.py", None, None, None)

    if wp in WPS_TMDB and not solo_si_mas_largo:
        texto = wiki_plot.preparar_texto(overview if isinstance(overview, str) else None)
        if texto:
            return Decision(
                "propuesto",
                "overview de TMDB (NS-1b); sin trama de Wikipedia",
                texto,
                "tmdb_overview",
                "tmdb:overview",
            )
        return Decision("sin fuente", "sin trama de Wikipedia ni overview de TMDB de 40 caracteres o más", None, None, None)

    return Decision("sin fuente", "sin trama de Wikipedia (NS-1a)", None, None, None)


def resolver_ruta(valor: str) -> pathlib.Path:
    ruta = pathlib.Path(valor)
    if ruta.is_file():
        return ruta
    desde_repo = REPO / valor
    if desde_repo.is_file():
        return desde_repo
    raise FileNotFoundError(f"No existe la lista: {valor}")


def leer_ids(ruta: pathlib.Path) -> list[str]:
    ids: list[str] = []
    for linea in ruta.read_text(encoding="utf-8-sig").splitlines():
        texto = linea.strip()
        if not texto or texto.startswith("#"):
            continue
        ids.append(texto.split("\t", 1)[0].strip())
    return ids


def leer_estado(ruta: pathlib.Path) -> list[dict[str, str]]:
    if not ruta.is_file():
        return []
    filas: list[dict[str, str]] = []
    lector = csv.DictReader(ruta.read_text(encoding="utf-8-sig").splitlines(), delimiter="\t")
    for fila in lector:
        filas.append({col: str(fila.get(col) or "") for col in COLUMNAS_ESTADO})
    return filas


def _esta_dentro(ruta: pathlib.Path, raiz: pathlib.Path) -> bool:
    try:
        ruta.resolve().relative_to(raiz.resolve())
        return True
    except ValueError:
        return False


def siguiente_lote(destino: pathlib.Path, filas: list[dict[str, str]]) -> str:
    numeros: set[int] = set()
    if destino.is_dir():
        for archivo in destino.glob("lote-*.jsonl"):
            hallado = re.fullmatch(r"lote-(\d+)", archivo.stem)
            if hallado:
                numeros.add(int(hallado.group(1)))
    for fila in filas:
        hallado = re.fullmatch(r"lote-(\d+)", fila["lote"])
        if hallado:
            numeros.add(int(hallado.group(1)))
    return f"lote-{max(numeros, default=0) + 1:03d}"


def seleccionar(lista: list[str], prioridad: list[str], ya: set[str], tam: int) -> list[str]:
    alcance = set(lista)
    orden: list[str] = []
    vistos: set[str] = set()
    for doc_id in prioridad + lista:
        if doc_id not in alcance or doc_id in vistos or doc_id in ya:
            continue
        vistos.add(doc_id)
        orden.append(doc_id)
        if len(orden) >= tam:
            break
    return orden


def _linea(wp: str, lote: str, doc_id: str, campo: str, doc: dict[str, Any], nuevo: Any, fuente: str, motivo: str, agente: str, fecha: str) -> dict[str, Any]:
    catalogo.validar_cambio(campo, nuevo)
    actual = catalogo.obtener_campo(doc, campo)
    return {
        "wp": wp,
        "lote": lote,
        "id": doc_id,
        "campo": campo,
        "antes_sha256": catalogo.sha(actual),
        "nuevo": nuevo,
        "fuente": fuente,
        "motivo": motivo,
        "agente": agente,
        "fecha": fecha,
    }


def _escribir(ruta: pathlib.Path, contenido: str) -> None:
    ruta.parent.mkdir(parents=True, exist_ok=True)
    temporal = ruta.with_suffix(ruta.suffix + ".tmp")
    temporal.write_text(contenido, encoding="utf-8", newline="\n")
    temporal.replace(ruta)


def _motivo_fila(decision: Decision, detalle: str) -> str:
    texto = decision.motivo if decision.resultado == "propuesto" or not detalle else f"{decision.motivo}. {detalle}"
    return texto.replace("\t", " ").replace("\n", " ")[:400]


def _ruta_fuentes(destino: pathlib.Path) -> pathlib.Path:
    if _esta_dentro(destino, WORK):
        return destino / "plot-fuentes.tsv"
    return DQ / "fuentes" / "plot-fuentes.tsv"


def articulos_usados(ruta: pathlib.Path) -> dict[str, str]:
    """`{título del artículo: id}` de las sinopsis de Wikipedia ya registradas en `plot-fuentes.tsv`."""
    usados: dict[str, str] = {}
    if ruta.is_file():
        for linea in ruta.read_text(encoding="utf-8-sig").splitlines()[1:]:
            partes = linea.split("\t")
            if len(partes) >= 2 and partes[0]:
                usados.setdefault(partes[1], partes[0])
    return usados


def _agregar_fuentes(ruta: pathlib.Path, filas_nuevas: list[tuple[str, str, str, str]]) -> None:
    if not filas_nuevas:
        return
    existentes: set[tuple[str, str]] = set()
    previas: list[str] = []
    if ruta.is_file():
        for linea in ruta.read_text(encoding="utf-8-sig").splitlines():
            previas.append(linea)
            partes = linea.split("\t")
            if len(partes) >= 3 and partes[0] != "id":
                existentes.add((partes[0], partes[2]))
    else:
        previas = ["id\ttitulo\trevid\tfecha"]
    for doc_id, titulo, revid, fecha in filas_nuevas:
        if (doc_id, revid) in existentes:
            continue
        titulo_limpio = titulo.replace("\t", " ").replace("\n", " ")
        previas.append(f"{doc_id}\t{titulo_limpio}\t{revid}\t{fecha}")
    _escribir(ruta, "\n".join(previas) + "\n")


def proponer(
    wp: str,
    lista: pathlib.Path,
    prioridad: pathlib.Path | None,
    tam: int,
    destino: pathlib.Path,
    agente: str,
    solo_si_mas_largo: bool,
) -> int:
    if _esta_dentro(destino, catalogo.DATA_DIR):
        raise ValueError("proponer_trama.py no escribe dentro de data/.")
    if not re.fullmatch(r"WP-\d+", wp):
        raise ValueError(f"WP inválido: {wp}")
    if tam < 1:
        raise ValueError("--tam tiene que ser 1 o más")

    docs = catalogo.cargar()
    ids_lista = leer_ids(lista)
    ids_prioridad = leer_ids(prioridad) if prioridad else []
    ruta_estado = destino / "estado.tsv"
    filas = leer_estado(ruta_estado)
    ya = {fila["id"] for fila in filas}
    elegidos = seleccionar(ids_lista, ids_prioridad, ya, tam)
    if not elegidos:
        print("No quedan películas del alcance sin fila en estado.tsv.")
        return 0

    faltan = [doc_id for doc_id in elegidos if doc_id not in docs]
    if faltan:
        raise KeyError(f"Estas películas están en la lista y no en el catálogo: {', '.join(faltan[:8])}")

    fecha = datetime.now(timezone.utc).date().isoformat()
    lote = siguiente_lote(destino, filas)
    detalles: dict[str, str] = {}
    articulos = wiki_plot.buscar(elegidos, docs, detalles)

    lineas: list[dict[str, Any]] = []
    nuevas_filas: list[dict[str, str]] = []
    fuentes: list[tuple[str, str, str, str]] = []
    cuentas = {"propuesto": 0, "sin fuente": 0, "omitido": 0}
    # Un artículo de Wikipedia es de una sola película: si ya lo usa otra, no se repite (R-10, Red Cliff).
    usados = articulos_usados(_ruta_fuentes(destino))

    for doc_id in elegidos:
        doc = docs[doc_id]
        titulo = articulos.get(doc_id)
        resultado = wiki_plot.trama(titulo, wiki_plot._anio(doc)) if titulo else None
        if resultado is not None and usados.get(resultado.titulo, doc_id) != doc_id:
            detalles[doc_id] = f"el artículo {resultado.titulo} ya es la fuente de {usados[resultado.titulo]}"
            print(f"{doc_id}: {detalles[doc_id]}; no se repite", file=sys.stderr)
            resultado = None
        if resultado is not None:
            usados[resultado.titulo] = doc_id
        if resultado is not None and wiki_plot.cita_visible(resultado.texto):
            raise RuntimeError(f"{doc_id}: la trama todavía tiene referencias y no se escribe el lote.")
        fuente_wiki = f"enwiki:{resultado.titulo}#revid={resultado.revid}" if resultado else None
        decision = decidir(
            catalogo.obtener_campo(doc, "plot"),
            resultado.texto if resultado else None,
            fuente_wiki,
            catalogo.obtener_campo(doc, "overview"),
            wp=wp,
            solo_si_mas_largo=solo_si_mas_largo,
            fecha=fecha,
        )
        if decision.resultado == "propuesto":
            if not decision.plot or not decision.etiqueta or not decision.fuente:
                raise RuntimeError(f"{doc_id}: la propuesta no trae plot, etiqueta y fuente.")
            if wiki_plot.cita_visible(decision.plot):
                raise RuntimeError(f"{doc_id}: el texto propuesto todavía tiene referencias.")
            for campo, nuevo in (("plot", decision.plot), ("enrichment.plot", decision.etiqueta)):
                lineas.append(_linea(wp, lote, doc_id, campo, doc, nuevo, decision.fuente, decision.motivo, agente, fecha))
            if resultado is not None and decision.etiqueta.startswith("wikipedia_plot_"):
                fuentes.append((doc_id, resultado.titulo, str(resultado.revid), fecha))
        cuentas[decision.resultado] = cuentas.get(decision.resultado, 0) + 1
        nuevas_filas.append(
            {"id": doc_id, "lote": lote, "resultado": decision.resultado, "motivo": _motivo_fila(decision, detalles.get(doc_id, ""))}
        )
        origen = decision.fuente or "sin cambio"
        print(f"{doc_id} {doc.get('title')}: {decision.resultado} · {origen} · {decision.motivo}")

    if lineas:
        contenido = "\n".join(json.dumps({clave: linea[clave] for clave in CAMPOS_LOTE}, ensure_ascii=False) for linea in lineas) + "\n"
        _escribir(destino / f"{lote}.jsonl", contenido)
        _agregar_fuentes(_ruta_fuentes(destino), fuentes)
    else:
        print(f"{lote}: sin líneas de cambio; no se escribe un jsonl vacío.")

    salida = ["\t".join(COLUMNAS_ESTADO)]
    for fila in filas + nuevas_filas:
        salida.append("\t".join(fila[col] for col in COLUMNAS_ESTADO))
    _escribir(ruta_estado, "\n".join(salida) + "\n")

    print(
        f"{lote}: {cuentas['propuesto']} propuestas ({len(lineas)} líneas), "
        f"{cuentas['sin fuente']} sin fuente, {cuentas['omitido']} omitidas."
    )
    print(f"Destino: {destino}")
    print("data/ no se modificó.")
    return 0


MOTIVO_D06 = "D-06: texto generado sin trama de Wikipedia que la limpieza no arregla; overview de TMDB (NS-1b)"


def decidir_d06(plot_actual: Any, etiqueta_actual: Any, overview: Any) -> Decision:
    """WP-05 T-06 (D-06): el overview de TMDB reemplaza un texto generado, salvo que ya haya trama de Wikipedia."""
    if isinstance(etiqueta_actual, str) and etiqueta_actual.startswith("wikipedia_plot_"):
        return Decision("omitido", "D-06: ya tiene trama de Wikipedia; no se toca", None, None, None)
    texto = wiki_plot.preparar_texto(overview if isinstance(overview, str) else None)
    if not texto:
        return Decision("sin fuente", "D-06: sin overview de TMDB de 40 caracteres o más", None, None, None)
    if texto == plot_actual:
        return Decision("omitido", "D-06: el plot ya es el overview de TMDB", None, None, None)
    return Decision("propuesto", MOTIVO_D06, texto, "tmdb_overview", "tmdb:overview")


def proponer_d06(lista: pathlib.Path, destino: pathlib.Path, agente: str) -> int:
    """Un lote de WP-05 con el overview de TMDB para cada película de `wp05-d06-generadas.tsv` (R-12, P-16).

    A diferencia de `proponer()`, toma también películas que ya tienen fila en `estado.tsv` (las de la lista ya
    pasaron por WP-05) y la reemplaza por la del lote nuevo. Una película que, cuando se corre, ya tiene trama de
    Wikipedia queda igual.
    """
    if lista.name != "wp05-d06-generadas.tsv":
        raise ValueError("--d06 solo acepta la lista data-quality/hallazgos/listas/wp05-d06-generadas.tsv")
    docs = catalogo.cargar()
    ruta_estado = destino / "estado.tsv"
    filas = leer_estado(ruta_estado)
    elegidos = list(dict.fromkeys(leer_ids(lista)))
    faltan = [doc_id for doc_id in elegidos if doc_id not in docs]
    if faltan:
        raise KeyError(f"Estas películas están en la lista y no en el catálogo: {', '.join(faltan[:8])}")
    fecha = datetime.now(timezone.utc).date().isoformat()
    lote = siguiente_lote(destino, filas)
    lineas: list[dict[str, Any]] = []
    nuevas: dict[str, dict[str, str]] = {}
    cuentas = {"propuesto": 0, "sin fuente": 0, "omitido": 0}
    for doc_id in elegidos:
        doc = docs[doc_id]
        decision = decidir_d06(
            catalogo.obtener_campo(doc, "plot"), catalogo.obtener_campo(doc, "enrichment.plot"), catalogo.obtener_campo(doc, "overview")
        )
        if decision.resultado == "propuesto":
            assert decision.plot and decision.etiqueta and decision.fuente
            for campo, nuevo in (("plot", decision.plot), ("enrichment.plot", decision.etiqueta)):
                lineas.append(_linea("WP-05", lote, doc_id, campo, doc, nuevo, decision.fuente, decision.motivo, agente, fecha))
        cuentas[decision.resultado] += 1
        if decision.resultado != "omitido":
            nuevas[doc_id] = {"id": doc_id, "lote": lote, "resultado": decision.resultado, "motivo": decision.motivo}
        print(f"{doc_id} {doc.get('title')}: {decision.resultado} · {decision.motivo}")
    if lineas:
        contenido = "\n".join(json.dumps({clave: linea[clave] for clave in CAMPOS_LOTE}, ensure_ascii=False) for linea in lineas) + "\n"
        _escribir(destino / f"{lote}.jsonl", contenido)
    else:
        print(f"{lote}: sin líneas de cambio; no se escribe un jsonl vacío.")
    salida = ["\t".join(COLUMNAS_ESTADO)]
    for fila in filas:
        if fila["id"] not in nuevas:
            salida.append("\t".join(fila[col] for col in COLUMNAS_ESTADO))
    for fila in nuevas.values():
        salida.append("\t".join(fila[col] for col in COLUMNAS_ESTADO))
    _escribir(ruta_estado, "\n".join(salida) + "\n")
    print(
        f"{lote}: {cuentas['propuesto']} propuestas ({len(lineas)} líneas), "
        f"{cuentas['sin fuente']} sin fuente, {cuentas['omitido']} omitidas."
    )
    print(f"Destino: {destino}")
    print("data/ no se modificó.")
    return 0


def comprobar() -> None:
    """Reglas NS sin red y sin escribir `data/`."""
    wiki_plot.comprobar_texto()
    doc = {"plot": "hola", "enrichment": None}
    _exigir(catalogo.obtener_campo(doc, "plot") == "hola", "obtener_campo(plot)")
    _exigir(catalogo.obtener_campo(doc, "enrichment.plot") is None, "un enrichment ausente vale None")
    _exigir(catalogo.sha(None) == catalogo.sha(catalogo.obtener_campo(doc, "enrichment.plot")), "sha(None) es el hash del campo ausente")
    catalogo.validar_cambio("plot", "x" * 40)
    catalogo.validar_cambio("enrichment.plot", "wikipedia_plot_2026-11-12")
    catalogo.validar_cambio("enrichment.plot", "tmdb_overview")
    for campo, valor in (("plt", "x"), ("enrichment.plot", 123), ("enrichment.no_existe", "x")):
        try:
            catalogo.validar_cambio(campo, valor)
        except ValueError:
            continue
        raise AssertionError(f"validar_cambio aceptó {campo}={valor!r}")

    fecha = "2026-11-12"
    fuente = "enwiki:Example#revid=1"
    wiki = "W" * 80
    corto = "C" * 40
    overview = "O" * 50

    decision = decidir("actual", wiki, fuente, overview, wp="WP-02", solo_si_mas_largo=False, fecha=fecha)
    _exigir(decision.resultado == "propuesto" and decision.etiqueta == f"wikipedia_plot_{fecha}" and decision.plot == wiki, "WP-02 debe usar Wikipedia")
    decision = decidir("actual", None, None, overview, wp="WP-01", solo_si_mas_largo=False, fecha=fecha)
    _exigir(decision.resultado == "propuesto" and decision.etiqueta == "tmdb_overview" and decision.plot == overview, "WP-01 cae al overview")
    decision = decidir("actual", None, None, "corto", wp="WP-02", solo_si_mas_largo=False, fecha=fecha)
    _exigir(decision.resultado == "sin fuente", "Un overview de menos de 40 caracteres no sirve")
    decision = decidir(wiki, corto, fuente, overview, wp="WP-06", solo_si_mas_largo=True, fecha=fecha)
    _exigir(decision.resultado == "omitido", "WP-06 no reemplaza una trama que no es más larga")
    decision = decidir(corto, wiki, fuente, overview, wp="WP-06", solo_si_mas_largo=False, fecha=fecha)
    _exigir(decision.resultado == "propuesto" and decision.plot == wiki, "WP-06 reemplaza cuando la trama es más larga")
    decision = decidir(corto, None, None, overview, wp="WP-06", solo_si_mas_largo=True, fecha=fecha)
    _exigir(decision.resultado == "sin fuente" and decision.etiqueta is None, "WP-06 no usa el overview de TMDB")
    decision = decidir("actual", None, None, overview, wp="WP-05", solo_si_mas_largo=False, fecha=fecha)
    _exigir(decision.resultado == "omitido" and "limpiar_enciclopedico" in decision.motivo, "WP-05 sin trama queda para la limpieza")
    decision = decidir("actual", wiki, fuente, None, wp="WP-05", solo_si_mas_largo=False, fecha=fecha)
    _exigir(decision.resultado == "propuesto" and decision.plot == wiki, "WP-05 usa Wikipedia cuando existe")
    decision = decidir_d06("ensayo", "unverified", overview)
    _exigir(decision.resultado == "propuesto" and decision.etiqueta == "tmdb_overview" and decision.plot == overview, "D-06 usa el overview")
    decision = decidir_d06("trama", "wikipedia_plot_2026-11-13", overview)
    _exigir(decision.resultado == "omitido", "D-06 no toca una trama de Wikipedia")
    decision = decidir_d06("ensayo", "unverified", "corto")
    _exigir(decision.resultado == "sin fuente", "D-06 sin overview de 40 caracteres queda sin fuente")
    tsv = WORK / "prueba_usados.tsv"
    tsv.write_text("id\ttitulo\trevid\tfecha\n12289\tRed Cliff (film)\t1\t2026-11-12\n", encoding="utf-8")
    _exigir(articulos_usados(tsv) == {"Red Cliff (film)": "12289"}, "articulos_usados lee plot-fuentes.tsv")
    tsv.unlink()
    print("Reglas de proponer_trama y de catalogo.sha/obtener_campo/validar_cambio: OK.")


def main() -> None:
    parser = argparse.ArgumentParser(description="Propone un lote de tramas (NS) sin modificar data/.")
    parser.add_argument("--wp", help="Paquete, por ejemplo WP-02.")
    parser.add_argument("--lista", help="TSV del alcance: la primera columna es el id.")
    parser.add_argument("--prioridad", help="TSV opcional. Sus ids se proponen primero.")
    parser.add_argument("--tam", type=int, default=100, help="Máximo de películas de este lote (por defecto 100).")
    parser.add_argument("--solo-si-mas-largo", action="store_true", help="WP-06: solo NS-1(a), y solo si la trama nueva es más larga.")
    parser.add_argument("--destino", type=pathlib.Path, help="Carpeta del lote y de estado.tsv. Por defecto data-quality/cambios/<WP>.")
    parser.add_argument("--agente", default=os.environ.get("DQ_AGENTE"), help="Quién propone el lote, por ejemplo «Cursor (Grok 4.7)». Obligatorio, o la variable DQ_AGENTE.")
    parser.add_argument("--comprobar", action="store_true", help="Prueba las reglas NS sin red y sin escribir data/.")
    parser.add_argument("--d06", action="store_true", help="WP-05 T-06: overview de TMDB para la lista wp05-d06-generadas.tsv (D-06).")
    args = parser.parse_args()

    try:
        if args.comprobar:
            comprobar()
            return
        if args.d06:
            if args.wp != "WP-05" or not args.lista:
                parser.error("--d06 va con --wp WP-05 y --lista data-quality/hallazgos/listas/wp05-d06-generadas.tsv")
            if not args.agente or not args.agente.strip():
                parser.error("hace falta --agente (o la variable DQ_AGENTE)")
            sys.exit(proponer_d06(resolver_ruta(args.lista), args.destino or (DQ / "cambios" / "WP-05"), args.agente))
        if not args.wp or not args.lista:
            parser.error("hacen falta --wp y --lista (o --comprobar)")
        if not args.agente or not args.agente.strip():
            parser.error("hace falta --agente (o la variable DQ_AGENTE): el lote registra quién lo propuso")
        destino = args.destino or (DQ / "cambios" / args.wp)
        sys.exit(
            proponer(
                args.wp,
                resolver_ruta(args.lista),
                resolver_ruta(args.prioridad) if args.prioridad else None,
                args.tam,
                destino,
                args.agente,
                args.solo_si_mas_largo,
            )
        )
    except (AssertionError, RuntimeError, KeyError, ValueError, FileNotFoundError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
