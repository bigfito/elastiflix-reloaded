"""WP-05 T-03: estado de las películas de `wp02-revisar-reparto.tsv` (solo lectura).

Cruza la lista con la salida de `analizar.py --etiqueta <X> --desde-data` (`llm_ids.json`, clave `cast_miss`: el plot
nombra protagonistas que no están en `cast`) y con la etiqueta `enrichment.plot` de cada película. Escribe
`hallazgos/salidas/<X>/reparto.tsv` y un resumen. No cambia `data/`.

Uso (después de `analizar.py --etiqueta t12 --desde-data`):
    python data-quality/scripts/revisar_reparto.py --etiqueta t12

Las películas que siguen en `cast_miss` se anotan en el diario de WP-05 para que la revisión decida si son un falso
positivo (P-13). Preparado por Claude Code al ajustar el plan para Qwen 3.5 9B.
"""

from __future__ import annotations

import argparse
import csv
import json
import sys

import catalogo
from paths import DQ, SALIDAS

LISTA = DQ / "hallazgos" / "listas" / "wp02-revisar-reparto.tsv"


def main() -> int:
    sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--etiqueta", required=True, help="Etiqueta de una corrida de analizar.py, por ejemplo t12")
    args = parser.parse_args()

    ids_json = SALIDAS / args.etiqueta / "llm_ids.json"
    if not ids_json.is_file():
        print(f"Falta {ids_json}. Corre antes: python data-quality/scripts/analizar.py --etiqueta {args.etiqueta} --desde-data")
        return 1
    cast_miss = set(json.loads(ids_json.read_text(encoding="utf-8"))["cast_miss"])
    docs = catalogo.cargar()

    filas = []
    for linea in LISTA.read_text(encoding="utf-8").splitlines():
        if not linea.strip() or linea.startswith("#"):
            continue
        doc_id, titulo = linea.split("\t")[:2]
        etiqueta = (docs[doc_id].get("enrichment") or {}).get("plot", "")
        sigue = doc_id in cast_miss
        estado = "pendiente de juicio" if sigue else "cerrada"
        filas.append((doc_id, titulo, etiqueta, "sí" if sigue else "no", estado))

    salida = SALIDAS / args.etiqueta / "reparto.tsv"
    with salida.open("w", encoding="utf-8", newline="\n") as f:
        escritor = csv.writer(f, delimiter="\t", lineterminator="\n")
        escritor.writerow(["id", "título (año)", "enrichment.plot", "sigue en cast_miss", "estado"])
        escritor.writerows(filas)

    pendientes = [f for f in filas if f[4] != "cerrada"]
    print(f"{len(filas)} películas en la lista: {len(filas) - len(pendientes)} cerradas, {len(pendientes)} pendientes de juicio.")
    for doc_id, titulo, etiqueta, _, _ in pendientes:
        print(f"  {doc_id}\t{titulo}\t{etiqueta}")
    print(f"Detalle en {salida.relative_to(DQ.parent)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
