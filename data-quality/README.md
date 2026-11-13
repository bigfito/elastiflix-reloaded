# 🔬 data-quality

Calidad del catálogo `elastiflix-reloaded`: el análisis del índice vivo, sus hallazgos y el plan para corregirlos.

## Empezar

| Si eres… | Lee |
|---|---|
| La dueña del proyecto | [`informe/informe-calidad.html`](informe/informe-calidad.html) (ábrelo en el navegador) y las aprobaciones de [`BITACORA.md` §3](BITACORA.md#3-aprobaciones-y-decisiones-de-la-dueña-del-proyecto). La precondición P-0 es tuya |
| Un agente que va a trabajar | [`AGENTS.md`](AGENTS.md) completo, después [`BITACORA.md`](BITACORA.md) y la tarjeta de tu WP en [`PLAN.md`](PLAN.md) |

El equipo trabaja por turnos (`AGENTS.md` §1). Desde el turno 12 ejecutan **Cursor y Gemma 4 E4B**, y **Claude
Code** revisa después de cada turno (P-13; el orden exacto está en `PLAN.md` · Reparto y turnos). Abre la herramienta que indica «Próximo paso» de la bitácora con el
worktree `E:\02 - programming\03 - elastiflix-reloaded-dq` como carpeta, y envíale su mensaje:

| Herramienta | Mensaje |
|---|---|
| Cursor | `Eres Cursor, ejecutor del equipo de calidad. Lee data-quality/AGENTS.md y ejecuta tu turno según «Próximo paso» de data-quality/BITACORA.md.` |
| Gemma 4 E4B | `Eres Gemma, ejecutor del equipo de calidad. Lee data-quality/AGENTS.md (§3, §5 y §5.1) y ejecuta tu turno según «Próximo paso» de data-quality/BITACORA.md.` |
| Claude Code | `Eres Claude Code, revisor del equipo de calidad. Lee data-quality/AGENTS.md y haz la revisión del último turno según data-quality/BITACORA.md.` |

## Qué hay aquí

| Ruta | Contenido |
|---|---|
| `informe/informe-calidad.html` | Informe del análisis del 12-nov-2026 (índice `elastiflix-reloaded-20261112-144129`, 6 899 películas) |
| `hallazgos/hallazgos.csv` | Un hallazgo por fila: `id`, título, año, severidad, categoría, campo y detalle (2 514 filas, 2 401 películas) |
| `hallazgos/listas/` | Alcance congelado de cada WP (`.tsv` con `id` y título) |
| `hallazgos/salidas/m0/` | Línea base: salida de cada script del análisis, mapping vivo y listas de ids. No se sobrescribe |
| `hallazgos/salidas/<etiqueta>/` | Mediciones posteriores (`wp-05`, `m1`, `m3`…), una carpeta por corrida |
| `scripts/` | Análisis en Python y herramientas del plan |
| `PLAN.md` · `AGENTS.md` · `BITACORA.md` · `bitacora/` | Plan, protocolo del equipo, tablero y diarios por WP |
| `cambios/` · `fuentes/` | Se crean durante la ejecución: lotes de cambios a `data/` con su estado, y atribución de cada sinopsis de Wikipedia |
| `work/` | Caché local fuera de git (`docs.json`, vectores, descargas) |

## Volver a correr el análisis

Necesita el `.env` de la raíz y Python 3.11+ con `numpy`.

```bash
python data-quality/scripts/analizar.py --etiqueta prueba --desde-data
```

| Opción | Efecto |
|---|---|
| `--etiqueta X` | Obligatoria: carpeta `hallazgos/salidas/X/`. `m0` está congelada |
| `--desde-data` | Analiza `data/` sin red ni Elasticsearch (sin vectores) |
| `--sin-descarga` | Reutiliza `work/docs.json` |
| `--sin-vectores` | Omite `vectors.py`, que baja unos 165 MB y llama al servicio de inferencia |

Sin `--desde-data` lee el alias de `ELASTIFLIX_INDEX_ALIAS` (por defecto, el del `.env`: producción). Para staging,
antepón `ELASTIFLIX_INDEX_ALIAS=elastiflix-dq-staging`. Ningún script de análisis escribe en Elasticsearch, y
`es.py` bloquea cualquier escritura fuera de los índices `elastiflix-dq-staging*`.

| Script | Qué hace |
|---|---|
| `analizar.py` | Corre los siguientes en orden y guarda su salida |
| `dump.py` | Películas a `work/docs.json`, del índice (PIT y `search_after`) o de `data/` |
| `profile.py` | Presencia y tipo de cada campo |
| `deep.py` | Ids, duplicados, texto, fechas, números, vocabularios, rutas, colecciones, palabras clave, `enrichment` |
| `deep2.py` | Puntuaciones, conflictos de palabras clave, idiomas, países y montos |
| `plots.py` | Largo, origen, duplicados y contenido no narrativo de `plot` |
| `llm.py` | Firmas de texto generado, respuestas de asistente y reparto que no coincide con `cast` |
| `vectors.py` | Normas, proyección 1024 → 512 → 256, casi duplicados y frescura (vuelve a generar 52 vectores) |
| `findings_csv.py` | Escribe `hallazgos.csv` en la carpeta de salida |
| `listas.py` | Congeló `hallazgos/listas/` al crear el plan; no se vuelve a correr |
| `es.py` · `paths.py` | Cliente de Elasticsearch con las reglas de escritura y `aprobada()`, y rutas comunes |

Las herramientas del plan (`catalogo.py`, `aplicar_cambios.py`, `wiki_plot.py`, `proponer_trama.py`,
`limpiar_enciclopedico.py`, `cargar.py`, `publicar.py`) se escriben en WP-00 y WP-05; `revisar_reparto.py` (WP-05 T-03,
solo lectura) se agregó con P-13.
