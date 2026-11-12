"""Aplica lotes de cambios (.jsonl) al catálogo de `data/` usando `catalogo.py` y actualiza `estado.tsv`.

Formato de lote (`data-quality/cambios/WP-XX/lote-NNN.jsonl`) — WP-00 T-03:
Cada línea es un objeto JSON en una sola línea con exactamente estos 10 campos:
  {
    "wp": "WP-01",
    "lote": "lote-001",
    "id": "597",
    "campo": "plot",
    "antes_sha256": "<sha256 hex de catalogo.sha(valor_actual)>",
    "nuevo": "<valor final, o null para borrar>",
    "fuente": "enwiki:Titanic (1997 film)#revid=123456789",
    "motivo": "ensayo generado",
    "agente": "Google Antigravity (Gemini 4 Argon)",
    "fecha": "2026-11-12"
  }

Reglas del formato y de la aplicación (WP-00 T-03 y T-04):
1. `campo` admite campos de primer nivel (`plot`, `keywords`, `budget`…) y rutas con punto como `enrichment.plot`.
2. `antes_sha256` es `catalogo.sha(valor_actual)`, donde un campo ausente vale `null` (`None`).
3. `nuevo` es el valor final del campo (`null` borra una clave dentro de `enrichment` o pone `null` en el campo).
4. **Todo o nada por lote:** antes de escribir nada, se validan todas las líneas del lote. Si alguna línea tiene un
   valor actual cuyo hash no coincide ni con `antes_sha256` ni con `catalogo.sha(nuevo)`, se rechaza el lote entero
   sin modificar `data/` ni `estado.tsv`.
5. **Idempotencia:** si el valor actual ya es igual a `nuevo` (`catalogo.sha(valor_actual) == catalogo.sha(nuevo)`),
   la línea cuenta como ya aplicada. Si todas las líneas del lote ya estaban aplicadas, no se reescribe `data/`.
6. `--simular` valida e informa qué cambiaría sin escribir en `data/` ni en `estado.tsv`.
7. Al aplicar un lote real, actualiza `estado.tsv` (en la misma carpeta del lote, salvo `--estado`): las filas de las
   películas del lote pasan a `aplicado` (o a `corregido` si `motivo` indica una corrección `corrección R-NN`).
"""
import argparse
import copy
import csv
import json
import pathlib
import sys
from typing import Any

sys.stdout.reconfigure(encoding="utf-8")
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import catalogo
from paths import WORK

CAMPOS_LOTE = (
    "wp",
    "lote",
    "id",
    "campo",
    "antes_sha256",
    "nuevo",
    "fuente",
    "motivo",
    "agente",
    "fecha",
)
COLUMNAS_ESTADO = ["id", "lote", "resultado", "motivo"]


def leer_lote(ruta_lote: pathlib.Path) -> list[dict[str, Any]]:
    """Lee y valida la estructura sintáctica de un archivo `lote-NNN.jsonl`."""
    if not ruta_lote.exists():
        raise FileNotFoundError(f"No existe el archivo de lote: {ruta_lote}")
    cambios: list[dict[str, Any]] = []
    for num_linea, linea in enumerate(ruta_lote.read_text(encoding="utf-8").splitlines(), start=1):
        texto = linea.strip()
        if not texto:
            continue
        try:
            item = json.loads(texto)
        except json.JSONDecodeError as exc:
            raise ValueError(f"{ruta_lote.name}:{num_linea}: JSON inválido: {exc}") from exc
        if not isinstance(item, dict):
            raise ValueError(f"{ruta_lote.name}:{num_linea}: cada línea debe ser un objeto JSON.")
        faltantes = [k for k in CAMPOS_LOTE if k not in item]
        if faltantes:
            raise ValueError(f"{ruta_lote.name}:{num_linea}: faltan campos obligatorios: {faltantes}")
        cambios.append(item)
    if not cambios:
        raise ValueError(f"{ruta_lote.name}: el lote está vacío.")
    return cambios


def _es_correccion(motivo: str) -> bool:
    m = (motivo or "").lower()
    return m.startswith("corrección") or m.startswith("correccion")


def actualizar_estado_tsv(ruta_estado: pathlib.Path, cambios: list[dict[str, Any]]) -> bool:
    """Actualiza o crea `estado.tsv` marcando las películas del lote como `aplicado` o `corregido`.

    Devuelve True si el archivo `estado.tsv` cambió en disco.
    """
    filas: list[dict[str, str]] = []
    contenido_previo = ""
    if ruta_estado.exists():
        contenido_previo = ruta_estado.read_text(encoding="utf-8")
        reader = csv.DictReader(contenido_previo.splitlines(), delimiter="\t")
        for r in reader:
            filas.append(
                {
                    "id": str(r.get("id", "")),
                    "lote": str(r.get("lote", "")),
                    "resultado": str(r.get("resultado", "")),
                    "motivo": str(r.get("motivo", "")),
                }
            )

    # Agrupar por película conservando el orden de aparición en el lote
    por_pelicula: dict[str, dict[str, Any]] = {}
    for c in cambios:
        doc_id = str(c["id"])
        if doc_id not in por_pelicula:
            por_pelicula[doc_id] = c

    for doc_id, c in por_pelicula.items():
        lote_id = str(c["lote"])
        motivo = str(c["motivo"])
        nuevo_res = "corregido" if _es_correccion(motivo) else "aplicado"

        actualizada = False
        for fila in filas:
            if fila["id"] == doc_id and fila["lote"] == lote_id:
                fila["resultado"] = nuevo_res
                if not fila["motivo"] or _es_correccion(motivo):
                    fila["motivo"] = motivo
                actualizada = True
                break

        if not actualizada and _es_correccion(motivo):
            # La fila conserva su lote original, para saber de dónde venía el error; el motivo nombra la corrección.
            for fila in filas:
                if fila["id"] == doc_id:
                    fila["resultado"] = nuevo_res
                    fila["motivo"] = f"{motivo} ({lote_id})"
                    actualizada = True
                    break

        if not actualizada:
            filas.append(
                {
                    "id": doc_id,
                    "lote": lote_id,
                    "resultado": nuevo_res,
                    "motivo": motivo,
                }
            )

    lineas_out = ["\t".join(COLUMNAS_ESTADO)]
    for fila in filas:
        lineas_out.append("\t".join(fila[col] for col in COLUMNAS_ESTADO))
    nuevo_contenido = "\n".join(lineas_out) + "\n"

    if nuevo_contenido == contenido_previo:
        return False
    ruta_estado.parent.mkdir(parents=True, exist_ok=True)
    ruta_estado.write_text(nuevo_contenido, encoding="utf-8", newline="\n")
    return True


def aplicar_lote(
    ruta_lote: pathlib.Path,
    *,
    catalogo_dir: pathlib.Path = catalogo.DATA_DIR,
    ruta_estado: pathlib.Path | None = None,
    simular: bool = False,
) -> tuple[int, int]:
    """Valida y aplica un lote sobre `catalogo_dir`.

    Devuelve `(cambios_nuevos, cambios_ya_aplicados)`.
    Lanza `ValueError` o `KeyError` si alguna línea falla la verificación de `antes_sha256` (todo o nada).
    """
    cambios = leer_lote(ruta_lote)
    docs = catalogo.cargar(catalogo_dir)
    docs_copia = copy.deepcopy(docs)

    por_aplicar = 0
    ya_aplicados = 0

    for idx, c in enumerate(cambios, start=1):
        doc_id = str(c["id"])
        if doc_id not in docs_copia:
            raise KeyError(f"{ruta_lote.name}:{idx}: película id={doc_id!r} no existe en el catálogo.")
        doc = docs_copia[doc_id]
        campo = str(c["campo"])
        antes_esp = str(c["antes_sha256"])
        nuevo_val = c["nuevo"]

        try:
            catalogo.validar_cambio(campo, nuevo_val)
        except ValueError as exc:
            raise ValueError(f"{ruta_lote.name}:{idx}: id={doc_id}: {exc}. Se rechaza el lote completo.") from exc

        val_actual = catalogo.obtener_campo(doc, campo)
        sha_actual = catalogo.sha(val_actual)
        sha_nuevo = catalogo.sha(nuevo_val)

        if sha_actual == antes_esp:
            if sha_actual == sha_nuevo:
                ya_aplicados += 1
            else:
                catalogo.asignar_campo(doc, campo, nuevo_val)
                por_aplicar += 1
        elif sha_actual == sha_nuevo:
            ya_aplicados += 1
        else:
            raise ValueError(
                f"{ruta_lote.name}:{idx}: conflicto en id={doc_id} campo={campo!r}: "
                f"antes_sha256 esperado={antes_esp}, actual={sha_actual} (valor actual={val_actual!r}). "
                f"Se rechaza el lote completo."
            )

    peliculas = len({str(c["id"]) for c in cambios})
    modo = "SIMULACIÓN" if simular else "APLICADO"
    print(
        f"[{modo}] {ruta_lote.name}: {peliculas} películas · "
        f"{por_aplicar} cambios aplicados · {ya_aplicados} ya estaban aplicados."
    )

    if not simular:
        if por_aplicar > 0:
            catalogo.guardar(docs_copia, destino=catalogo_dir, plantilla_dir=catalogo_dir)
        destino_estado = ruta_estado if ruta_estado is not None else (ruta_lote.parent / "estado.tsv")
        actualizar_estado_tsv(destino_estado, cambios)

    return por_aplicar, ya_aplicados


def verificar_en_work() -> int:
    """Ejecuta la prueba de aceptación de WP-00 T-04 en `work/prueba_lote/`."""
    prueba_dir = WORK / "prueba_lote"
    cat_dir = prueba_dir / "data"
    cambios_dir = prueba_dir / "cambios" / "WP-00"
    cambios_dir.mkdir(parents=True, exist_ok=True)

    # 1. Preparar copia aislada del catálogo en work/prueba_lote/data
    docs_base = catalogo.cargar(catalogo.DATA_DIR)
    catalogo.guardar(docs_base, destino=cat_dir, plantilla_dir=catalogo.DATA_DIR)

    doc_11 = docs_base["11"]
    plot_original = doc_11["plot"]
    enr_plot_original = catalogo.obtener_campo(doc_11, "enrichment.plot")
    nuevo_plot = plot_original + "\n\n[Prueba de lote WP-00 T-04]"

    lote_path = cambios_dir / "lote-001.jsonl"
    estado_path = cambios_dir / "estado.tsv"
    estado_path.write_text("id\tlote\tresultado\tmotivo\n11\tlote-001\tpropuesto\tprueba T-04\n", encoding="utf-8")

    lineas_lote = [
        {
            "wp": "WP-00",
            "lote": "lote-001",
            "id": "11",
            "campo": "plot",
            "antes_sha256": catalogo.sha(plot_original),
            "nuevo": nuevo_plot,
            "fuente": "work:prueba-t04",
            "motivo": "prueba T-04",
            "agente": "Google Antigravity (Gemini 4 Argon)",
            "fecha": "2026-11-12",
        },
        {
            "wp": "WP-00",
            "lote": "lote-001",
            "id": "11",
            "campo": "enrichment.plot",
            "antes_sha256": catalogo.sha(enr_plot_original),
            "nuevo": "wikipedia_plot_2026-11-12",
            "fuente": "work:prueba-t04",
            "motivo": "prueba T-04",
            "agente": "Google Antigravity (Gemini 4 Argon)",
            "fecha": "2026-11-12",
        },
    ]
    lote_path.write_text(
        "\n".join(json.dumps(x, ensure_ascii=False) for x in lineas_lote) + "\n",
        encoding="utf-8",
    )

    # Paso 1: Primera pasada aplica el lote
    aplicados_1, ya_1 = aplicar_lote(lote_path, catalogo_dir=cat_dir, ruta_estado=estado_path)
    assert aplicados_1 == 2 and ya_1 == 0, f"Paso 1 inesperado: {aplicados_1}, {ya_1}"
    bytes_tras_paso_1 = (cat_dir / "elastiflix-reloaded_01.json").read_bytes()
    estado_tras_paso_1 = estado_path.read_text(encoding="utf-8")
    assert "11\tlote-001\taplicado\tprueba T-04" in estado_tras_paso_1

    # Paso 2: Segunda pasada es idempotente y no cambia nada
    aplicados_2, ya_2 = aplicar_lote(lote_path, catalogo_dir=cat_dir, ruta_estado=estado_path)
    assert aplicados_2 == 0 and ya_2 == 2, f"Paso 2 inesperado: {aplicados_2}, {ya_2}"
    bytes_tras_paso_2 = (cat_dir / "elastiflix-reloaded_01.json").read_bytes()
    assert bytes_tras_paso_1 == bytes_tras_paso_2, "La segunda pasada modificó los bytes del catálogo"

    # Paso 3: Un antes_sha256 alterado rechaza el lote entero sin tocar nada
    lote_invalido_path = cambios_dir / "lote-002-invalido.jsonl"
    lineas_invalidas = [
        {
            "wp": "WP-00",
            "lote": "lote-002",
            "id": "11",
            "campo": "plot",
            "antes_sha256": "0" * 64,
            "nuevo": "Plot que no debe aplicarse jamás",
            "fuente": "work:prueba-t04",
            "motivo": "prueba rechazo T-04",
            "agente": "Google Antigravity (Gemini 4 Argon)",
            "fecha": "2026-11-12",
        }
    ]
    lote_invalido_path.write_text(
        "\n".join(json.dumps(x, ensure_ascii=False) for x in lineas_invalidas) + "\n",
        encoding="utf-8",
    )
    rechazado = False
    try:
        aplicar_lote(lote_invalido_path, catalogo_dir=cat_dir, ruta_estado=estado_path)
    except ValueError as exc:
        rechazado = True
        print(f"[RECHAZO ESPERADO] {exc}")
    assert rechazado, "El lote con antes_sha256 alterado NO fue rechazado"
    assert (cat_dir / "elastiflix-reloaded_01.json").read_bytes() == bytes_tras_paso_2
    assert estado_path.read_text(encoding="utf-8") == estado_tras_paso_1

    # Pasos 4 y 5 (R-01): un campo que no existe y un enrichment que no es texto rechazan el lote sin tocar nada
    for paso, campo, valor in ((4, "plt", "errata"), (5, "enrichment.plot", 123)):
        lote_malo = cambios_dir / f"lote-00{paso}-invalido.jsonl"
        linea = dict(lineas_lote[0], lote=f"lote-00{paso}", campo=campo, nuevo=valor,
                     antes_sha256=catalogo.sha(catalogo.obtener_campo(catalogo.cargar(cat_dir)["11"], campo)))
        lote_malo.write_text(json.dumps(linea, ensure_ascii=False) + "\n", encoding="utf-8")
        try:
            aplicar_lote(lote_malo, catalogo_dir=cat_dir, ruta_estado=estado_path)
            raise AssertionError(f"Paso {paso}: el lote con {campo}={valor!r} NO fue rechazado")
        except ValueError as exc:
            print(f"[RECHAZO ESPERADO] {exc}")
        assert (cat_dir / "elastiflix-reloaded_01.json").read_bytes() == bytes_tras_paso_2

    # Paso 6 (R-01): guardar() con una película de menos no escribe nada
    incompleto = catalogo.cargar(cat_dir)
    incompleto.pop("11")
    try:
        catalogo.guardar(incompleto, destino=cat_dir, plantilla_dir=cat_dir)
        raise AssertionError("Paso 6: guardar() aceptó un catálogo con una película de menos")
    except ValueError as exc:
        print(f"[RECHAZO ESPERADO] {exc}")
    assert (cat_dir / "elastiflix-reloaded_01.json").read_bytes() == bytes_tras_paso_2

    print("Verificación T-04 en work/prueba_lote/: OK (aplicación, idempotencia, rechazo por antes_sha256, "
          "campo desconocido, enrichment no textual y catálogo incompleto).")
    return 0


def main() -> None:
    parser = argparse.ArgumentParser(description="Aplica lotes de cambios JSONL al catálogo de data/.")
    parser.add_argument("lotes", nargs="*", help="Rutas a uno o más archivos lote-NNN.jsonl.")
    parser.add_argument(
        "--simular",
        action="store_true",
        help="Solo valida e informa cuántos cambios se aplicarían, sin escribir en data/ ni en estado.tsv.",
    )
    parser.add_argument(
        "--catalogo-dir",
        type=pathlib.Path,
        default=catalogo.DATA_DIR,
        help="Carpeta del catálogo (por defecto data/).",
    )
    parser.add_argument(
        "--estado",
        type=pathlib.Path,
        default=None,
        help="Ruta explícita a estado.tsv (por defecto <carpeta_del_lote>/estado.tsv).",
    )
    parser.add_argument(
        "--verificar",
        action="store_true",
        help="Ejecuta la prueba de aceptación de T-04 en work/prueba_lote/ sin tocar data/.",
    )
    args = parser.parse_args()

    if args.verificar:
        sys.exit(verificar_en_work())

    if not args.lotes:
        parser.print_help()
        sys.exit(1)

    try:
        for ruta_str in args.lotes:
            aplicar_lote(
                pathlib.Path(ruta_str),
                catalogo_dir=args.catalogo_dir,
                ruta_estado=args.estado,
                simular=args.simular,
            )
    except (FileNotFoundError, KeyError, ValueError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
