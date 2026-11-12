# 📒 Bitácora · calidad del catálogo, ronda 4

Tablero del plan. Vive en la rama `dq/ronda-4`; reglas de uso en [`AGENTS.md` §4](AGENTS.md#4-protocolo-de-sesión).
Fechas en UTC, ISO 8601. Los diarios detallados están en [`bitacora/`](bitacora/).

**Última actualización:** 2026-11-12 17:30 UTC · Claude Code (Claude Opus 5.5) · COORD
**Dónde se trabaja:** worktree `E:\02 - programming\03 - elastiflix-reloaded-dq`, rama `dq/ronda-4`, solo en local (P-04)
**Llave de `data/`:** libre
**Staging:** no existe todavía · alias `elastiflix-dq-staging` · índice — · `data/` cargado en el commit — · app compilada en el commit — · instancia — (puerto 8081, apagada)

## 1. Tablero

| WP | Título | Estado | Responsable | Rama | Actualizado |
|---|---|---|---|---|---|
| WP-00 | Preparación del equipo | PENDIENTE | — | `dq/ronda-4` | — |
| WP-01 | Respuestas de asistente y datos inventados | PENDIENTE | — | `dq/ronda-4` | — |
| WP-02 | Reescribir los ensayos generados | PENDIENTE | — | `dq/ronda-4` | — |
| WP-03 | Ampliar las reglas de advertencia | PENDIENTE | — | `dq/wp-03-reglas-plot` | — |
| WP-04 | Etiquetar el origen de todas las sinopsis | PENDIENTE | — | `dq/ronda-4` | — |
| WP-05 | Limpiar el plot enciclopédico | PENDIENTE | — | `dq/ronda-4` | — |
| WP-06 | Completar las sinopsis cortas | PENDIENTE | — | `dq/ronda-4` | — |
| WP-07 | Normalizar al cargar | PENDIENTE | — | `dq/ronda-4` | — |
| WP-08 | Corregir palabras clave y colecciones | PENDIENTE | — | `dq/ronda-4` | — |
| WP-09 | Una sola puntuación y montos imposibles | PENDIENTE | — | `dq/ronda-4` | — |
| WP-10 | Opcional: `dynamic: strict` y filtro `adult` | PENDIENTE | — | `dq/wp-10-mapping` | — |
| C-01 | Cierre: medición, publicación y documentación | PENDIENTE | — | `dq/ronda-4` | — |

## 2. Próximo paso

> **COORD/DATOS:** en el worktree `E:\02 - programming\03 - elastiflix-reloaded-dq`, tomar **WP-00** y empezar por
> T-01. Las tareas T-01 a T-07 no necesitan staging. **Dueña del proyecto:** contestar **H-1** antes de T-08.

## 3. Aprobaciones y decisiones de la dueña del proyecto

Formato del estado: `ABIERTA`, `APROBADA AAAA-MM-DD · «texto literal»` o `RECHAZADA AAAA-MM-DD · «texto literal»`.
Solo la dueña del proyecto, o un agente copiando su respuesta literal, cambia esta columna.

| ID | Pregunta | Opciones | Recomendación | Estado |
|---|---|---|---|---|
| P-0 | Precondición: dónde vive el trabajo. Se planteó commitear `data-quality/` en `main` y publicar `dq/ronda-4` | En `main` y remoto · Solo en local | Lo que decida la dueña | APROBADA 2026-11-12 · «Vamos a trabajar con la copia de nuestro repositorio local.» · se aplicó como P-04 |
| H-1 | ¿Se puede crear y recargar el alias `elastiflix-dq-staging` en el proyecto Serverless (índices nuevos con vectores generados por inferencia; unas 4 recargas completas en todo el plan)? | Sí · No | Sí: permite medir sin tocar producción | ABIERTA |
| D-01 | Largo máximo de `plot` tomado de Wikipedia | 3 000 caracteres · otro valor | 3 000, con corte en fin de oración (hoy p95 = 2 670) | ABIERTA · mientras no se conteste rige 3 000 |
| D-02 | ¿Qué puntuación muestra la demo? (WP-09) | A · B · C del plan | A: mantener las dos y rotularlas | ABIERTA · se pregunta con el memo de WP-09 |
| D-03 | ¿`"dynamic": "strict"` en el mapping? (WP-10) | Sí · No | Sí | ABIERTA · WP-10 espera esta respuesta |
| D-04 | ¿Quitar el filtro `adult`? (WP-10) | Sí · No | No; documentar | ABIERTA · WP-10 espera esta respuesta |
| H-2 | ¿Se publica el catálogo nuevo cambiando `elastiflix-reloaded` al índice de staging (cambio atómico, con vuelta atrás), y se borra el índice anterior a los 7 días? (C-01) | Sí · No | Se decide con M3 | ABIERTA · se pregunta en C-01 T-06 |

## 4. Decisiones tomadas

| Fecha | ID | Decisión | Quién |
|---|---|---|---|
| 2026-11-12 | P-01 | El alcance de cada WP es el de `hallazgos/listas/` (congelado sobre el índice `elastiflix-reloaded-20261112-144129`) | COORD, al crear el plan |
| 2026-11-12 | P-02 | Las correcciones se hacen en `data/` y llegan al índice por la aplicación; producción solo cambia con `publicar.py` y H-2 | COORD, al crear el plan |
| 2026-11-12 | P-03 | Todo el trabajo y esta bitácora viven en `dq/ronda-4`, trabajada desde un worktree; a `main` solo llegan los hitos A, B y C | COORD, tras la revisión independiente del plan |
| 2026-11-12 | P-04 | Solo en local: worktree `E:\02 - programming\03 - elastiflix-reloaded-dq` en `dq/ronda-4`, creado desde `main` (`9a1b864`). Sin `push` ni `pull`; `main` y `origin` no se tocan. La dueña integra los hitos con `git merge --no-ff` y decide si publica | Dueña del proyecto (P-0) |

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
