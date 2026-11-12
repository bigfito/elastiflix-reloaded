"""Rutas de data-quality, calculadas desde este archivo para que los scripts corran desde cualquier carpeta.

OUT es la carpeta de salidas de la corrida actual: `hallazgos/salidas/<etiqueta>/`. La pone analizar.py en la
variable DQ_ETIQUETA (por defecto `actual`). `m0` es la línea base congelada del informe y no se sobrescribe.
"""
import os
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent
DQ = SCRIPTS.parent                      # data-quality/
REPO = DQ.parent                         # raíz del checkout o del worktree
WORK = DQ / "work"                       # caché local, fuera de git (docs.json, vectores, descargas)
HALLAZGOS = DQ / "hallazgos"
SALIDAS = HALLAZGOS / "salidas"
M0 = SALIDAS / "m0"                      # línea base del informe (12-nov-2026)
ETIQUETA = os.environ.get("DQ_ETIQUETA", "actual")
OUT = SALIDAS / ETIQUETA
WORK.mkdir(exist_ok=True)
OUT.mkdir(parents=True, exist_ok=True)
