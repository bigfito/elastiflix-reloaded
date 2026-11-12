"""Lectura y escritura exacta del catálogo data/elastiflix-reloaded_0{1..7}.{json,csv}.

Garantiza round-trip byte a byte entre data/ y las estructuras en memoria:
- `cargar() -> dict[id, doc]` lee las 7 partes .json en orden.
- `guardar(docs, destino=...)` reescribe las 7 partes .json (sangría de 1 espacio, LF, mismas claves de cabecera,
  `document_count` recalculado, `exported_at` intacto) y sus 7 .csv (UTF-8 con BOM, CRLF entre filas, coma, mismo
  orden de columnas, listas y objetos como JSON compacto y `enrichment` con las claves en el orden del mapping).
- `sha(valor) -> str` calcula el SHA-256 hexadecimal de `json.dumps(valor, ensure_ascii=False, separators=(',', ':'), sort_keys=True)`,
  donde un campo ausente vale `None` (`null` en JSON).
- `validar_cambio(campo, nuevo)` rechaza un campo que no existe en el catálogo o un valor de `enrichment.*` que no es
  texto, antes de que llegue a `data/` (revisión R-01).

`guardar()` exige exactamente las mismas películas que la plantilla (no agrega ni pierde ninguna en silencio) y
escribe las 14 partes primero en archivos temporales y después las reemplaza, para no dejar `data/` a medias.
"""
import argparse
import csv
import hashlib
import io
import json
import os
import pathlib
import sys
from typing import Any, Iterable

sys.stdout.reconfigure(encoding="utf-8")
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from paths import REPO, WORK

DATA_DIR = REPO / "data"
MAPPING_PATH = REPO / "elasticsearch" / "elastiflix-reloaded-mapping.json"
PARTES = [f"elastiflix-reloaded_0{i}" for i in range(1, 8)]

COLUMNAS = [
    "_id",
    "id",
    "imdb_id",
    "title",
    "original_title",
    "original_language",
    "release_date",
    "runtime",
    "status",
    "adult",
    "video",
    "tagline",
    "overview",
    "plot",
    "genres",
    "keywords",
    "cast",
    "belongs_to_collection",
    "budget",
    "revenue",
    "popularity",
    "vote_average",
    "vote_count",
    "user_score",
    "rating",
    "homepage",
    "poster_path",
    "backdrop_path",
    "production_companies",
    "production_countries",
    "spoken_languages",
    "enrichment",
]


def _cargar_claves_enrichment() -> list[str]:
    mapping = json.loads(MAPPING_PATH.read_text(encoding="utf-8"))
    props = mapping["mappings"]["properties"]["enrichment"]["properties"]
    return list(props.keys())


ENRICHMENT_KEYS = _cargar_claves_enrichment()

# Campos que un lote puede cambiar. `_id` e `id` identifican la película y no se cambian.
CAMPOS_EDITABLES = frozenset(COLUMNAS) - {"_id", "id"}
SUBCAMPOS_COLECCION = frozenset({"id", "name", "poster_path", "backdrop_path"})


def validar_cambio(campo: str, nuevo: Any) -> None:
    """Lanza `ValueError` si `campo` no existe en el catálogo o si `nuevo` no sirve para ese campo.

    Admite los 30 campos editables de primer nivel, `enrichment.<clave del mapping>` (texto o `null`, como exige
    `EFX-ING-012`) y `belongs_to_collection.<id|name|poster_path|backdrop_path>`.
    """
    if "." not in campo:
        if campo not in CAMPOS_EDITABLES:
            raise ValueError(f"campo {campo!r} no existe en el catálogo o no se puede cambiar")
        if campo == "enrichment" and nuevo is not None:
            if not isinstance(nuevo, dict):
                raise ValueError("enrichment debe ser un objeto o null")
            for clave, valor in nuevo.items():
                validar_cambio(f"enrichment.{clave}", valor)
        return
    padre, hijo = campo.split(".", 1)
    if padre == "enrichment":
        if hijo not in ENRICHMENT_KEYS:
            raise ValueError(f"enrichment.{hijo} no es una de las {len(ENRICHMENT_KEYS)} claves del mapping")
        if nuevo is not None and not (isinstance(nuevo, str) and nuevo.strip()):
            raise ValueError(f"enrichment.{hijo} debe ser un texto no vacío o null (EFX-ING-012)")
        return
    if padre == "belongs_to_collection" and hijo in SUBCAMPOS_COLECCION:
        return
    raise ValueError(f"ruta {campo!r} no admitida: solo enrichment.<clave> y belongs_to_collection.<subcampo>")


def ordenar_enrichment(enr: dict[str, Any] | None) -> dict[str, Any] | None:
    """Devuelve `enrichment` con sus claves en el orden del mapping, o `None` si queda vacío."""
    if not enr:
        return None
    limpio = {k: v for k, v in enr.items() if v is not None}
    if not limpio:
        return None
    ordenadas: dict[str, Any] = {}
    for k in ENRICHMENT_KEYS:
        if k in limpio:
            ordenadas[k] = limpio[k]
    for k in sorted(limpio.keys()):
        if k not in ordenadas:
            ordenadas[k] = limpio[k]
    return ordenadas


def normalizar_doc(doc: dict[str, Any]) -> dict[str, Any]:
    """Asegura el orden canónico de las 32 columnas y de las claves de `enrichment`."""
    copia = dict(doc)
    copia["enrichment"] = ordenar_enrichment(copia.get("enrichment"))
    out: dict[str, Any] = {}
    for col in COLUMNAS:
        out[col] = copia.get(col)
    for k, v in copia.items():
        if k not in out:
            out[k] = v
    return out


def sha(valor: Any) -> str:
    """SHA-256 de `json.dumps(valor, ensure_ascii=False, separators=(',', ':'), sort_keys=True)`.

    Un campo ausente debe pasarse como `None` (serializado como `null`).
    """
    canonico = json.dumps(valor, ensure_ascii=False, separators=(",", ":"), sort_keys=True)
    return hashlib.sha256(canonico.encode("utf-8")).hexdigest()


def obtener_campo(doc: dict[str, Any], campo: str) -> Any:
    """Obtiene el valor de `campo` en `doc`, admitiendo rutas con punto como `enrichment.plot`."""
    if "." not in campo:
        return doc.get(campo, None)
    partes = campo.split(".")
    actual: Any = doc
    for p in partes:
        if not isinstance(actual, dict) or p not in actual:
            return None
        actual = actual[p]
    return actual


def asignar_campo(doc: dict[str, Any], campo: str, nuevo: Any) -> None:
    """Asigna `nuevo` a `campo` en `doc` (admite rutas como `enrichment.plot`; `None` borra claves en `enrichment`)."""
    if "." not in campo:
        if campo == "enrichment":
            doc["enrichment"] = ordenar_enrichment(nuevo)
        else:
            doc[campo] = nuevo
        return
    padre, hijo = campo.split(".", 1)
    if padre == "enrichment":
        enr = dict(doc.get("enrichment") or {})
        if nuevo is None:
            enr.pop(hijo, None)
        else:
            enr[hijo] = nuevo
        doc["enrichment"] = ordenar_enrichment(enr)
    else:
        contenedor = dict(doc.get(padre) or {})
        if nuevo is None:
            contenedor.pop(hijo, None)
        else:
            contenedor[hijo] = nuevo
        doc[padre] = contenedor


def cargar(origen: pathlib.Path | str = DATA_DIR) -> dict[str, dict[str, Any]]:
    """Carga las 7 partes .json en orden y devuelve `dict[id, doc]`."""
    origen_path = pathlib.Path(origen)
    docs: dict[str, dict[str, Any]] = {}
    for base in PARTES:
        ruta = origen_path / f"{base}.json"
        contenido = json.loads(ruta.read_text(encoding="utf-8"))
        for d in contenido["documents"]:
            docs[str(d["id"])] = d
    return docs


def _serializar_celda_csv(col: str, val: Any) -> str:
    if val is None:
        return ""
    if isinstance(val, bool):
        return "true" if val else "false"
    if isinstance(val, (list, dict)):
        if col == "enrichment":
            val = ordenar_enrichment(val)
            if val is None:
                return ""
        return json.dumps(val, ensure_ascii=False, separators=(",", ":"))
    return str(val)


def _escribir_csv(ruta_csv: pathlib.Path, part_docs: list[dict[str, Any]]) -> None:
    buf = io.StringIO()
    writer = csv.writer(buf, delimiter=",", lineterminator="\r\n")
    writer.writerow(COLUMNAS)
    for d in part_docs:
        fila = [_serializar_celda_csv(col, d.get(col)) for col in COLUMNAS]
        writer.writerow(fila)
    ruta_csv.write_bytes(b"\xef\xbb\xbf" + buf.getvalue().encode("utf-8"))


def guardar(
    docs: dict[str, dict[str, Any]] | Iterable[dict[str, Any]],
    destino: pathlib.Path | str = DATA_DIR,
    plantilla_dir: pathlib.Path | str = DATA_DIR,
) -> None:
    """Reescribe las 7 partes `.json` y sus 7 `.csv` en `destino`."""
    destino_path = pathlib.Path(destino)
    plantilla_path = pathlib.Path(plantilla_dir)
    destino_path.mkdir(parents=True, exist_ok=True)

    if isinstance(docs, dict):
        por_id = {str(k): normalizar_doc(v) for k, v in docs.items()}
    else:
        por_id = {str(d["id"]): normalizar_doc(d) for d in docs}

    plantillas = {}
    ids_plantilla: set[str] = set()
    for base in PARTES:
        plantillas[base] = json.loads((plantilla_path / f"{base}.json").read_text(encoding="utf-8"))
        ids_plantilla.update(str(d["id"]) for d in plantillas[base]["documents"])
    faltan = ids_plantilla - set(por_id)
    sobran = set(por_id) - ids_plantilla
    if faltan or sobran:
        raise ValueError(
            f"guardar() no agrega ni quita películas: faltan {len(faltan)} {sorted(faltan)[:5]}, "
            f"sobran {len(sobran)} {sorted(sobran)[:5]}. No se escribió nada."
        )

    escritos: list[tuple[pathlib.Path, pathlib.Path]] = []
    for base in PARTES:
        orig = plantillas[base]
        ids_parte = [str(d["id"]) for d in orig["documents"]]
        part_docs = [por_id[doc_id] for doc_id in ids_parte]

        out_obj = {
            "index": orig["index"],
            "exported_at": orig["exported_at"],
            "document_count": len(part_docs),
            "csv_file": orig["csv_file"],
            "load_notes": orig["load_notes"],
            "mapping": orig["mapping"],
            "documents": part_docs,
        }
        texto_json = json.dumps(out_obj, indent=1, ensure_ascii=False) + "\n"
        tmp_json = destino_path / f"{base}.json.tmp"
        tmp_csv = destino_path / f"{base}.csv.tmp"
        tmp_json.write_bytes(texto_json.encode("utf-8"))
        _escribir_csv(tmp_csv, part_docs)
        escritos += [(tmp_json, destino_path / f"{base}.json"), (tmp_csv, destino_path / f"{base}.csv")]
    # Las 14 partes ya están completas en disco: recién ahora se reemplazan los originales.
    for tmp, final in escritos:
        os.replace(tmp, final)


def verificar() -> int:
    """Reescribe las 14 partes en `work/roundtrip/` y comprueba que sean byte a byte iguales a `data/`."""
    roundtrip_dir = WORK / "roundtrip"
    roundtrip_dir.mkdir(parents=True, exist_ok=True)
    docs = cargar(DATA_DIR)
    guardar(docs, destino=roundtrip_dir, plantilla_dir=DATA_DIR)

    iguales = 0
    total = 0
    for base in PARTES:
        for ext in (".json", ".csv"):
            total += 1
            nombre = f"{base}{ext}"
            b_orig = (DATA_DIR / nombre).read_bytes()
            b_nuevo = (roundtrip_dir / nombre).read_bytes()
            ok = b_orig == b_nuevo
            if ok:
                iguales += 1
                print(f"OK   {nombre} ({len(b_nuevo)} bytes)")
            else:
                print(f"DIFF {nombre}: orig={len(b_orig)} bytes, roundtrip={len(b_nuevo)} bytes")
    print(f"Round-trip: {iguales}/{total} archivos byte a byte iguales ({len(docs)} películas).")
    return 0 if iguales == total else 1


def main() -> None:
    parser = argparse.ArgumentParser(description="Lectura, escritura y verificación round-trip de data/.")
    parser.add_argument(
        "--verificar",
        action="store_true",
        help="Reescribe las 14 partes en work/roundtrip/ y verifica que sean byte a byte iguales a data/.",
    )
    args = parser.parse_args()
    if args.verificar:
        sys.exit(verificar())
    parser.print_help()


if __name__ == "__main__":
    main()
