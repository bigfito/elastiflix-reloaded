# 📒 Bitácora · calidad del catálogo, ronda 4

Tablero del plan. Vive en la rama `dq/ronda-4`; reglas de uso en [`AGENTS.md` §5 y §6](AGENTS.md#5-turno-de-un-ejecutor).
Fechas en UTC, ISO 8601. Los diarios de cada WP y los informes de revisión están en [`bitacora/`](bitacora/).

**Última actualización:** 2026-11-12 19:35 UTC · Claude Code (Claude Opus 5.5) · revisor (R-05)
**Equipo:** ejecutan Google Antigravity, Cursor y GitHub Copilot; revisa y corrige Claude Code; decide la dueña del proyecto
**Orden de turnos:** Antigravity → Claude Code → Cursor → Claude Code → GitHub Copilot → Claude Code → …
**Dónde se trabaja:** worktree `E:\02 - programming\03 - elastiflix-reloaded-dq`, rama `dq/ronda-4`, solo en local (P-04)
**Staging:** alias `elastiflix-dq-staging` · índice `elastiflix-dq-staging-20261112-174954` (6 899 películas) · `data/` cargado en el commit `5c4b78e` · app compilada en el commit `5c4b78e` · instancia apagada (puerto 8081)

## 1. Tablero

### 1.1 Turnos

Estados: `PENDIENTE` → `EN CURSO` → `EN REVISIÓN` (o `INCOMPLETO`) → `REVISADO`, o `EN ESPERA` en el turno 21.
Etiquetas: `dq-t<NN>` al cerrar el turno y `dq-r<NN>` al cerrar su revisión. `dq-r00` marca el punto de partida.

| Turno | Ejecutor | Tareas (detalle en el plan) | Estado | Inicio | Etiqueta `dq-t` | Revisión |
|---|---|---|---|---|---|---|
| 01 | Google Antigravity | WP-00 T-01 a T-04 | REVISADO | 2026-11-12 | `dq-t01` | [R-01](bitacora/R-01.md) |
| 02 | Cursor | WP-00 T-05 y T-09 | REVISADO | 2026-11-12 | `dq-t02` | [R-02](bitacora/R-02.md) |
| 03 | GitHub Copilot | WP-00 T-06, T-07, T-08 y T-10 | REVISADO | 2026-11-12 | `dq-t03` | [R-03](bitacora/R-03.md) |
| 04 | Google Antigravity | WP-01 y WP-02 `lote-001` | REVISADO | 2026-11-12 | `dq-t04` | [R-04](bitacora/R-04.md) |
| 05 | Cursor | WP-02 lotes `002`–`006` | REVISADO | 2026-11-12 | `dq-t05` | [R-05](bitacora/R-05.md) |
| 06 | GitHub Copilot | WP-02 lotes `009`–`013` (500 desde `68727`; P-07) | PENDIENTE | — | — | — |
| 07 | Google Antigravity | WP-02 lotes `014`–`017` (302 restantes; P-07), T-04 a T-06 (M1) · hito A | PENDIENTE | — | — | — |
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
| WP-00 | Preparación del equipo | HECHA | 1, 2, 3 | 2026-11-12 |
| WP-01 | Respuestas de asistente y datos inventados | HECHA | 4 | 2026-11-12 |
| WP-02 | Reescribir los ensayos generados | EN CURSO · 600/1 402 aplicadas y revisadas (2 corregidas en R-05) | 4, 5, 6, 7 | 2026-11-12 |
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
| R-01 | 01 · Google Antigravity | APROBADO CON CORRECCIONES | 3 defectos de validación en `catalogo.py` y `aplicar_cambios.py`; trazabilidad de correcciones en `estado.tsv` | `fb06ad8` | [R-01](bitacora/R-01.md) |
| R-02 | 02 · Cursor | APROBADO CON CORRECCIONES | Límite de Wikimedia (429), subtítulos dentro de la trama, comilla final, `--agente` por defecto | `83af348` | [R-02](bitacora/R-02.md) |
| R-03 | 03 · GitHub Copilot | APROBADO CON CORRECCIONES | Guardas de borrado de `publicar.py` (alias y comodines); comando del harness sobre staging | `0ab67e4` | [R-03](bitacora/R-03.md) |
| R-04 | 04 · Google Antigravity | APROBADO | Ninguno que corregir: 113 tramas correctas; T-02 de WP-01 cerrada | — | [R-04](bitacora/R-04.md) |
| R-05 | 05 · Cursor | APROBADO CON CORRECCIONES | 2 tramas de otra obra (*Gundam I* → serie de TV; *Jackass 3.5* → *Jackass 3D*), guarda nueva en `wiki_plot.py` | `4ea1d8d` `9c0f2ff` `70c638b` | [R-05](bitacora/R-05.md) |

## 2. Próximo paso

> **Turno 06 · GitHub Copilot:** WP-02, 5 lotes de 100 desde `68727` *Trance* (`proponer_trama.py` los numerará
> `009` a `013`; decisión P-07). Empieza con el protocolo de `AGENTS.md` §5: lee primero [R-05](bitacora/R-05.md),
> revisa sus commits `4ea1d8d`, `9c0f2ff` y `70c638b`, y anota en tu diario que los leíste. El punto de partida es la
> etiqueta `dq-r05`.

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
| D-05 | ¿Qué contacto va en el `User-Agent` para Wikipedia y Wikidata (NS-7)? Sin contacto, Wikimedia limita a 10 peticiones por minuto y las unas 5 950 descargas de los turnos 4 a 13 tardan 11 h o más en total; con contacto, menos de 2 h. El valor va en `.env` como `DQ_WIKI_CONTACTO=` y no se commitea | URL pública del repo · un correo · sin contacto | Lo que prefiera la dueña del proyecto: es un dato suyo que se envía a Wikimedia | APROBADA 2026-11-12 · «La URL del repo» · `DQ_WIKI_CONTACTO=https://github.com/bigfito/elastiflix-reloaded` en el `.env` del worktree |
| H-2 | ¿Se publica el catálogo nuevo cambiando `elastiflix-reloaded` al índice de staging (cambio atómico, con vuelta atrás), y se borra el índice anterior a los 7 días? (C-01) | Sí · No | Se decide con M3 | ABIERTA · se pregunta en C-01 T-06 |

## 4. Decisiones tomadas

| Fecha | ID | Decisión | Quién |
|---|---|---|---|
| 2026-11-12 | P-01 | El alcance de cada WP es el de `hallazgos/listas/` (congelado sobre el índice `elastiflix-reloaded-20261112-144129`) | COORD, al crear el plan |
| 2026-11-12 | P-02 | Las correcciones se hacen en `data/` y llegan al índice por la aplicación; producción solo cambia con `publicar.py` y H-2 | COORD, al crear el plan |
| 2026-11-12 | P-03 | Todo el trabajo y esta bitácora viven en `dq/ronda-4`, trabajada desde un worktree; a `main` solo llegan los hitos A, B y C | COORD, tras la revisión independiente del plan |
| 2026-11-12 | P-04 | Solo en local: worktree `E:\02 - programming\03 - elastiflix-reloaded-dq` en `dq/ronda-4`, creado desde `main` (`9a1b864`). Sin `push` ni `pull`; `main` y `origin` no se tocan. La dueña integra los hitos con `git merge --no-ff` y decide si publica | Dueña del proyecto (P-0) |
| 2026-11-12 | P-05 | Equipo: ejecutan Google Antigravity, Cursor y GitHub Copilot, 7 turnos cada uno en orden fijo; Claude Code revisa y corrige después de cada turno y no toma tareas del plan. Reparto en `PLAN.md` · Reparto y turnos | Dueña del proyecto |
| 2026-11-12 | P-06 | Qwen 2.5 Coder queda fuera del equipo de ejecutores; GitHub Copilot trabaja con el modelo Kimi K3 | Dueña del proyecto |
| 2026-11-12 | D-01 | Largo máximo de `plot` de Wikipedia: 3 000 caracteres, con corte en fin de oración | Dueña del proyecto |
| 2026-11-12 | D-02 | Opción A: se mantienen `vote_average` y `user_score` y se rotulan; WP-09 implementa sin memo | Dueña del proyecto |
| 2026-11-12 | D-03 | `"dynamic": "strict"` en `elasticsearch/elastiflix-reloaded-mapping.json` (WP-10) | Dueña del proyecto |
| 2026-11-12 | D-04 | El filtro `adult` se queda; se documenta que el catálogo de `data/` no tiene contenido adulto (WP-10) | Dueña del proyecto |
| 2026-11-12 | D-05 | Contacto del `User-Agent` para Wikimedia: la URL pública del repo. Va en `.env` como `DQ_WIKI_CONTACTO` (no se commitea); `wiki_plot.py` pasa a 1 petición por segundo | Dueña del proyecto |
| 2026-11-12 | P-07 | Los lotes de corrección de R-05 tomaron los números `007` y `008`. Los números de lote los asigna `proponer_trama.py`; las películas de cada turno no cambian (turno 06: 500 desde `68727`, lotes `009`–`013`; turno 07: las 302 restantes, `014`–`017`) | Claude Code (R-05) |

## 5. Bloqueos abiertos

Ninguno.

## 6. Métricas de relevancia

Valores de `target/eval/report.md` de `RelevanceIT`, en formato `nDCG@10 · MRR@10`. M0 es producción; M1, M2 y M3 son
staging. Criterio de parada: [WP-02](PLAN.md#wp-02--reescribir-los-ensayos-generados).

| Configuración | Mínimo nDCG actual | M0 | M1 | M2 | M3 | Producción final |
|---|---|---|---|---|---|---|
| `bm25_title` | 0.06 | 0.113 · 0.125 | | | | |
| `bm25_multi_match` | 0.06 | 0.115 · 0.125 | | | | |
| `bm25_plot` | 0.37 | 0.423 · 0.444 | | | | |
| `knn_plot_256` | 0.49 | 0.549 · 0.536 | | | | |
| `knn_plot_512` | 0.60 | 0.651 · 0.653 | | | | |
| `knn_plot_1024` | 0.69 | 0.742 · 0.755 | | | | |
| `rrf_title_knn256` | 0.29 | 0.348 · 0.316 | | | | |
| `rrf_title_knn1024` | 0.41 | 0.464 · 0.422 | | | | |
| `rrf_plot_knn256` | 0.53 | 0.581 · 0.612 | | | | |
| `rrf_plot_knn1024` | 0.61 | 0.662 · 0.677 | | | | |

M0 medida el 2026-11-12 17:48 UTC (turno 03) sobre el índice de producción `elastiflix-reloaded-20261112-144129`;
`target/eval/report.md` lleva la hora de esa corrida (`RelevanceIT`, 1 prueba, 0 fallos, 0 omitidas).

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

### 2026-11-12 15:55 UTC · Claude Code (Claude Opus 5.5) · COORD
- Hice: análisis completo del índice vivo en solo lectura; informe en `informe/informe-calidad.html`; detalle por
  película en `hallazgos/hallazgos.csv`; alcance congelado en `hallazgos/listas/`; plan, protocolo y bitácora.
- Evidencia: `hallazgos/salidas/m0/*.txt`. `analizar.py --desde-data` reproduce `hallazgos.csv` byte a byte, así que
  `data/` y el índice coinciden.
- Quedó pendiente: todo el plan.

### 2026-11-12 16:10 UTC · Claude Code (Claude Opus 5.5) · COORD
- Hice: revisión independiente del plan contra el código (23 observaciones) y correcciones. Publicación por cambio
  atómico de alias con vuelta atrás; bitácora única en `dq/ronda-4` con worktree; `es.py` solo escribe en staging;
  mediciones por etiqueta con `m0` congelada; `--desde-data`; `estado.tsv` por WP; WP-03 se integra tras WP-05;
  regla de umbrales corregida (medición − 0.05).
- Quedó pendiente: P-0 y H-1 de la dueña del proyecto.
- Para el siguiente: ver «Próximo paso».

### 2026-11-12 16:16 UTC · Claude Code (Claude Opus 5.5) · COORD
- Hice: P-0 según la respuesta de la dueña (P-04). Worktree `E:\02 - programming\03 - elastiflix-reloaded-dq` en la
  rama local `dq/ronda-4` desde `main` (`9a1b864`); `data-quality/` movido ahí y commiteado; `.env` copiado sin
  mostrarlo. `AGENTS.md`, `PLAN.md` y `README.md` adaptados al modo local (sin push ni PR; hitos por merge local).
- Evidencia: `git worktree list`; `analizar.py --etiqueta p0 --desde-data` reproduce `hallazgos.csv` desde el worktree.
- Quedó pendiente: H-1.
- Para el siguiente: ver «Próximo paso».

### 2026-11-12 16:27 UTC · Claude Code (Claude Opus 5.5) · revisor
- Hice: P-05 a pedido de la dueña del proyecto. Reparto del plan en 21 turnos (7 por ejecutor, 2 grandes cada uno);
  protocolo de turno del ejecutor y de revisión de Claude Code (`AGENTS.md` §5 y §6, plantilla `R-NN.md`);
  tablero de turnos y de revisiones; etiqueta de partida `dq-r00`. GitHub Copilot entra en lugar de Qwen.
- Quedó pendiente: H-1 y D-01 a D-04.
- Para el siguiente: turno 01, Google Antigravity.

### 2026-11-12 16:30 UTC · Claude Code (Claude Opus 5.5) · revisor
- Hice: anoté la respuesta de la dueña del proyecto, «Autorizo H-1 y acepto las recomendaciones de D-01 a D-04»: H-1, D-01, D-02 (opción A), D-03 (sí) y D-04 (no se
  quita) en APROBADA, con su fila en «Decisiones tomadas».
- Evidencia: `es.aprobada("H-1")` devuelve `True`; `es.aprobada("H-2")`, `False`.
- Quedó pendiente: H-2, que se pide en el turno 21.
- Para el siguiente: turno 01, Google Antigravity.

### 2026-11-12 16:55 UTC · Claude Code (Claude Opus 5.5) · revisor (R-01)
- Hice: revisión del turno 01 de Google Antigravity. APROBADO CON CORRECCIONES: tres defectos de validación
  corregidos (`fb06ad8`), dos líneas sin commitear descartadas, y las horas de mis entradas anteriores corregidas
  según los commits. Detalle en `bitacora/R-01.md`.
- Evidencia: `./mvnw -q verify` (798 pruebas, 0 fallos); `catalogo.py --verificar` 14/14; `aplicar_cambios.py
  --verificar` con los tres rechazos nuevos.
- Para el siguiente: turno 02, Cursor.

### 2026-11-12 17:45 UTC · Claude Code (Claude Opus 5.5) · revisor (R-02)
- Hice: P-06 registrada antes de la revisión (`4e25443`). Revisión del turno 02 de Cursor: APROBADO CON
  CORRECCIONES (`83af348`). Wikimedia limita a 10 peticiones por minuto a un `User-Agent` sin contacto; se abre
  D-05 y se ajusta NS-7. Detalle en `bitacora/R-02.md`.
- Evidencia: `wiki_plot.py --verificar` y `proponer_trama.py --comprobar` en verde; lote real de las 13 de WP-01
  propuesto en `work/r02-wp01/` (13 de 13 con trama) y simulado sin escribir `data/`.
- Para el siguiente: turno 03, GitHub Copilot.

### 2026-11-12 18:00 UTC · Claude Code (Claude Opus 5.5) · revisor (R-02)
- Hice: D-05 aprobada por la dueña del proyecto («La URL del repo»). Agregué
  `DQ_WIKI_CONTACTO=https://github.com/bigfito/elastiflix-reloaded` al `.env` del worktree, que no se commitea.
- Evidencia: el `User-Agent` es `elastiflix-data-quality/1.0 (https://github.com/bigfito/elastiflix-reloaded)
  python-urllib`, con pausa de 1 s; 8 de 8 tramas de `wp02-prioridad.tsv` descargadas sin ningún 429.
- Para el siguiente: turno 03, GitHub Copilot.

### 2026-11-12 18:15 UTC · Claude Code (Claude Opus 5.5) · revisor (R-03)
- Hice: revisión del turno 03 de GitHub Copilot. APROBADO CON CORRECCIONES (`0ab67e4`): guardas de borrado en
  `publicar.py` y comando del harness sobre staging en `AGENTS.md` §9. WP-00 queda HECHA. Detalle en
  `bitacora/R-03.md`.
- Evidencia: producción intacta (`elastiflix-reloaded-20261112-144129`, 6 899); staging con 6 899 y comprobaciones de
  `publicar.py` en verde; harness sobre staging repetido con el comando corregido, dentro de ±0.02 de M0.
- Para el siguiente: turno 04, Google Antigravity.

### 2026-11-12 18:45 UTC · Claude Code (Claude Opus 5.5) · revisor (R-04)
- Hice: revisión del turno 04 de Google Antigravity. APROBADO sin correcciones. Cerré T-02 de WP-01 (lectura de las
  13) y WP-01 queda HECHA; WP-02 `lote-001` aceptado. Detalle en `bitacora/R-04.md`.
- Evidencia: `data/` cambió solo en las 113 películas de los lotes; `./mvnw -q verify` en verde (798);
  `analizar.py --etiqueta r04 --desde-data` en `hallazgos/salidas/r04/`.
- Para el siguiente: turno 05, Cursor.

### 2026-11-12 19:35 UTC · Claude Code (Claude Opus 5.5) · revisor (R-05)
- Hice: revisión del turno 05 de Cursor. APROBADO CON CORRECCIONES: dos tramas venían del artículo de otra obra
  (*Mobile Suit Gundam I* y *Jackass 3.5*) y se corrigieron con los lotes `007` y `008` (overview de TMDB);
  `wiki_plot.py` descarta artículos de series, videojuegos y manga. Decisión P-07 sobre los números de lote.
  Detalle en `bitacora/R-05.md`.
- Evidencia: `data/` cambió solo en las 500 películas de los lotes y en las 2 corregidas; `./mvnw -q verify` en
  verde; `hallazgos/salidas/r05/`.
- Para el siguiente: turno 06, GitHub Copilot.
