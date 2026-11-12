# 📒 Bitácora · calidad del catálogo, ronda 4

Tablero del plan. Vive en la rama `dq/ronda-4`; reglas de uso en [`AGENTS.md` §5 y §6](AGENTS.md#5-turno-de-un-ejecutor).
Fechas en UTC, ISO 8601. Los diarios de cada WP y los informes de revisión están en [`bitacora/`](bitacora/).

**Última actualización:** 2026-11-12 16:43 UTC · Google Antigravity (Gemini 4 Argon) · ejecutor (turno 01)
**Equipo:** ejecutan Google Antigravity, Cursor y GitHub Copilot; revisa y corrige Claude Code; decide la dueña del proyecto
**Orden de turnos:** Antigravity → Claude Code → Cursor → Claude Code → GitHub Copilot → Claude Code → …
**Dónde se trabaja:** worktree `E:\02 - programming\03 - elastiflix-reloaded-dq`, rama `dq/ronda-4`, solo en local (P-04)
**Staging:** no existe todavía · alias `elastiflix-dq-staging` · índice — · `data/` cargado en el commit — · app compilada en el commit — · instancia — (puerto 8081, apagada)

## 1. Tablero

### 1.1 Turnos

Estados: `PENDIENTE` → `EN CURSO` → `EN REVISIÓN` (o `INCOMPLETO`) → `REVISADO`, o `EN ESPERA` en el turno 21.
Etiquetas: `dq-t<NN>` al cerrar el turno y `dq-r<NN>` al cerrar su revisión. `dq-r00` marca el punto de partida.

| Turno | Ejecutor | Tareas (detalle en el plan) | Estado | Inicio | Etiqueta `dq-t` | Revisión |
|---|---|---|---|---|---|---|
| 01 | Google Antigravity | WP-00 T-01 a T-04 | EN REVISIÓN | 2026-11-12 | `dq-t01` | — |
| 02 | Cursor | WP-00 T-05 y T-09 | PENDIENTE | — | — | — |
| 03 | GitHub Copilot | WP-00 T-06, T-07, T-08 y T-10 | PENDIENTE | — | — | — |
| 04 | Google Antigravity | WP-01 y WP-02 `lote-001` | PENDIENTE | — | — | — |
| 05 | Cursor | WP-02 lotes `002`–`006` | PENDIENTE | — | — | — |
| 06 | GitHub Copilot | WP-02 lotes `007`–`011` | PENDIENTE | — | — | — |
| 07 | Google Antigravity | WP-02 lotes `012`–`015`, T-04 a T-06 (M1) · hito A | PENDIENTE | — | — | — |
| 08 | Cursor | WP-03 T-01, T-02 y T-04 | PENDIENTE | — | — | — |
| 09 | GitHub Copilot | WP-04 | PENDIENTE | — | — | — |
| 10 | Google Antigravity | WP-05 T-01 y lotes `001`–`010` | PENDIENTE | — | — | — |
| 11 | Cursor | WP-05 lotes `011`–`025` | PENDIENTE | — | — | — |
| 12 | GitHub Copilot | WP-05 lotes `026`–`039`, T-03 y T-05 | PENDIENTE | — | — | — |
| 13 | Google Antigravity | WP-03 T-03 y WP-06 T-01 y T-02 | PENDIENTE | — | — | — |
| 14 | Cursor | WP-06 T-03 (M2) y WP-07 parte A · hito B | PENDIENTE | — | — | — |
| 15 | GitHub Copilot | WP-07 parte B | PENDIENTE | — | — | — |
| 16 | Google Antigravity | WP-08 | PENDIENTE | — | — | — |
| 17 | Cursor | WP-09 | PENDIENTE | — | — | — |
| 18 | GitHub Copilot | WP-10 | PENDIENTE | — | — | — |
| 19 | Google Antigravity | C-01 T-01 a T-03 (M3) | PENDIENTE | — | — | — |
| 20 | Cursor | C-01 T-04 y T-05 · hito C | PENDIENTE | — | — | — |
| 21 | GitHub Copilot | C-01 T-06 a T-08 (H-2) | PENDIENTE | — | — | — |

### 1.2 Paquetes de trabajo

| WP | Título | Estado | Turnos | Actualizado |
|---|---|---|---|---|
| WP-00 | Preparación del equipo | EN CURSO | 1, 2, 3 | 2026-11-12 |
| WP-01 | Respuestas de asistente y datos inventados | PENDIENTE | 4 | — |
| WP-02 | Reescribir los ensayos generados | PENDIENTE | 4, 5, 6, 7 | — |
| WP-03 | Ampliar las reglas de advertencia (rama `dq/wp-03-reglas-plot`) | PENDIENTE | 8, 13 | — |
| WP-04 | Etiquetar el origen de todas las sinopsis | PENDIENTE | 9 | — |
| WP-05 | Limpiar el plot enciclopédico | PENDIENTE | 10, 11, 12 | — |
| WP-06 | Completar las sinopsis cortas | PENDIENTE | 13, 14 | — |
| WP-07 | Normalizar al cargar | PENDIENTE | 14, 15 | — |
| WP-08 | Corregir palabras clave y colecciones | PENDIENTE | 16 | — |
| WP-09 | Una sola puntuación y montos imposibles | PENDIENTE | 17 | — |
| WP-10 | Opcional: `dynamic: strict` y filtro `adult` | PENDIENTE | 18 | — |
| C-01 | Cierre: medición, publicación y documentación | PENDIENTE | 19, 20, 21 | — |

### 1.3 Revisiones de Claude Code

| Revisión | Turno revisado | Veredicto | Hallazgos corregidos | Commits de corrección | Informe |
|---|---|---|---|---|---|
| R-00 | (plan inicial) | APROBADO | 23 de la revisión independiente del plan | `ebab666` y siguiente | Diario del plan, §8 |

## 2. Próximo paso

> **Claude Code: revisión R-01** del turno 01 de Google Antigravity (`dq-r00..dq-t01`: WP-00 T-01 a T-04).
> Al aprobarse R-01, sigue el **turno 02 · Cursor** (WP-00 T-05 `wiki_plot.py` y T-09 `proponer_trama.py`).

## 3. Aprobaciones y decisiones de la dueña del proyecto

Formato del estado: `ABIERTA`, `APROBADA AAAA-MM-DD · «texto literal»` o `RECHAZADA AAAA-MM-DD · «texto literal»`.
Solo la dueña del proyecto, o un agente copiando su respuesta literal, cambia esta columna.

| ID | Pregunta | Opciones | Recomendación | Estado |
|---|---|---|---|---|
| P-0 | Precondición: dónde vive el trabajo. Se planteó commitear `data-quality/` en `main` y publicar `dq/ronda-4` | En `main` y remoto · Solo en local | Lo que decida la dueña | APROBADA 2026-11-12 · «Vamos a trabajar con la copia de nuestro repositorio local.» · se aplicó como P-04 |
| H-1 | ¿Se puede crear y recargar el alias `elastiflix-dq-staging` en el proyecto Serverless (índices nuevos con vectores generados por inferencia; unas 4 recargas completas en todo el plan)? | Sí · No | Sí: permite medir sin tocar producción | APROBADA 2026-11-12 · «Autorizo H-1 y acepto las recomendaciones de D-01 a D-04» |
| D-01 | Largo máximo de `plot` tomado de Wikipedia | 3 000 caracteres · otro valor | 3 000, con corte en fin de oración (hoy p95 = 2 670) | APROBADA 2026-11-12 · «Autorizo H-1 y acepto las recomendaciones de D-01 a D-04» · rige 3 000 |
| D-02 | ¿Qué puntuación muestra la demo? (WP-09) | A · B · C del plan | A: mantener las dos y rotularlas | APROBADA 2026-11-12 · «Autorizo H-1 y acepto las recomendaciones de D-01 a D-04» · opción A; WP-09 T-01 no necesita memo |
| D-03 | ¿`"dynamic": "strict"` en el mapping? (WP-10) | Sí · No | Sí | APROBADA 2026-11-12 · «Autorizo H-1 y acepto las recomendaciones de D-01 a D-04» · sí, `dynamic: strict` |
| D-04 | ¿Quitar el filtro `adult`? (WP-10) | Sí · No | No; documentar | APROBADA 2026-11-12 · «Autorizo H-1 y acepto las recomendaciones de D-01 a D-04» · no se quita; se documenta |
| H-2 | ¿Se publica el catálogo nuevo cambiando `elastiflix-reloaded` al índice de staging (cambio atómico, con vuelta atrás), y se borra el índice anterior a los 7 días? (C-01) | Sí · No | Se decide con M3 | ABIERTA · se pregunta en C-01 T-06 |

## 4. Decisiones tomadas

| Fecha | ID | Decisión | Quién |
|---|---|---|---|
| 2026-11-12 | P-01 | El alcance de cada WP es el de `hallazgos/listas/` (congelado sobre el índice `elastiflix-reloaded-20261112-144129`) | COORD, al crear el plan |
| 2026-11-12 | P-02 | Las correcciones se hacen en `data/` y llegan al índice por la aplicación; producción solo cambia con `publicar.py` y H-2 | COORD, al crear el plan |
| 2026-11-12 | P-03 | Todo el trabajo y esta bitácora viven en `dq/ronda-4`, trabajada desde un worktree; a `main` solo llegan los hitos A, B y C | COORD, tras la revisión independiente del plan |
| 2026-11-12 | P-04 | Solo en local: worktree `E:\02 - programming\03 - elastiflix-reloaded-dq` en `dq/ronda-4`, creado desde `main` (`9a1b864`). Sin `push` ni `pull`; `main` y `origin` no se tocan. La dueña integra los hitos con `git merge --no-ff` y decide si publica | Dueña del proyecto (P-0) |
| 2026-11-12 | P-05 | Equipo: ejecutan Google Antigravity, Cursor y GitHub Copilot, 7 turnos cada uno en orden fijo; Claude Code revisa y corrige después de cada turno y no toma tareas del plan. Reparto en `PLAN.md` · Reparto y turnos | Dueña del proyecto |
| 2026-11-12 | D-01 | Largo máximo de `plot` de Wikipedia: 3 000 caracteres, con corte en fin de oración | Dueña del proyecto |
| 2026-11-12 | D-02 | Opción A: se mantienen `vote_average` y `user_score` y se rotulan; WP-09 implementa sin memo | Dueña del proyecto |
| 2026-11-12 | D-03 | `"dynamic": "strict"` en `elasticsearch/elastiflix-reloaded-mapping.json` (WP-10) | Dueña del proyecto |
| 2026-11-12 | D-04 | El filtro `adult` se queda; se documenta que el catálogo de `data/` no tiene contenido adulto (WP-10) | Dueña del proyecto |

## 5. Bloqueos abiertos

Ninguno.

## 6. Métricas de relevancia

Valores de `target/eval/report.md` de `RelevanceIT`, en formato `nDCG@10 · MRR@10`. M0 es producción; M1, M2 y M3 son
staging. Criterio de parada: [WP-02](PLAN.md#wp-02--reescribir-los-ensayos-generados).

| Configuración | Mínimo nDCG actual | M0 | M1 | M2 | M3 | Producción final |
|---|---|---|---|---|---|---|
| `bm25_title` | 0.06 | | | | | |
| `bm25_multi_match` | 0.06 | | | | | |
| `bm25_plot` | 0.37 | | | | | |
| `knn_plot_256` | 0.49 | | | | | |
| `knn_plot_512` | 0.60 | | | | | |
| `knn_plot_1024` | 0.69 | | | | | |
| `rrf_title_knn256` | 0.29 | | | | | |
| `rrf_title_knn1024` | 0.41 | | | | | |
| `rrf_plot_knn256` | 0.53 | | | | | |
| `rrf_plot_knn1024` | 0.61 | | | | | |

## 7. Indicadores de calidad del catálogo

Cifras de `python data-quality/scripts/analizar.py --etiqueta <X>` (con `--desde-data` mientras no haya recarga).
La columna «Inicial» sale de `hallazgos/salidas/m0/`.

| Indicador | Salida | Inicial | Meta | M3 |
|---|---|---|---|---|
| Plot con 2 o más firmas de texto generado | `llm.txt` («con >=2») | 1 408 | ≤ 50 | |
| Respuestas de asistente y correcciones de año | `llm.txt` | 2 + 6 | 0 | |
| Plot con taquilla o crítica | `plots.txt` («producción/recepción») | 1 549 | < 300 | |
| Plot que abre con la ficha | `plots.txt` («ficha inicial») | 3 734 | < 500 | |
| Plot de menos de 200 caracteres | `plots.txt` («< 200 chars») | 686 | solo las «sin fuente» | |
| Plot sin `enrichment.plot` (6 899 menos la cuenta de `plot` en «enrichment: claves») | `deep.txt` | 5 275 | 0 | |
| Textos con espacios, NBSP, sin NFC o con `&amp;` | `deep.txt` | 124 | 0 | |
| Ids de palabra clave con varios nombres | `deep.txt` | 14 | solo los legítimos de TMDB | |

## 8. Diario del plan

Las entradas de cada WP van en su diario. Aquí solo se anotan hitos del plan completo.

### 2026-11-12 16:30 UTC · Claude Code (Claude Opus 5.5) · COORD
- Hice: análisis completo del índice vivo en solo lectura; informe en `informe/informe-calidad.html`; detalle por
  película en `hallazgos/hallazgos.csv`; alcance congelado en `hallazgos/listas/`; plan, protocolo y bitácora.
- Evidencia: `hallazgos/salidas/m0/*.txt`. `analizar.py --desde-data` reproduce `hallazgos.csv` byte a byte, así que
  `data/` y el índice coinciden.
- Quedó pendiente: todo el plan.

### 2026-11-12 17:30 UTC · Claude Code (Claude Opus 5.5) · COORD
- Hice: revisión independiente del plan contra el código (23 observaciones) y correcciones. Publicación por cambio
  atómico de alias con vuelta atrás; bitácora única en `dq/ronda-4` con worktree; `es.py` solo escribe en staging;
  mediciones por etiqueta con `m0` congelada; `--desde-data`; `estado.tsv` por WP; WP-03 se integra tras WP-05;
  regla de umbrales corregida (medición − 0.05).
- Quedó pendiente: P-0 y H-1 de la dueña del proyecto.
- Para el siguiente: ver «Próximo paso».

### 2026-11-12 17:50 UTC · Claude Code (Claude Opus 5.5) · COORD
- Hice: P-0 según la respuesta de la dueña (P-04). Worktree `E:\02 - programming\03 - elastiflix-reloaded-dq` en la
  rama local `dq/ronda-4` desde `main` (`9a1b864`); `data-quality/` movido ahí y commiteado; `.env` copiado sin
  mostrarlo. `AGENTS.md`, `PLAN.md` y `README.md` adaptados al modo local (sin push ni PR; hitos por merge local).
- Evidencia: `git worktree list`; `analizar.py --etiqueta p0 --desde-data` reproduce `hallazgos.csv` desde el worktree.
- Quedó pendiente: H-1.
- Para el siguiente: ver «Próximo paso».

### 2026-11-12 18:20 UTC · Claude Code (Claude Opus 5.5) · revisor
- Hice: P-05 a pedido de la dueña del proyecto. Reparto del plan en 21 turnos (7 por ejecutor, 2 grandes cada uno);
  protocolo de turno del ejecutor y de revisión de Claude Code (`AGENTS.md` §5 y §6, plantilla `R-NN.md`);
  tablero de turnos y de revisiones; etiqueta de partida `dq-r00`. GitHub Copilot entra en lugar de Qwen.
- Quedó pendiente: H-1 y D-01 a D-04.
- Para el siguiente: turno 01, Google Antigravity.

### 2026-11-12 18:40 UTC · Claude Code (Claude Opus 5.5) · revisor
- Hice: anoté la respuesta de la dueña del proyecto, «Autorizo H-1 y acepto las recomendaciones de D-01 a D-04»: H-1, D-01, D-02 (opción A), D-03 (sí) y D-04 (no se
  quita) en APROBADA, con su fila en «Decisiones tomadas».
- Evidencia: `es.aprobada("H-1")` devuelve `True`; `es.aprobada("H-2")`, `False`.
- Quedó pendiente: H-2, que se pide en el turno 21.
- Para el siguiente: turno 01, Google Antigravity.
