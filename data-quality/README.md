# 🔬 data-quality

Calidad del catálogo `elastiflix-reloaded`: el análisis del índice vivo, sus hallazgos y la ronda 4 que los corrigió.

## Estado: ronda 4 cerrada

La ronda 4 se hizo del 12 al 13 de noviembre de 2026 y ya está en producción:
- El alias `elastiflix-reloaded` apunta a `elastiflix-dq-staging-20261113-220031` (medición M4, publicada con H-3).
- La rama `dq/ronda-4` está integrada en `main` y publicada en origin.
- El equipo de agentes ya no trabaja por turnos: no hay un turno que ejecutar.

Queda una tarea de mantenimiento, **C-01 T-08, desde el 2026-11-20**:
- Borrar los dos índices que se guardan para volver atrás: `elastiflix-dq-staging-20261113-195801` y
  `elastiflix-reloaded-20261112-144129`.
- Hacerlo solo con la confirmación de la dueña del proyecto. Los comandos están en «Próximo paso» de
  [`BITACORA.md`](BITACORA.md#2-próximo-paso).
- Después viene la revisión final R-21b, con la retrospectiva del equipo.

Hasta esa fecha, nada de cargas bulk en producción ni en staging: la limpieza de huérfanos borraría la vuelta atrás.

## Empezar

| Si quieres… | Lee |
|---|---|
| Entender qué se hizo, qué se aprendió y qué queda abierto | [`APRENDIZAJES.md`](APRENDIZAJES.md) |
| Ver el antes y el después de cada hallazgo | [`informe/informe-calidad-v2.html`](informe/informe-calidad-v2.html) (ábrelo en el navegador) |
| Saber qué se decidió, quién lo decidió y con qué palabras | [`BITACORA.md` §3 y §4](BITACORA.md#3-aprobaciones-y-decisiones-de-la-dueña-del-proyecto) |
| Las métricas de relevancia M0 a M4 | [`BITACORA.md` §6](BITACORA.md#6-métricas-de-relevancia) |
| Preparar una ronda 5 | [`APRENDIZAJES.md` §10](APRENDIZAJES.md#10-lo-que-queda-abierto-para-una-ronda-5), y después [`PLAN.md`](PLAN.md) y [`AGENTS.md`](AGENTS.md) como modelo de plan y de protocolo del equipo |

Para la ronda 4 trabajaron, por turnos, Google Antigravity, Cursor, GitHub Copilot y Gemma 4 E4B como ejecutores, y
Claude Code como revisor después de cada turno. El reparto completo está en `BITACORA.md` §1.1.

## Qué hay aquí

| Ruta | Contenido |
|---|---|
| `APRENDIZAJES.md` | Memoria de la ronda: qué se hizo, resultados, lecciones, lo que queda abierto y la documentación que toca |
| `informe/informe-calidad.html` | Informe del análisis del 12-nov-2026 (índice `elastiflix-reloaded-20261112-144129`, 6 899 películas) |
| `informe/informe-calidad-v2.html` | Informe v2: el estado de cada hallazgo y de las 10 recomendaciones después de la ronda |
| `hallazgos/hallazgos.csv` | Un hallazgo por fila: `id`, título, año, severidad, categoría, campo y detalle (2 514 filas, 2 401 películas) |
| `hallazgos/listas/` | Alcance congelado de cada WP (`.tsv` con `id` y título) |
| `hallazgos/salidas/m0/` | Línea base: salida de cada script del análisis, mapping vivo y listas de ids. No se sobrescribe |
| `hallazgos/salidas/<etiqueta>/` | Mediciones posteriores (`wp-05`, `m1` a `m4`…), una carpeta por corrida |
| `scripts/` | Análisis en Python y herramientas del plan |
| `PLAN.md` · `AGENTS.md` · `BITACORA.md` · `bitacora/` | Plan, protocolo del equipo, tablero, diarios por WP e informes de revisión (R-00 a R-22) |
| `cambios/` | Los 85 lotes de cambios a `data/`, cada WP con su `estado.tsv`: el resultado y el motivo por película |
| `fuentes/plot-fuentes.tsv` | Atribución de las 5 403 sinopsis de Wikipedia (artículo, `revid` y fecha), como pide CC BY-SA |
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

Las herramientas que cambiaron el catálogo se escribieron en WP-00 y WP-05, y siguen sirviendo para una ronda nueva:

| Herramienta | Qué hace |
|---|---|
| `catalogo.py` | Lee y escribe las 7 partes de `data/` en JSON y CSV a la vez, con el mapping del repo en el sobre |
| `aplicar_cambios.py` | Aplica un lote a `data/`; es idempotente y rechaza el lote entero si un valor «antes» ya no coincide |
| `wiki_plot.py` | Busca la trama de una película en Wikipedia (por `imdb_id` → Wikidata) con las guardas contra tramas de otra obra |
| `proponer_trama.py` | Escribe los lotes de sinopsis de Wikipedia o de TMDB y su `estado.tsv` |
| `limpiar_enciclopedico.py` | Quita párrafos completos de ficha, producción o recepción, sin reescribir nada |
| `revisar_reparto.py` | Solo lectura: el estado de las películas cuya sinopsis nombra a protagonistas que no están en `cast` (se agregó con P-13) |
| `cargar.py` | Carga `data/` en staging con la app; se niega a cargar producción |
| `publicar.py` | Publica un índice de staging cambiando el alias de producción (`--revertir` vuelve atrás y `--borrar-anterior` borra un índice viejo); exige la aprobación de la dueña del proyecto |
