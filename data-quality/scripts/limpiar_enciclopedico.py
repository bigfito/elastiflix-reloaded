"""Limpia el plot enciclopédico cuando no hay sección de trama en Wikipedia (WP-05 T-01).

Especificación (`PLAN.md` · WP-05 T-01):
- `limpiar(texto) -> texto | None`:
  1. Divide el texto en párrafos completos (separados por línea en blanco `\\n\\n`). Nunca corta dentro de un párrafo.
  2. Quita el **primer párrafo** si es la ficha (`^…\\bis an? (\\d{4} )?…(film|movie|documentary|anime)\\b`) y quedan
     400 caracteres o más.
  3. Quita **párrafos completos** que contengan `box office`, `grossed`, `Rotten Tomatoes`,
     `received (mixed|positive|negative) reviews` o `premiered at`.
  4. Si la limpieza deja menos de 400 caracteres o no cambia ningún párrafo, devuelve `None` (no se cambia nada y la
     película queda `sin fuente` en `estado.tsv`).
- La etiqueta `enrichment.plot` sigue en `unverified` (solo se propone cambio sobre `plot`).
- CLI `--wp WP-05 [--ids <tsv>] [--tam 100] [--destino <dir>] --agente "<nombre>"`:
  Toma las películas de `estado.tsv` que quedaron en `omitido` con `pendiente de limpiar_enciclopedico.py` (o los ids de
  `--ids`) y escribe el lote `lote-NNN.jsonl` y actualiza `estado.tsv`.
"""
import argparse
import csv
import json
import os
import pathlib
import re
import sys
from datetime import datetime, timezone
from typing import Any

sys.stdout.reconfigure(encoding="utf-8", line_buffering=True)
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import catalogo
from aplicar_cambios import CAMPOS_LOTE
from paths import DQ, WORK
from proponer_trama import (
    COLUMNAS_ESTADO,
    _escribir,
    _esta_dentro,
    _exigir,
    _linea,
    leer_estado,
    leer_ids,
    resolver_ruta,
    siguiente_lote,
)

MIN_CARACTERES = 400

RE_FICHA = re.compile(
    r"^[^\n]{0,250}\bis an? (?:\d{4} )?[^\n]*\b(?:film|movie|documentary|anime)\b",
    re.IGNORECASE,
)

RE_TAQUILLA_CRITICA = re.compile(
    r"\b(?:box[- ]office|grossed|rotten tomatoes|received (?:mixed|positive|negative) reviews|premiered at)\b",
    re.IGNORECASE,
)

MOTIVO_PROPUESTO = "limpieza de plot enciclopédico (ficha/taquilla/crítica)"
MOTIVO_SIN_FUENTE_CORTO = "sin trama de Wikipedia; la limpieza enciclopédica deja menos de 400 caracteres"
MOTIVO_SIN_FUENTE_IGUAL = "sin trama de Wikipedia; sin párrafos de ficha, taquilla o crítica que limpiar"
FUENTE_LIMPIEZA = "limpiar_enciclopedico"

IDS_PRUEBA_WORK = ("1593", "1597", "1646", "1830", "1844")


def dividir_parrafos(texto: str) -> list[str]:
    """Divide `texto` en párrafos completos separados por una o más líneas en blanco."""
    return [p.strip() for p in re.split(r"\n\s*\n", texto.strip()) if p.strip()]


def es_ficha_inicial(parrafo: str) -> bool:
    """Indica si el primer párrafo corresponde a la ficha enciclopédica de la película."""
    return bool(RE_FICHA.search(parrafo.strip()))


def es_taquilla_o_critica(parrafo: str) -> bool:
    """Indica si el párrafo contiene datos de taquilla, estreno o recepción crítica."""
    return bool(RE_TAQUILLA_CRITICA.search(parrafo))


def limpiar(texto: str | None) -> str | None:
    """Quita el primer párrafo si es la ficha y los párrafos completos de taquilla o crítica.

    Nunca corta dentro de un párrafo. Devuelve `None` si el texto resultante tiene menos
    de 400 caracteres o si no cambia respecto del original.
    """
    if not isinstance(texto, str):
        return None
    original = texto.strip()
    if not original:
        return None

    parrafos = dividir_parrafos(original)
    if not parrafos:
        return None

    # 1. Quitar el primer párrafo si es la ficha y quedan 400 caracteres o más
    if es_ficha_inicial(parrafos[0]):
        resto_sin_ficha = "\n\n".join(parrafos[1:])
        if len(resto_sin_ficha) >= MIN_CARACTERES:
            parrafos = parrafos[1:]

    # 2. Quitar párrafos completos de taquilla o crítica
    parrafos = [p for p in parrafos if not es_taquilla_o_critica(p)]

    limpio = "\n\n".join(parrafos)
    if len(limpio) < MIN_CARACTERES or limpio == original:
        return None
    return limpio


def _motivo_sin_fuente(texto: str | None) -> str:
    if not isinstance(texto, str) or not texto.strip():
        return MOTIVO_SIN_FUENTE_CORTO
    parrafos = dividir_parrafos(texto)
    tiene_ficha = bool(parrafos and es_ficha_inicial(parrafos[0]))
    tiene_taquilla = any(es_taquilla_o_critica(p) for p in parrafos)
    if tiene_ficha or tiene_taquilla:
        return MOTIVO_SIN_FUENTE_CORTO
    return MOTIVO_SIN_FUENTE_IGUAL


def _es_pendiente_limpieza(fila: dict[str, str]) -> bool:
    return fila.get("resultado") == "omitido" and "limpiar_enciclopedico" in (fila.get("motivo") or "")


def seleccionar_para_limpiar(
    filas_estado: list[dict[str, str]],
    ids_archivo: list[str] | None,
    tam: int,
) -> list[str]:
    """Selecciona hasta `tam` películas para limpiar.

    - Si en `estado.tsv` hay filas `omitido` con `pendiente de limpiar_enciclopedico.py`
      (filtradas por `ids_archivo` cuando se proporciona), toma esas en orden.
    - Si `ids_archivo` se proporciona y no hay filas pendientes en `estado.tsv` para esos ids,
      toma los ids de `ids_archivo` que aún no tengan fila en `estado.tsv` (o estén `omitido`).
    """
    conjunto_ids = set(ids_archivo) if ids_archivo is not None else None
    pendientes: list[str] = []
    vistos: set[str] = set()

    for fila in filas_estado:
        doc_id = fila["id"]
        if doc_id in vistos:
            continue
        if _es_pendiente_limpieza(fila) and (conjunto_ids is None or doc_id in conjunto_ids):
            vistos.add(doc_id)
            pendientes.append(doc_id)
            if len(pendientes) >= tam:
                return pendientes

    if ids_archivo is not None and not pendientes:
        ya_finalizados = {
            fila["id"]
            for fila in filas_estado
            if not _es_pendiente_limpieza(fila)
        }
        for doc_id in ids_archivo:
            if doc_id in vistos or doc_id in ya_finalizados:
                continue
            vistos.add(doc_id)
            pendientes.append(doc_id)
            if len(pendientes) >= tam:
                break

    return pendientes


def proponer(
    wp: str,
    ruta_ids: pathlib.Path | None,
    tam: int,
    destino: pathlib.Path,
    agente: str,
) -> int:
    """Propone un lote de limpieza enciclopédica sin modificar `data/`."""
    if _esta_dentro(destino, catalogo.DATA_DIR):
        raise ValueError("limpiar_enciclopedico.py no escribe dentro de data/.")
    if not re.fullmatch(r"WP-\d+", wp):
        raise ValueError(f"WP inválido: {wp}")
    if tam < 1:
        raise ValueError("--tam tiene que ser 1 o más")

    docs = catalogo.cargar()
    ruta_estado = destino / "estado.tsv"
    filas = leer_estado(ruta_estado)

    ids_archivo: list[str] | None = None
    if ruta_ids is not None and ruta_ids.resolve() != ruta_estado.resolve():
        ids_archivo = [i for i in leer_ids(ruta_ids) if i != "id"]

    elegidos = seleccionar_para_limpiar(filas, ids_archivo, tam)
    if not elegidos:
        print("No quedan películas pendientes de limpiar_enciclopedico.py.")
        return 0

    faltan = [doc_id for doc_id in elegidos if doc_id not in docs]
    if faltan:
        raise KeyError(f"Estas películas están en la lista y no en el catálogo: {', '.join(faltan[:8])}")

    fecha = datetime.now(timezone.utc).date().isoformat()
    lote = siguiente_lote(destino, filas)

    lineas: list[dict[str, Any]] = []
    actualizaciones: dict[str, dict[str, str]] = {}
    cuentas = {"propuesto": 0, "sin fuente": 0}

    for doc_id in elegidos:
        doc = docs[doc_id]
        plot_actual = catalogo.obtener_campo(doc, "plot")
        nuevo_plot = limpiar(plot_actual if isinstance(plot_actual, str) else None)
        if nuevo_plot is not None:
            lineas.append(
                _linea(
                    wp,
                    lote,
                    doc_id,
                    "plot",
                    doc,
                    nuevo_plot,
                    FUENTE_LIMPIEZA,
                    MOTIVO_PROPUESTO,
                    agente,
                    fecha,
                )
            )
            cuentas["propuesto"] += 1
            actualizaciones[doc_id] = {
                "id": doc_id,
                "lote": lote,
                "resultado": "propuesto",
                "motivo": MOTIVO_PROPUESTO,
            }
            print(
                f"{doc_id} {doc.get('title')}: propuesto · {FUENTE_LIMPIEZA} "
                f"({len(plot_actual)} -> {len(nuevo_plot)} chars)"
            )
        else:
            motivo_sf = _motivo_sin_fuente(plot_actual if isinstance(plot_actual, str) else None)
            cuentas["sin fuente"] += 1
            actualizaciones[doc_id] = {
                "id": doc_id,
                "lote": lote,
                "resultado": "sin fuente",
                "motivo": motivo_sf,
            }
            print(f"{doc_id} {doc.get('title')}: sin fuente · sin cambio · {motivo_sf}")

    if lineas:
        contenido = (
            "\n".join(
                json.dumps({clave: linea[clave] for clave in CAMPOS_LOTE}, ensure_ascii=False)
                for linea in lineas
            )
            + "\n"
        )
        _escribir(destino / f"{lote}.jsonl", contenido)
    else:
        print(f"{lote}: sin líneas de cambio; no se escribe un jsonl vacío.")

    # Actualizar en el sitio las filas existentes en estado.tsv o agregar al final si no estaban
    ya_en_estado: set[str] = set()
    filas_salida: list[dict[str, str]] = []
    for fila in filas:
        doc_id = fila["id"]
        if doc_id in actualizaciones and doc_id not in ya_en_estado:
            filas_salida.append(actualizaciones[doc_id])
            ya_en_estado.add(doc_id)
        else:
            filas_salida.append(fila)
    for doc_id in elegidos:
        if doc_id not in ya_en_estado:
            filas_salida.append(actualizaciones[doc_id])
            ya_en_estado.add(doc_id)

    salida = ["\t".join(COLUMNAS_ESTADO)]
    for fila in filas_salida:
        salida.append("\t".join(fila[col] for col in COLUMNAS_ESTADO))
    _escribir(ruta_estado, "\n".join(salida) + "\n")

    print(
        f"{lote}: {cuentas['propuesto']} propuestas ({len(lineas)} líneas), "
        f"{cuentas['sin fuente']} sin fuente."
    )
    print(f"Destino: {destino}")
    print("data/ no se modificó.")
    return 0


def _catalogo_en(etiqueta: str) -> dict[str, dict[str, Any]]:
    """Catálogo tal como estaba en una etiqueta de git (solo lectura; no toca data/)."""
    import subprocess
    import tempfile

    tmp = pathlib.Path(tempfile.mkdtemp(prefix="dq-catalogo-"))
    for base in catalogo.PARTES:
        contenido = subprocess.run(["git", "-C", str(catalogo.REPO), "show", f"{etiqueta}:data/{base}.json"],
                                   capture_output=True, check=True).stdout
        (tmp / f"{base}.json").write_bytes(contenido)
    return catalogo.cargar(tmp)


def comprobar() -> None:
    """Ejecuta pruebas unitarias de `limpiar()` y la prueba sobre 5 sinopsis reales en `work/` (WP-05 T-01)."""
    # 1. Pruebas unitarias de `limpiar(texto)`
    p_ficha = "Example Movie is a 2005 American action thriller film directed by Jane Doe."
    p_trama_larga = (
        "In a quiet coastal town, a former detective investigates the disappearance of a cargo ship "
        "carrying experimental medical equipment. As he follows the trail of falsified manifests, he "
        "uncovers a smuggling ring operating out of the old shipyard and forms an uneasy alliance "
        "with a journalist whose brother vanished on the same vessel.\n\n"
        "Together they infiltrate the harbor warehouse at night, recover the captain's logbook and "
        "expose the port commissioner before the conspirators can destroy the evidence, bringing the "
        "missing crew members safely home."
    )
    p_taquilla = "The film grossed $120 million at the box office and received positive reviews on Rotten Tomatoes."
    p_estreno = "It premiered at the Cannes Film Festival on May 15, 2005."

    texto_completo = f"{p_ficha}\n\n{p_trama_larga}\n\n{p_taquilla}\n\n{p_estreno}"
    resultado = limpiar(texto_completo)
    _exigir(resultado == p_trama_larga, "Debe quitar la ficha inicial y los párrafos de taquilla y estreno")
    _exigir(len(resultado) >= MIN_CARACTERES, "El resultado limpio debe tener al menos 400 caracteres")

    # Si tras limpiar quedan menos de 400 caracteres, devuelve None
    p_trama_corta = "A short plot paragraph of less than four hundred characters."
    _exigir(limpiar(f"{p_ficha}\n\n{p_trama_corta}\n\n{p_taquilla}") is None, "Menos de 400 caracteres devuelve None")

    # Si no hay nada que quitar, devuelve None
    _exigir(limpiar(p_trama_larga) is None, "Un texto sin ficha ni taquilla no se modifica (devuelve None)")

    # Nunca corta dentro de un párrafo
    for parrafo in dividir_parrafos(resultado):
        _exigir(parrafo in dividir_parrafos(texto_completo), "Nunca debe cortar dentro de un párrafo")

    # 2a. limpiar() sobre 5 sinopsis reales tal como estaban al empezar el plan (etiqueta dq-r00), que no cambian
    #     aunque WP-02 o WP-05 ya hayan reemplazado esas películas en data/ (R-10).
    originales = _catalogo_en("dq-r00")
    for doc_id in IDS_PRUEBA_WORK:
        original = originales[doc_id]["plot"]
        nuevo = limpiar(original)
        _exigir(nuevo is not None, f"{doc_id}: la sinopsis original de dq-r00 debía tener algo que limpiar")
        _exigir(len(nuevo) >= MIN_CARACTERES, f"{doc_id}: el texto limpio debe tener >= 400 caracteres")
        _exigir(not es_ficha_inicial(dividir_parrafos(nuevo)[0]), f"{doc_id}: no debe quedar ficha inicial")
        _exigir(not any(es_taquilla_o_critica(p) for p in dividir_parrafos(nuevo)),
                f"{doc_id}: no deben quedar párrafos de taquilla o crítica")
        _exigir(all(p in dividir_parrafos(original) for p in dividir_parrafos(nuevo)),
                f"{doc_id}: cada párrafo conservado debe ser un párrafo íntegro del original")

    # 2b. Flujo completo en work/prueba_limpiar/ con 5 películas que hoy siguen en unverified y tienen algo que limpiar
    actuales = catalogo.cargar()
    candidatas = [i for i, d in actuales.items()
                  if (d.get("enrichment") or {}).get("plot") == "unverified" and limpiar(d["plot"]) is not None]
    if len(candidatas) < 5:
        print(f"Prueba de flujo omitida: solo quedan {len(candidatas)} películas unverified con algo que limpiar.")
        print("Prueba de limpiar_enciclopedico.py (unitaria + 5 sinopsis reales de dq-r00): OK.")
        return
    ids_flujo = tuple(sorted(candidatas, key=int)[:5])
    destino_prueba = WORK / "prueba_limpiar"
    destino_prueba.mkdir(parents=True, exist_ok=True)
    for previo in destino_prueba.glob("lote-*.jsonl"):
        previo.unlink()
    ruta_estado = destino_prueba / "estado.tsv"
    if ruta_estado.exists():
        ruta_estado.unlink()

    ruta_cinco = destino_prueba / "cinco.tsv"
    ruta_cinco.write_text("# id\n" + "\n".join(ids_flujo) + "\n", encoding="utf-8")

    # Simular que proponer_trama.py las dejó en omitido pendientes de limpiar_enciclopedico.py
    filas_iniciales = ["id\tlote\tresultado\tmotivo"]
    for doc_id in ids_flujo:
        filas_iniciales.append(
            f"{doc_id}\tlote-001\tomitido\tsin trama de Wikipedia; pendiente de limpiar_enciclopedico.py"
        )
    ruta_estado.write_text("\n".join(filas_iniciales) + "\n", encoding="utf-8")

    rc = proponer(
        "WP-05",
        ruta_cinco,
        tam=5,
        destino=destino_prueba,
        agente="Google Antigravity (Gemini 4 Argon)",
    )
    _exigir(rc == 0, "proponer() en work/prueba_limpiar debe devolver 0")

    lote_generado = destino_prueba / "lote-002.jsonl"
    _exigir(lote_generado.is_file(), "Debe haberse escrito lote-002.jsonl en work/prueba_limpiar")
    lineas_jsonl = [json.loads(l) for l in lote_generado.read_text(encoding="utf-8").splitlines() if l.strip()]
    _exigir(len(lineas_jsonl) == 5, f"Se esperaban 5 cambios de plot y hay {len(lineas_jsonl)}")

    docs = catalogo.cargar()
    for item in lineas_jsonl:
        doc_id = item["id"]
        original = docs[doc_id]["plot"]
        nuevo = item["nuevo"]
        _exigir(item["campo"] == "plot", "limpiar_enciclopedico.py solo debe cambiar el campo plot")
        _exigir(
            catalogo.obtener_campo(docs[doc_id], "enrichment.plot") == "unverified",
            f"{doc_id}: enrichment.plot debe seguir en unverified",
        )
        _exigir(len(nuevo) >= MIN_CARACTERES, f"{doc_id}: el texto limpio debe tener >= 400 caracteres")
        _exigir(not es_ficha_inicial(dividir_parrafos(nuevo)[0]), f"{doc_id}: no debe quedar ficha inicial")
        _exigir(
            not any(es_taquilla_o_critica(p) for p in dividir_parrafos(nuevo)),
            f"{doc_id}: no deben quedar párrafos de taquilla o crítica",
        )
        _exigir(
            all(p in dividir_parrafos(original) for p in dividir_parrafos(nuevo)),
            f"{doc_id}: cada párrafo conservado debe ser un párrafo íntegro del original",
        )

    filas_finales = leer_estado(ruta_estado)
    _exigir(len(filas_finales) == 5, "estado.tsv debe conservar una sola fila por película (5 filas)")
    _exigir(
        all(f["lote"] == "lote-002" and f["resultado"] == "propuesto" for f in filas_finales),
        "Las 5 filas de estado.tsv deben pasar a propuesto en lote-002",
    )
    print(f"Prueba de limpiar_enciclopedico.py (unitaria, 5 sinopsis de dq-r00 y flujo con {', '.join(ids_flujo)}): OK.")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Limpia el plot enciclopédico (ficha inicial, taquilla y crítica) sin modificar data/."
    )
    parser.add_argument("--wp", default="WP-05", help="Paquete de trabajo (por defecto WP-05).")
    parser.add_argument(
        "--ids",
        "--lista",
        dest="ids",
        help="TSV opcional de ids (o estado.tsv). Si se omite, toma las filas omitidas de estado.tsv.",
    )
    parser.add_argument("--tam", type=int, default=100, help="Máximo de películas de este lote (por defecto 100).")
    parser.add_argument(
        "--destino",
        type=pathlib.Path,
        help="Carpeta del lote y de estado.tsv. Por defecto data-quality/cambios/<WP>.",
    )
    parser.add_argument(
        "--agente",
        default=os.environ.get("DQ_AGENTE"),
        help="Quién propone el lote, por ejemplo «Google Antigravity (Gemini 4 Argon)».",
    )
    parser.add_argument(
        "--comprobar",
        "--verificar",
        dest="comprobar",
        action="store_true",
        help="Ejecuta las pruebas unitarias y la prueba sobre 5 sinopsis reales en work/prueba_limpiar.",
    )
    args = parser.parse_args()

    try:
        if args.comprobar:
            comprobar()
            return
        if not args.agente or not args.agente.strip():
            parser.error("hace falta --agente (o la variable DQ_AGENTE): el lote registra quién lo propuso")
        destino = args.destino or (DQ / "cambios" / args.wp)
        ruta_ids = resolver_ruta(args.ids) if args.ids else None
        sys.exit(proponer(args.wp, ruta_ids, args.tam, destino, args.agente))
    except (AssertionError, RuntimeError, KeyError, ValueError, FileNotFoundError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
