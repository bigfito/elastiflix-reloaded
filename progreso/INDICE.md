# 📍 Índice de estado del plan

> Punto de entrada para retomar el trabajo. Lo mantiene el agente que ejecuta `PLAN.md`.
> Actualízalo **al terminar cada tarea** (y al empezar y cerrar cada fase), en el mismo commit que el
> archivo de la fase, y haz push.
> Reglas completas: `PLAN.md` §9 "Estado del trabajo y reanudación".

## Punto de reanudación

- **Fase actual:** Fase 8 — Interfaz en español e inglés (i18n) (Requerimiento 2)
- **Estado de la fase:** terminado
- **Ramas:** `fase-8-i18n`
- **Diseño:** [`docs/diseno/8.md`](../docs/diseno/8.md) completado en 8.0
- **Estado:** tablero [`progreso/8.md`](8.md)
- **Tarea actual:** Fase 8 completada (8.0 a 8.7 terminadas)
- **Próximo paso:** Preparar PR de la rama `fase-8-i18n` hacia `main` y coordinar inicio del Requerimiento 3
- **Última actualización:** 2026-10-20 05:15 UTC

## Fases

| Fase | Nombre | Compuerta | Estado | Archivo de estado | Verificación de fase |
|---|---|---|---|---|---|
| 0 | Andamiaje y calidad | — | terminado | `progreso/0.md` | 2026-10-17 en verde |
| 1 | Núcleo: conexión, catálogo y embeddings | 🔒 | terminado | `progreso/1.md` | 2026-10-18 en verde |
| 2 | Búsqueda | 🔒 | terminado | `progreso/2.md` | 2026-10-18 en verde |
| 3 | Carga de datos | 🔒 | terminado | `progreso/3.md` | 2026-10-18 en verde |
| 4 | Interfaz web | — | terminado | `progreso/4.md` | 2026-10-18 en verde |
| 5 | Pruebas integrales y smoke test | — | terminado | `progreso/5.md` | 2026-10-18 en verde |
| 6 | Documentación y cierre | — | terminado | `progreso/6.md` | 2026-10-18 en verde |
| — | Revisión 1 | — | terminado | `progreso/revision-1.md` | Fusionada en `main` (`dc9a315`, PR #10). La rama remota `cursor/revision-1-29a6` se borró. Adolfo Orozco aprobó las capturas de R-21. Nueve R quedaron incompletas y continúan en la revisión 2. |
| — | Sincronización de documentación | — | terminado | `progreso/INDICE.md` | Fusionada en `main` (`2f6f9ed`, PR #11). La rama remota `cursor/docs-sync-29a6` se borró. |
| — | Bugs de la revisión de documentación | — | terminado | `progreso/bugs.md` | Fusionada en `main` (`41c1188`, PR #12). La rama remota `cursor/bugs-revision-29a6` se borró. B-04 continúa en S-01. |
| — | Revisión 2 | — | terminada | `progreso/revision-2.md` | Lista en `docs/revisiones/revision-2.md`. S-01 a S-27 terminadas; `verify` y `verify -Pit` en verde. Fusionada en `main` (`7f08810`, PR #13). Se borró la rama `revision-2` (local y remota). El estado posterior entró con el PR #14 (`27fcdef`). La revisión 3 encontró restos de S-01, S-03, S-05, S-06, S-14, S-21 y S-22. |
| — | Revisión 3 | — | terminada | `progreso/revision-3.md` (tablero) y `progreso/revision-3-<carril>.md` | Lista en `docs/revisiones/revision-3.md`. T-01 a T-27 terminadas. Carriles A–F con seis agentes de Claude Code en paralelo (PR #15 a #20, fusionados en `7e360b4`); cierre Z fusionado en `5c21934` (PR #21); las ramas `revision-3-*` se borraron. `./mvnw verify -Pit` en verde (326 unitarias y 29 de integración) y pruebas de navegador 11 de 11, el 2026-10-19. |
| 7 | Detalle de película | 🔒 | terminado | `progreso/7.md` | Fusionada en `main` (`2902be7`, PR #23). Rama remota y local `fase-7-detalle` borrada. `./mvnw clean verify` en verde (80 unitarias web / 329 repo, Checkstyle 0, PMD 0, ArchUnit 7/7). |
| 8 | Interfaz en español e inglés (i18n) | — | terminado | `progreso/8.md` | 8.0 a 8.7 terminadas. `./mvnw clean verify` en verde (104 unitarias web / 347 repo, Checkstyle 0, PMD 0, ArchUnit 7/7). SmokeIT 4/4. |

## Aprobaciones del usuario

| Fecha UTC | Qué se aprobó | Comentarios |
|---|---|---|
| 2026-10-17 | Decisiones sobre el plan (no es aún el visto bueno para programar) | Paquete `com.bigfito.elastiflix`; migración a alias sin aprobación previa; rutas en español; híbrida con `_msearch`; pruebas de integración contra Serverless; sin autenticación; borrar el índice anterior al publicar; registro individual directo al catálogo; CSV sin encabezado se rechaza; vectores siempre generados con inferencia; pósters de TMDB; commit + push por tarea; documentación al cerrar cada fase; estado actualizado en cada tarea; un solo usuario a la vez, sin controles de concurrencia; programa un agente de Cursor; vectores de documento con la combinación calibrada en 1.9 (los archivos no traen vectores y el formulario no los captura); atribución de TMDB en el pie y en «Acerca de»; sin restricción por costo de inferencia |
| 2026-10-17 | Visto bueno del plan e inicio de la Fase 0 | Adolfo (bigfito) pidió explícitamente ejecutar la Fase 0. El agente de Cursor arranca por la tarea 0.1. |
| 2026-10-17 | Fase 0 aprobada y fusionada en `main` | Adolfo revisó y aprobó la Fase 0. El PR #1 quedó fusionado en `main` (merge commit `275478f`). |
| 2026-10-18 | Diseño de la Fase 1 y sus cinco supuestos | Adolfo aprobó `docs/diseno/1.md` tal cual: lotes de 16, no crear catálogo vacío, `belongs_to_collection` como objeto, códigos de otras fases más tarde y tabla de calibración vacía hasta la tarea 1.9. |
| 2026-10-18 | Fase 1 aprobada y fusionada en `main` | Adolfo revisó y aprobó la Fase 1. El PR #2 quedó fusionado en `main` (merge commit `b85555b`). |
| 2026-10-18 | Diseño de la Fase 2 y sus seis supuestos | Adolfo aprobó `docs/diseno/2.md` tal cual: la búsqueda habla con Elasticsearch solo por `SearchTransport`; vectorial e híbrida piden `k` y parten la página en la aplicación; el estado vacío no cuenta el catálogo entero; una página fuera del total pero dentro de la ventana no es un error; las etiquetas «Todos», «Solo no adultos» y «Ver todos» llegan en la Fase 4; «fuera de su top» usa el `k` real. |
| 2026-10-18 | Fila full-text `Kolejne 365 dni` | Adolfo aceptó OR 1 · AND 0 como resultado esperado del `match` sobre `title`. El token `365` coincide con «The Next 365 Days» (`829560`). |
| 2026-10-18 | Fase 2 aprobada y fusionada en `main` | Adolfo revisó y aprobó la Fase 2. El PR #3 quedó fusionado en `main` (merge commit `6d98bb9`). |
| 2026-10-18 | Diseño de la Fase 3 y sus supuestos | Adolfo aprobó `docs/diseno/3.md` tal cual. Incluye borrar el índice anterior al publicar, el registro individual directo al alias, sin controles de concurrencia, rechazar un CSV sin encabezado y generar siempre los vectores con inferencia. |
| 2026-10-18 | Fase 3 aprobada y fusionada en `main` | Adolfo revisó y aprobó la Fase 3. El PR #4 quedó fusionado en `main` (merge commit `05ad8f8`). |
| 2026-10-18 | Fase 4 aprobada y fusionada en `main` | Adolfo revisó y aprobó la Fase 4, incluida la interfaz de las capturas. El PR #5 quedó fusionado en `main` (merge commit `c64aa8a`). |
| 2026-10-18 | Fase 5 aprobada y fusionada en `main` | Adolfo revisó y aprobó la Fase 5. El PR #6 quedó fusionado en `main` (merge commit `68c1a24`). |
| 2026-10-18 | Fase 6 aprobada y fusionada en `main` | Adolfo revisó y aprobó la Fase 6. El PR #7 quedó fusionado en `main` (merge commit `eb309d9`). |
| 2026-10-18 | Corrección de documentación fusionada en `main` | Adolfo aprobó el PR #8. Quedó fusionado en `main` (merge commit `22dac52`). Después se borraron las ramas remotas `cursor/*` de las fases 0 a 6. |
| 2026-10-18 | Criterios de §6 relajados, fusionados en `main` | El PR #9 (`cursor/revision-docs-29a6`, merge `dc44535`, commit `2a3aaea`) dejó de afirmar los totales de 4 322 y lee `_count`. La revisión 1 (R-05, D1) restituye los valores exactos sobre 6 899 películas. |
| 2026-10-18 | Capturas de R-21 | Adolfo Orozco aprobó la fidelidad visual de las cinco capturas en `docs/capturas/` (inicio, resultados, cargar, acerca, no-encontrada). Sustituida por S-18: esas capturas no reflejaban la interfaz y se reemplazaron; la aprobación nueva va aparte. |
| 2026-10-18 | Capturas de S-18 | Adolfo Orozco aprobó las nueve capturas de página completa con datos reales en `docs/capturas/` y la comparación de `progreso/4.md` («S-18 apruebo»), después de tomadas (`20b1f59`). |
| 2026-10-18 | Importación de `movies.json` al catálogo real | Adolfo eligió reemplazar el índice de `elastiflix-reloaded`. Arranque 05:44:29 UTC y publicación 05:49:22 UTC (unos 4 min 54 s). Arreglo JSON de 6899 registros, validación `valido`, 0 errores y 5055 advertencias (`EFX-ING-022`: 4960, `EFX-ING-021`: 95). Índice nuevo `elastiflix-reloaded-20261018-054429` con 6899 documentos. Se borró `elastiflix-reloaded-20261018-003535`. Una lectura posterior contó `title`, `plot`, `plot_256`, `plot_512` y `plot_1024` en 6899/6899. |
| 2026-10-18 | Revisión 1 fusionada en `main` | Adolfo fusionó el PR #10 (merge `dc9a315`). |
| 2026-10-18 | Revisión 2 fusionada en `main` | Adolfo pidió fusionar el PR #13 («Fusiona el pr»); merge `7f08810`. Después pidió borrar las ramas que ya no se necesitan: se borró `revision-2`. |
| 2026-10-18 | Decisión D1 de la revisión 1 (R-05) | Las pruebas de §6 vuelven a afirmar los valores exactos sobre 6 899 películas y se omiten con `assumeTrue` si `_count` cambió. La tomó el agente en la revisión 1 (`progreso/revision-1.md`, «Decisiones»); Adolfo la aceptó al fusionar el PR #10 (`dc9a315`). No hay otra aprobación registrada. |
| 2026-10-18 | Decisión D2 de la revisión 1 (R-12) | La ingesta trabaja en memoria (hasta 150 MB) en lugar de releer el archivo en streaming: los vectores se quitan al leer y las películas se sueltan al terminar. Mismo origen que D1; aceptada al fusionar el PR #10 (`dc9a315`). La revisión 2 (S-06) completa sus mitigaciones. |
| 2026-10-18 | Decisión D3 de la revisión 1 (R-30) | «Acerca de» rotula la tabla de precisión y los datos de `adult` y `release_date` como medidos en el catálogo original de 4 322 películas, sin recalcular. Mismo origen que D1; aceptada al fusionar el PR #10 (`dc9a315`). |
| 2026-10-18 | Cambio del mockup aprobado en B-05 | `design/mockups/About.dc.html` cambió «`Document: ` en las sinopsis» por «`input_type` `ingest` y sin prefijo». Lo hizo el agente en B-05 (`8cbfa49`); Adolfo lo aceptó al fusionar el PR #12 (`41c1188`). Registrado en PLAN §1 por S-16. |
| 2026-10-18 | Sincronización de documentación fusionada en `main` | Adolfo fusionó el PR #11 (merge `2f6f9ed`). |
| 2026-10-18 | Bugs B-01 a B-05 fusionados en `main` | Adolfo fusionó el PR #12 (merge `41c1188`). |
| 2026-10-18 | Revisión 2 encargada a otro agente | Adolfo pidió pasar la lista de correcciones al repositorio (`docs/revisiones/revision-2.md`) para que la tome un agente de Claude Code, Google Antigravity o JetBrains Junie. Ya no se le encarga al agente de grokbot (Cursor). Las decisiones abiertas de S-03, S-05 y S-06 las toma el agente y las registra en `progreso/revision-2.md`. |
| 2026-10-18 | Estado posterior a la Revisión 2 fusionado en `main` | Adolfo pidió fusionar el PR #14 (merge `27fcdef`, commit `ef53328`) y borrar las ramas que ya no se necesitan. Se borró `estado-revision-2`; en el remoto solo queda `main`. |
| 2026-10-19 | Revisión 3: solo documentar el código, directo a `main` | Adolfo pidió una revisión exhaustiva del código y de la documentación. Eligió que los problemas de código **solo se documenten** (`docs/revisiones/revision-3.md`, T-01 a T-27) y que la documentación alineada se suba **directo a `main`**. La revisión no pudo compilar ni correr pruebas en su entorno. |
| 2026-10-19 | Revisión 3 en carriles paralelos | Adolfo pidió que la revisión 3 se planifique para varios agentes en paralelo, sin dependencias que obliguen a detenerse. Quedó en seis carriles con archivos disjuntos (A–F) y un carril de cierre (Z); reglas en `docs/revisiones/revision-3.md` › «Cómo trabajar en paralelo». |
| 2026-10-19 | Mockup de la página de detalle y requerimientos nuevos | Adolfo aprobó `design/mockups/Detail.dc.html` (paneles a la izquierda, póster más grande, «N/A» en campos vacíos, USD y regreso a los resultados con la búsqueda intacta) y el enlace del título en `Results.dc.html`. Pidió documentar la ruta `GET /pelicula/{id}` y cuatro requerimientos nuevos (por compartir) en PLAN › «Requerimientos nuevos». Ninguno se implementa hasta que termine la revisión 3 y él dé su consentimiento. |
| 2026-10-19 | Decisiones sobre el requerimiento 1 (detalle de película) | Adolfo confirmó que la página de detalle es el primero de sus cuatro requerimientos nuevos; que un `budget` vacío o en 0 se muestra como «N/A»; y que llegar a la ficha con un enlace directo, sin una búsqueda, muestra un error controlado que dice que su consulta falló. Después decidió que `revenue` sigue la misma regla: vacío o en 0, «N/A». |
| 2026-10-19 | Requerimiento 2: interfaz en español e inglés | Adolfo pidió una versión en inglés de cada página, con un botón por idioma en el header. Eligió: idioma en el parámetro `lang` recordado en una cookie; español al inicio; traducir todo lo que escribe la aplicación (no los datos del catálogo); y aprobar la versión en inglés con mockups. Los cinco mockups `-en` y el selector «ES · EN» están en el canvas y en `design/mockups/`, **pendientes de su aprobación**. Glosario en `docs/i18n/glosario-en.md`. |
| 2026-10-19 | Mockups del requerimiento 2 aprobados | Adolfo aprobó los mockups en inglés y el selector «ES · EN» («Apruebo los mockups»), con tres ajustes ya aplicados: traducir y adaptar al inglés las consultas de ejemplo (las de título original se quedan; posiciones y conteos se verifican al implementar), el conteo del catálogo siempre del número real de documentos del índice, y los créditos de TMDB en los dos idiomas en cada «Acerca de». |
| 2026-10-19 | Requerimiento 3: archivos de hasta 25 MB | Adolfo pidió que el tamaño máximo de los archivos `.json` y `.csv` sea 25 MB, con todas las demás validaciones. Decidió que sea fijo, sin configuración (se quita `ELASTIFLIX_INGEST_MAX_FILE_SIZE`). Cómo cargar un catálogo más grande lo aclara el requerimiento 4. Mockups de «Cargar datos» y «Acerca de» actualizados a 25 MB. |
| 2026-10-19 | Requerimiento 4: carga en modo append o bulk | Adolfo pidió dos modos para cargar archivos: bulk (índice temporal, alias y borrado del original) y append (sin índice temporal, directo al índice actual). Decidió: bulk carga solo el archivo (reemplaza el catálogo, sin copiarlo); en append un `id` existente se reemplaza; si append falla o se cancela, lo escrito se queda y se informa; append es el modo por defecto. Un catálogo de más de 25 MB se carga con un bulk y varios append. Mockups de «Cargar datos» y «Acerca de» actualizados. |
| 2026-10-19 | Mockups del requerimiento 4 aprobados | Adolfo aceptó los mockups de «Cargar datos» y «Acerca de» en los dos idiomas («Si. Acepto los mockups»), con el selector de modo y los textos restaurados de las fases 4.4, 4.5 y B-05. Los cuatro requerimientos nuevos quedan definidos en PLAN; se implementan cuando termine la revisión 3 y Adolfo dé su consentimiento. |
| 2026-10-19 | Revisión 3: carriles A–F fusionados | Adolfo pidió «Procede con la revisión 3» y después «Fusiona los PR del 15 al 20». Los seis PR quedaron en `main` (`7e360b4`). |
| 2026-10-19 | Revisión 3: cierre fusionado y ramas borradas | Adolfo pidió «Fusiona el PR 21 y luego borra las ramas que ya no se necesitan»: merge `5c21934`; se borraron las siete ramas `revision-3-*` después de comprobar que estaban en `main`. |
| 2026-10-19 | Revisión 3: dos puntos fuera de la lista en el cierre | Adolfo pidió incluir en el cierre («Inclúyelas en el cierre») el agrupado de miles del rango de paginación y quitar el `preference` sin uso de la híbrida. |
| 2026-10-19 | Inicio del Requerimiento 1 y diseño de la Fase 7 | Adolfo pidió iniciar con el Requerimiento 1 («Si. Inicia con el Requerimiento 1.») y aprobó el diseño `docs/diseno/7.md` tal cual («Apruebo el diseño de docs/diseno/7.md tal cual para proceder con la implementación de las tareas 7.1 a 7.6»). Rama `fase-7-detalle`. |
| 2026-10-19 | Cierre y fusión de la Fase 7 (Requerimiento 1: Detalle de película) | Adolfo pidió preparar el PR y fusionar la Fase 7 («Prepara el PR y fusiona la Fase 7»). PR #23 fusionado en `main` (merge commit `2902be7`). Rama `fase-7-detalle` borrada local y remotamente. Verificación `./mvnw clean verify` en verde (80 unitarias web / 329 repo, 0 Checkstyle, 0 PMD, ArchUnit 7/7). |

## Catálogo vigente

- **Alias:** `elastiflix-reloaded`
- **Índice:** `elastiflix-reloaded-20261018-054429`
- **Documentos:** 6899, con `title`, `plot` y los tres vectores en todos
- **Índice anterior:** `elastiflix-reloaded-20261018-003535` (4322 documentos), borrado al publicar
- **Pruebas:** `SmokeIT`, `CatalogStatusServiceIT`, `FullTextSearchIT`, `VectorSearchIT` y `HybridSearchIT` afirman la tabla de §6 (6 899 películas) y se omiten si `_count` cambió. No escriben en este alias.

## Última verificación

- **Comando:** `./mvnw verify -Pit` (con `.env`)
- **Resultado:** BUILD SUCCESS en 1 min 39 s: 264 unitarias (core 50, search 62, ingest 97, web 55) y 29 de
  integración (core 9, search 14, ingest 2, web 4), 0 fallos, 0 omitidas; Checkstyle y PMD en 0. Después, el
  alias `elastiflix-reloaded` seguía en `elastiflix-reloaded-20261018-054429` con 6 899 documentos.
- **Fecha:** 2026-10-18 22:51–22:53 UTC (revisión 2, con el árbol de `630d90a`).
- **Pendiente:** `763f71e` (S-18, `CsvColumnRows`, `CsvValues` y su prueba) es posterior a esa corrida y no
  tiene un `verify` completo registrado. La revisión 3 no pudo compilar. El primer paso de la revisión 3
  vuelve a correr `./mvnw verify` y `-Pit` en `main`.
- **Corrida anterior:** 2026-10-18 19:05–19:07 UTC, bugs B-01 a B-05: 193 unitarias y 27 de integración,
  0 fallos.
