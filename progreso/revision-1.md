# Estado de trabajo — Revisión 1

> Correcciones de la revisión de `main` en `dc44535`. Una tarea por cada `R-NN`.

## Identificación

- **Unidad:** Revisión 1
- **Nombre:** Correcciones de la revisión exhaustiva
- **Rama:** `cursor/revision-1-29a6`
- **Carpeta:** `/workspace`
- **Herramienta de la última sesión:** Cursor

## Estado general

en curso

## Punto de reanudación

- **Tarea actual:** R-30
- **Subpaso en curso:** commit y push
- **Próximo paso:** Regenerar las cinco capturas, correr `./mvnw verify` y `./mvnw verify -Pit`, y confirmar el catálogo real.
- **Última actualización:** 2026-10-18 18:23 UTC

## Tareas

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| R-01 | terminada | `2f7df05` | `./mvnw -pl elastiflix-search,elastiflix-core test` código 0. Checkstyle y PMD de esos módulos en 0. `k=10, tamano=25, pagina=1` devuelve 10; `k=150, tamano=100, pagina=2` devuelve 50; full-text página 400 aceptada y el conteo de páginas se corta en 10 000. |
| R-02 | terminada | `125d049` | `ImportJobTest` 11 pruebas, 0 fallos. Alias en el temporal: sin `deleteIndex(temp)` y paso 5. Alias en el anterior: `EFX-ES-004` y se borra el temporal. `describe` falla o el alias no existe: `EFX-IMP-006` y no se borra nada. Checkstyle y PMD de core e ingest en 0. |
| R-03 | terminada | `f24bb4d` | Variantes CSV: `_id` ≠ `id` (`EFX-ING-010`), fecha `16/07/1999`, `adult=yes` y `genres=Drama` (`EFX-ING-012`), subcampo `extra` en keywords (`EFX-ING-007`). `[REC]` y `007` se conservan. `movies-sample.csv` con 0 errores. Checkstyle y PMD de ingest en 0. |
| R-04 | terminada | `90b96ce` | MockMvc ve las tarjetas, la tabla «Se recibió / El mapping espera», «Archivo JSON», «Importación bloqueada» y el paso 4. `SmokeIT` espera el paso 4. Captura `docs/capturas/cargar.png`. Checkstyle y PMD en 0. |
| R-05 | terminada | `46aeda4` | Tabla de §6 medida sobre 6 899: dark knight 57/5, love story 107/3, Kolejne 3/0, el secreto 35/2, le fabuleux 26/1, heaven 10/0, next 15/0, parasite 1/0. Titanic 36/7/4, Life of Pi 30/7/4, WALL·E 1/1/1. `assumeTrue(count == 6899)`. FullText 8, Vector 4, Hybrid 2, CatalogStatus 1 y Smoke 4, 0 omitidas. |
| R-06 | terminada | `2793d22` | MockMvc: títulos Puntuación, Similitud y Puntuación en cada lista; «Mostrando 1–1 de 1 resultados»; chips «Géneros: Drama»; `<select id="page-size">`; fecha invertida con el texto del mockup y el formulario visible. Captura `docs/capturas/resultados.png`. Checkstyle y PMD en 0. |
| R-07 | terminada | `3614553` | `IngestJobErrorTest`: archivo de 64 B con tope 32 B responde 413 y `EFX-ING-003`. Trabajo desconocido responde 404 JSON `EFX-IMP-005`. Checkstyle y PMD en 0. |
| R-08 | terminada | `bf58431` | `CatalogHealthIndicatorTest`: desconectado queda `DOWN`, `errorCode=EFX-ES-001` y ningún detalle nulo. Checkstyle y PMD en 0. |
| R-09 | terminada | `c4f0d23` | `ElastiflixPropertiesTest`: consulta `search` / `Query: `; documento `ingest` / prefijo vacío. `embedQuery` lee esos campos. Checkstyle y PMD en 0. |
| R-10 | terminada | `f62b84b` | `about.html` y `PLAN.md` §2 y las tareas 1.2 y 1.9 dicen `ingest` y sin prefijo. La matriz de calibración sigue listando `Document: ` como combinación medida, no como la elegida. |
| R-11 | terminada | `7dd6329` | `JobThreadFailureTest`: una excepción imprevista deja el trabajo en `fallido` con `EFX-IMP-007`. `SearchQueryMapperTest`: página `999999999999` queda en 1; un `k` enorme es `SearchValidationException`. Checkstyle y PMD en 0. |
| R-12 | terminada | `f3f144a` | Tras el aviso `EFX-ING-020` los vectores salen del nodo. `plot_256/512/1024` quedan nulos. Al cerrar el trabajo, `movies()` queda vacío. Checkstyle y PMD en 0. |
| R-13 | terminada | `11ce6d8` | `CsvMovieReaderTest`: fila 2 con 2 celdas y fila 3 con 4, frente a un encabezado de 3, dan `EFX-ING-014`. El fixture de 4 películas sigue con 0 errores. Checkstyle y PMD en 0. |
| R-14 | terminada | `6e93147` | Se eliminó `JsonMovieReader`. `JsonDocumentsTest` cubre exportación, arreglo, NDJSON, conteo y JSON roto. Checkstyle y PMD en 0. |
| R-15 | terminada | `83e7cea` | `ReindexTasksTest`: `wait_for_completion` queda en falso y la copia espera dos lecturas de `_tasks` hasta `completed`. Si la tarea falla, `EFX-ES-006`. Checkstyle y PMD en 0. |
| R-16 | terminada | `95f9996` | `home.js` reescribe `k`, `candidatos` y `vector` de cada sugerencia con los valores del formulario. |
| R-17 | terminada | `4218be1` | El `img` del póster trae `onerror` y `results.js` también sustituye una imagen que ya falló antes de enganchar el evento. |
| R-18 | terminada | `8ab53f8` | `SmokeIT` lee el alias de `ElastiflixProperties`, no de un literal. Compila con `test-compile`. Checkstyle y PMD en 0. |
| R-19 | terminada | `6794f28` | README: se quitó «Maven ≥ 3.6.3 solo para generar el wrapper»; el alias y el id de inferencia salen de `application.yml` o de `.env`. Nueva subsección «Datos y proyecto Serverless» (exportación no versionada, índice concreto de la tarea 1.8, películas `597` `27205` `10681` `345887` `134` `87827`, endpoint `.jina-embeddings-v5-omni-small` y privilegios de la API key, cada uno con comando o enlace). |
| R-20 | terminada | `5a5ffc6` | Hashes verificados con `git log` en las 24 filas. La 2.3 queda `7a878d6` y `8bf3c1a`. Aprobaciones incluye el PR #9 (`dc44535`). `progreso/6.md` anota `2a3aaea`. La plantilla dice que el hash se completa en la tarea siguiente. |
| R-21 | terminada | `ab50458` | Cinco capturas en `docs/capturas/` (inicio, resultados, cargar, acerca, no-encontrada), copiadas también a `attachments/` del store. Comparación en `progreso/4.md`. La fila de aprobación queda sin marcar. |
| R-22 | terminada | `6f44800` | `CatalogBootstrapRunnerTest`: con `servlet` responde `EFX-CFG-004` y no cierra el cliente; con `none` migra y lo cierra. El log de una migración exitosa reutiliza el conteo ya verificado. Si ese conteo falla en un alias que ya existe, la migración sigue. Checkstyle y PMD en 0. |
| R-23 | terminada | `0c28269` | `InferenceEmbeddingServiceTest`: un lote con menos vectores que textos lanza `EFX-EMB-002`. Checkstyle y PMD en 0. |
| R-24 | terminada | `8706b83` | Un 4xx es `EFX-ES-007` y un corte de red sigue en `EFX-ES-001`. El log de `_msearch` usa el alias de la cabecera. El bulk registra id y tipo. El CSV ilegible es `EFX-ING-015`. La columna recibido de un rechazo de archivo ya no repite el código. Checkstyle y PMD en 0. |
| R-25 | terminada | `aa3bcb6` | El estado vacío reutiliza `queryVector` y no vuelve a llamar a la inferencia. Las listas de apoyo de la híbrida piden `_source: false` y se leen por `hit.id()`. `HybridRequestBuilderTest` 3, `SearchServiceTest` 13, 0 fallos. Checkstyle y PMD en 0. |
| R-26 | terminada | `2bae8ac` | `ElastiflixPropertiesTest`: `toString` de la conexión y del registro padre contiene `apiKey=****` y no la clave `test-key`. Checkstyle y PMD en 0. |
| R-27 | terminada | `96b9d55` | `fields()` conserva el orden del archivo (`Collections.unmodifiableMap` sobre el `LinkedHashMap`). El Javadoc de `Movie` dice que los vectores participan en `equals` por referencia. Se quitaron las sobrecargas de `build` que nadie llamaba. `IndexMappingTest` 2, `HybridRequestBuilderTest` 3, `VectorRequestBuilderTest` 3 y `SearchServiceTest` 13, 0 fallos. Checkstyle y PMD en 0. |
| R-28 | terminada | `19f0557` | El reemplazo se cuenta con `existingIds` sobre la copia, no con el número de la validación. `IdTexts` es el mismo texto para la búsqueda y el validador. Cada trabajo recibe su listener. Vectorizados y escritos salen juntos, antes de `writeAll`. Un `\r\n` entre comillas queda en `\n`. Lo que sigue al primer JSON es `EFX-ING-013`. Las advertencias del estado se agrupan por código y campo. El CSV de errores lleva BOM y apóstrofo delante de `=`, `+`, `-` y `@`. `IngestIT` cancela y espera un estado final antes de borrar. `ImportJobTest` 12, `MappingValidatorTest` 14, `JsonDocumentsTest` 5, `CsvRowsTest` 2, `JobThreadFailureTest` 4, 0 fallos. Checkstyle y PMD en 0. |
| R-29 | terminada | `4e4b865` | No aplica: las pestañas de `ingest.html` ya tienen `role="tab"`, `aria-controls` y `tabpanel`; `ingest.js` actualiza `aria-selected` y `aria-labelledby`. El paso 5 ya escribe `${shell.alias}`. Hecho: `aria-current="page"`; `role="alert"` en los avisos de Inicio; `aria-invalid` y `aria-describedby` en `q`, `knn-k` y `knn-cand`; «Ver todos» solo si hay más de 6, con `aria-expanded`. Seis tonos pastel, iconos de modo y de ajuste, lila `#F3EFFD`. La página de error y el JSON de una subida rechazada ya no repiten el código; el 404 ya no enlaza `/buscar` sin consulta; `+` queda como `%2B`; WARN al rechazar un archivo y al fallar la validación de búsqueda. «Publicación» usa el alias configurado. CSRF anotado en AGENTS (10 400 caracteres) y en el README. 16 pruebas, 0 fallos. Checkstyle y PMD en 0. |
| R-30 | terminada | se anota en el cierre | «Acerca de» rotula la precisión, `adult` y `release_date` como medidos en el catálogo original de 4 322, sin recalcular. El pie de §5 y el criterio 4.1 dicen que el conteo es el real. Portada de README y de AGENTS apuntan a `progreso/INDICE.md` › «Catálogo vigente»; AGENTS queda en 10 428 caracteres. El bloque de variables de `PLAN.md` es copia exacta de `.env.example`. `docs/diseno/3.md` anota que `MovieBulk` no existe. Las convenciones citan `MovieReaderFactory` y `SearchStrategy`. `progreso/0.md` ya no trae la ruta local del JDK. `grep -rn "4 322\|4322"` en README, AGENTS, PLAN y las plantillas solo deja menciones históricas. `AboutPageTest` en verde. |

## Cambios sin commit

Vacío al cerrar R-30.

## Última verificación

- **Comando:** `./mvnw -pl elastiflix-web -am test -Dtest=AboutPageTest -Dsurefire.failIfNoSpecifiedTests=false` y `./mvnw -pl elastiflix-web checkstyle:check`
- **Resultado:** BUILD SUCCESS. `AboutPageTest` 1 prueba, 0 fallos. Checkstyle de web en 0. `AGENTS.md` 10 428 caracteres. El bloque dotenv de `PLAN.md` coincide con `.env.example`.
- **Fecha:** 2026-10-18 18:22 UTC

## Decisiones

- Sesión iniciada 2026-10-18 16:23 UTC con Cursor, desde `main` en `dc44535`.
- D1: restaurar valores exactos de §6 con `Assumptions.assumeTrue` (R-05).
- D2: se acepta la ingesta en memoria, quitando vectores al leer y liberando al terminar (R-12).
- D3: «Acerca de» se rotula como medido en el catálogo de 4 322, sin recalcular (R-30).
- Sesión reanudada 2026-10-18 18:13 UTC con Cursor: árbol limpio en `19f0557`, siguiente paso R-29.
- R-29: el enlace «Resultados» del 404 se quita. `/buscar` sin `q` es `EFX-SRCH-001` y no se inventa una consulta.

## Resumen de diseño

Pendiente al cerrar la revisión.

## Revisión de convenciones

Pendiente al cerrar la revisión.

## Bloqueos y solicitudes

Ninguno.
