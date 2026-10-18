# Estado de trabajo — Revisión 2

> Correcciones de la segunda revisión de `main` en `41c1188`. Una tarea por cada `S-NN`. La especificación
> de cada una (dónde, problema, corrección y criterio) está en
> [`docs/revisiones/revision-2.md`](../docs/revisiones/revision-2.md); léela antes de empezar cada tarea.

## Identificación

- **Unidad:** Revisión 2
- **Nombre:** Correcciones de la segunda revisión exhaustiva
- **Rama:** `revision-2` (creada desde `main` en `3d0b8ff`)
- **Carpeta:** raíz del repositorio
- **Herramienta de la última sesión:** Claude Code (Opus 5.5)

## Estado general

en curso

## Punto de reanudación

- **Tarea actual:** S-08
- **Subpaso en curso:** ninguno
- **Próximo paso:** S-08: que `CsvMovieReader` entregue la fila real de cada documento y `MappingValidator` la
  use; validar también las filas de ancho correcto cuando hay errores de ancho y unir los informes.
- **Última actualización:** 2026-10-18 23:00 UTC

## Tareas

> El hash de una tarea se completa en el commit de la tarea siguiente. La fila que se cierra puede decir «se anota en …» hasta ese commit.
> Al cerrar una tarea, sustituye su título por la evidencia: comando, resultado y la prueba que falla si se rompe el comportamiento.

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| S-01 | terminada | `fc984de` | `ReindexTasks` espera en un bucle de lecturas `_tasks` de 20 s con plazo total de 30 min (`Clock`), revisa `error`/`failures`/`timed_out`, falla con `EFX-ES-006` sin id de tarea, y cancela la tarea (`_tasks/<id>/_cancel`) antes de lanzar si vence el plazo, se corta la conexión o la persona cancela. `CatalogIndex.reindex` recibe `stop`. Confirmado en el jar: `spring.elasticsearch.socket-timeout` vale 30 s. `./mvnw verify`: BUILD SUCCESS, 200 unitarias, 0 fallos, Checkstyle y PMD en 0. `ReindexTasksTest` (10) cubre los seis casos del criterio más la lectura de 20 s, `completed=false`, el error de la tarea y la cancelación de la persona. Mutaciones, una a la vez y revertidas: sin cancelar al vencer el plazo → falla `shouldCancelWhenTheDeadlinePasses`; sin cancelar ante `IOException` → `shouldCancelWhenTheConnectionFails`; `timeout_exception` como error → `shouldKeepWaitingAfterATimeoutException`; sin `failures` → `shouldFailWhenTheTaskReportsFailures`; sin `timed_out` → `shouldFailWhenTheTaskTimedOut`; id vacío como copia terminada → `shouldFailWhenThereIsNoTaskId`; `ImportJob` sin pasar la señal de cancelación → `ImportJobTest.shouldCancelTheCopyBeforeDeletingTheTemp`. `CatalogIndexIT` e `IngestIT` no se corrieron: no hay `.env` (ver «Bloqueos»). |
| S-02 | terminada | `09fe15e` | `ingest.html` y el bloque `.ingest` de `elastiflix.css` siguen las medidas de `Ingest.dc.html`: contenedor `max-width:1040px`, `margin:0 auto`, `padding:32px 20px 72px`; etiqueta superior con icono y sin margen extra; pestañas con su pastel (`#FFCDB2`/`#FBE7A1`/`#BDE8D7`), `border-radius:10px` e icono; campos y zona de arrastre en `#FBF7F1`; botones «Limpiar», «Guardar película», «Validar archivo», «Importar…» y «Cancelar importación» con estilo, iconos y estado deshabilitado `#EFE7DC`/`#6B6578`; pasos con título, estado en la tinta de su color (sin cursiva) y detalle en otra línea; alerta `#job-stopped` (`#FDEEF5`, `h3` Bricolage 21 px con icono); bloque «No se puede cargar» con `h2` 22 px e icono; tablas con `th` mono en mayúsculas; comprobaciones con icono verde; advertencias en caja `#FFF9E3` con icono; «Importación» en su propia tarjeta. También: el chip «Solo .json» pasa a «Solo .csv» en la pestaña CSV. Evidencia: capturas de página completa con Playwright (Chromium 131) sobre la app local, con las respuestas del trabajo simuladas (sin `.env`): registro individual, CSV válido (paso 4 «listo para empezar») y CSV inválido. `./mvnw verify`: BUILD SUCCESS. Las capturas finales con datos reales y la aprobación de Adolfo quedan en S-18; los textos y datos del trabajo, en S-03. |
| S-03 | terminada | `394d171` | `JobView` trae `fileCount` también en `invalido`, `validCount` = leídos menos registros distintos con error, `invalidRecords`, `tempIndex` (por `ImportProgress`), `batchSize` configurado, `kind`, `fileSize` y la tabla de columnas del CSV (`CsvColumnRows`: «Asignada», «Se ignora», «Sin datos», «No existe en el mapping»); `replaceCount` ya no cambia con el avance; `checks` sin la comprobación de vectores; `failureMessage` = `summary()` (sin código duplicado). Una columna desconocida ya no corta la lectura del CSV. Los textos los arma `JobTexts` en la web (`GET /cargar/trabajos/{id}` responde `{job, texts}`) con espacio de miles; `ingest.js` reescrito: paso 1 al elegir archivo, ficha con «Cambiar archivo», bloque de rechazo con su texto y motivos después de «Proceso de carga», `cancelado` deja el paso «detenido», una sola cadena de consulta, botones deshabilitados al hacer clic, 404/409 y fallos de red en una alerta propia, «Cancelar importación» solo en `importando`, `aria-valuemax` ≥ 1. Tamaño máximo y lote configurados en la plantilla (`IngestLimits`). `encabezado=false` se rechaza en el navegador y en la subida (400 `EFX-ING-005`). Textos fuera del mockup registrados en PLAN §1 (ver «Decisiones»). `./mvnw verify`: BUILD SUCCESS, 211 unitarias (core 45, search 58, ingest 70, web 38), 0 fallos, Checkstyle y PMD en 0. Pruebas: `JobFiguresTest` (512 leídos, 23 errores en 17 registros → 495 válidos), `CsvColumnRowsTest`, `IngestPageTest` (textos exactos de los escenarios «Archivo válido», «No coincide con el mapping» y «Archivo rechazado»; `encabezado=false`; `importar` pasa de `importando` a `publicado`). Mutaciones, una a la vez y revertidas: válidos mal contados → `JobFiguresTest.shouldCountValidRecordsWhenSomeHaveErrors`; `fileCount` solo en `ready` → la misma; el avance pisa `replaceCount` → `JobThreadFailureTest.shouldKeepProgressOnTheJobThatMoved`; vuelve la comprobación de vectores → `shouldLeaveOutTheVectorCheck`; lote fijo → `shouldUseTheConfiguredBatchSize`; mensaje con código → `shouldGiveTheSummaryWithoutTheCode`; sin rechazo de encabezado → `IngestPageTest.shouldRejectACsvWithoutHeader`; Registro/Fila al revés → `shouldGiveTheMismatchTexts`; cifras sin miles → `shouldGiveTheValidFileTexts`. JS comprobado con Playwright sobre la app local (respuestas simuladas): rechazo UTF-8, CSV sin encabezado, válido (Cancelar deshabilitado), importando (Importar deshabilitado al clic, `aria-valuemax=4322`) e inválido. |
| S-04 | terminada | `5ebd743` | `ImportJob.importInto` deja de borrar el temporal justo antes de `_aliases` y borra en un `finally` solo si no se publicó (o ante `EFX-ES-004`); el aviso del paso 5 y el log salen del `try`. `resolvePublish` y `CatalogBootstrap.confirmOrReject` borran solo si la causa es un `ElasticsearchException` 4xx (`ElasticsearchDiagnostics.isClientRejection`) y `describe` confirma el estado anterior; un corte o 5xx dan `EFX-IMP-006`. `publish` pide `master_timeout`/`timeout` 10 s; `indexOne` pide `require_alias=true`; una `JsonpMappingException` del cliente pasa por `describe` como un corte (Checkstyle prohíbe atrapar `RuntimeException`; cualquier otra excepción durante el swap deja el temporal, igual que `EFX-IMP-006`). `./mvnw verify`: BUILD SUCCESS, 215 unitarias (corregido en S-05; el commit decía 216), 0 fallos, Checkstyle y PMD en 0. Pruebas nuevas: `ImportJobTest.shouldKeepThePublishedTempWhenTheListenerFails` (listener que lanza en el paso 5 → sin `deleteIndex(temp)`), `shouldKeepTheTempWhenPublishIsCutOff` (`IOException` + alias en el anterior → `EFX-IMP-006` sin borrar), `shouldDeleteTheTempWhenTheAliasStaysOnThePreviousIndex` (4xx + anterior → borra), `shouldDeleteTheTempOnAnUnexpectedFailure`; `CatalogBootstrapTest.shouldKeepTheNewIndexWhenPublishIsCutOff` y `shouldDeleteTheNewIndexWhenPublishFails` (4xx). Mutaciones, una a la vez y revertidas: `ImportJob` borra ante cualquier falla → `shouldKeepTheTempWhenPublishIsCutOff`; sin bandera (aviso del paso 5 dentro del `try`) → `shouldKeepThePublishedTempWhenTheListenerFails` y 5 más; sin `finally` → `shouldDeleteTheTempOnAnUnexpectedFailure`; `CatalogBootstrap` borra ante cualquier falla → `shouldKeepTheNewIndexWhenPublishIsCutOff`. Que Serverless acepte `master_timeout` en `_aliases` y `require_alias` queda para `CatalogIndexIT`/`IngestIT` con `-Pit` (sin `.env`). |
| S-05 | terminada | `f7d6358` (código y diseño) y `c5d58b2` (estado) | `@DefaultValue Embedding embedding` en el record raíz; `Embedding` solo con `documentInputType`/`documentPrefix`, con el literal calibrado en `DEFAULT_DOCUMENT_INPUT_TYPE`/`DEFAULT_DOCUMENT_PREFIX` usado por `@DefaultValue` y por el constructor; se quitó `fallback()`. La consulta vuelve a constantes de `InferenceEmbeddingService` (`search`, `Query: `), como dice el Javadoc de `EmbeddingService.embedQuery`. `./mvnw verify`: BUILD SUCCESS, 217 unitarias (core 48, search 58, ingest 73, web 38), 0 fallos. `ElastiflixPropertiesTest.shouldTakeTheDocumentInputTypeFromTheDefaultValue` (binding real con `ApplicationContextRunner` y solo `elastiflix.embedding.document-prefix=`) y `shouldUseTheCalibratedPairWithoutEmbeddingProperties` (sin propiedades). Mutación: `@DefaultValue("search")` en `documentInputType` hace fallar las dos y `shouldUsePlanDefaultsWhenOptionalGroupsAreMissing`; revertida. |
| S-06 | terminada | `11f42a2` | `JsonDocuments.read` devuelve `JsonRead`: quita los vectores de cada registro al leerlo y deja una advertencia `EFX-ING-020` por campo (`VectorFields`); `CsvMovieReader` no convierte columnas de vector. `MappingValidator.validateAndRelease` pone en `null` cada nodo crudo al revisarlo y convierte la película al vuelo (con el primer error suelta las películas); se quitó `dropVectors`. `FileIntake` borra el archivo local al terminar de validar. `JobStore` anota la última actividad (`Clock`), conserva 20 trabajos finales (`JobRetention.KEEP_FINISHED`) y cancela un `valido` sin actividad en 30 min; nunca toca `importando`. `cancelledFlag()` pasó a `isCancelled()` (PMD `TooManyMethods`). `./mvnw verify`: BUILD SUCCESS, 223 unitarias (core 48, search 58, ingest 79, web 38), 0 fallos, Checkstyle y PMD en 0. Pruebas: `JsonDocumentsTest.shouldDropVectorsWhileReading`, `JobRetentionTest` (sin películas tras estado final y sin archivo local; con 22 terminados se olvidan los 2 más viejos; `importando` no se expulsa; `valido` vence), `MappingValidatorTest.shouldReleaseRawNodesWhileValidating`. Mutaciones, una a la vez y revertidas: el lector no quita vectores → `shouldDropVectorsWhileReading`; sin tope → `shouldForgetTheOldestFinishedJobs`; el tope cuenta `importando` → `shouldNeverEvictAnImportingJob`; `valido` no vence → `shouldExpireAnIdleValidJob`; el validador no suelta nodos → `shouldReleaseRawNodesWhileValidating`; estado final sin soltar películas → `shouldReleaseMoviesAfterAFinalState`. La documentación de D2 se corrige en S-15. |
| S-07 | terminada | se anota en S-08 | `ScalarValues.fitsInt` = integral y `canConvertToInt`; `CsvValues` lee enteros con `BigInteger` y no recorta; celdas en blanco por `isBlank`; `IdTexts` y el `id` de `keyword` usan `bigIntegerValue`; `NestedValues.writeId` exige `canConvertToLong`; `decimal` exige `float` finito. `MovieForms.read` → `MovieForm.Read`/`Invalid`, captura `InvalidFormatException` y el controlador vuelve a `/cargar` con el rechazo `EFX-ING-012` y el campo. `./mvnw verify`: BUILD SUCCESS, 235 unitarias (core 48, search 58, ingest 90, web 39), 0 fallos, Checkstyle y PMD en 0. Pruebas: `NumberRangeTest` (celda CSV de 25 dígitos, `1e3`, `12.0`, `runtime` de 11 dígitos, `float` `1e39` en CSV; número JSON de 25 dígitos, `1e39` y `1e400`; id de 25 dígitos como texto; id de keyword enorme; sin `trim`) e `IngestPageTest.shouldRejectAnOutOfRangeFormNumber` (MockMvc, `vote_count=1e3` y `runtime=12345678901` → 302 con `EFX-ING-012`, sin llamar a `saveOne`). Mutaciones, una a la vez y revertidas: entero sin rango → `shouldRejectOutOfRangeCsvCells` y `...JsonNumbers`; CSV con `BigDecimal` exacto → `shouldRejectOutOfRangeCsvCells`; `float` sin rango → las dos; id con `longValue` → `shouldKeepALongNumericIdAsText`; CSV con `trim` → `shouldNotTrimCsvTexts`; keyword sin rango → `shouldRejectAHugeKeywordId`; formulario sin captura → `shouldRejectAnOutOfRangeFormNumber`. |
| S-08 | pendiente | — | CSV: el número de fila de los errores no corresponde a la fila del archivo |
| S-09 | pendiente | — | La comprobación UTF-8 del navegador rechaza archivos válidos |
| S-10 | pendiente | — | Las sugerencias de Inicio siguen perdiendo los ajustes |
| S-11 | pendiente | — | Resultados: un filtro marcado desaparece si no está en las facetas |
| S-12 | pendiente | — | «Acerca de»: las tarjetas de modo salen apiladas |
| S-13 | pendiente | — | Resultados: estilos del mockup pendientes |
| S-14 | pendiente | — | Las pruebas que pedía R-18 no se hicieron |
| S-15 | pendiente | — | El registro de entrega no refleja las decisiones de la revisión 1 |
| S-16 | pendiente | — | Tabla de ajustes autorizados de PLAN §1: faltan R-10, R-30 y el cambio del mockup de B-05 |
| S-17 | pendiente | — | Seis documentos dicen que las pruebas solo exigen «conteo mayor que cero» |
| S-18 | pendiente | — | Las capturas y la comparación de R-21 no reflejan la interfaz real |
| S-19 | pendiente | — | Con solo el README todavía no se puede preparar el catálogo |
| S-20 | pendiente | — | Una API key inválida aparece como «corrige la petición» |
| S-21 | pendiente | — | Códigos y textos de error que no corresponden al contexto |
| S-22 | pendiente | — | Mensajes y página de error genérica |
| S-23 | pendiente | — | Errores JSON de la carga con alcance incompleto |
| S-24 | pendiente | — | Detalles de la ingesta |
| S-25 | pendiente | — | Código muerto |
| S-26 | pendiente | — | Documentación técnica desactualizada |
| S-27 | pendiente | — | Evidencia histórica que quedó sin nota |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw verify`
- **Resultado:** BUILD SUCCESS: 200 unitarias (core 45, search 58, ingest 64, web 33), 0 fallos; Checkstyle y
  PMD en 0. `./mvnw verify -Pit` no se pudo correr: en esta máquina no hay `.env`.
- **Fecha:** 2026-10-18 20:11 UTC

## Decisiones

- 2026-10-18: Adolfo pidió que esta lista la tome un agente de Claude Code, Google Antigravity o
  JetBrains Junie. Ya no se le encarga al agente de grokbot (Cursor) que hizo las fases, la revisión 1 y
  los bugs.
- 2026-10-18: las decisiones abiertas de S-03 (textos fuera del mockup), S-05 (consulta configurable o
  fija) y S-06 (cuántos trabajos conservar) las toma el agente. Elige la opción más simple que cumpla
  el criterio y la registra aquí.
- Sesión reanudada 2026-10-18 19:50 UTC con Claude Code (Opus 5.5): unidad `no iniciado`, rama `main` en
  `3d0b8ff` limpia y al día con `origin`. Se creó la rama `revision-2`. En una máquina **sin `.env`**,
  `./mvnw verify` fallaba en `main`: las siete pruebas `@WebMvcTest` de web no pasaban la URL y la API key
  ficticias que pide PLAN 0.6, y `ElasticsearchCredentialsEnvironmentPostProcessor` cortaba el contexto con
  `EFX-CFG-001` (22 errores). Con `.env` presente el fallo no se veía. Antes de S-01, el commit
  «revision-2: las pruebas web corren sin .env» agrega a cada `@WebMvcTest`
  `properties = {"elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test"}`.
  No cambia código de producción.

- 2026-10-18, S-03 (textos fuera del mockup, decisión del agente): se queda el botón «Validar archivo»,
  porque el separador y la casilla de encabezado se eligen antes de subir; se agrega a PLAN §1. Se
  quitan «· detectado»/«· detectada» (no hay detección), la nota CSV «Si se desmarca, el archivo se
  rechaza…» (la regla ya está en el rechazo con `EFX-ING-005`) y «Elige un archivo .json o .csv.» («Validar
  archivo» queda deshabilitado sin archivo). El aviso de guardado individual pasa a verde (`notice-ok`).
  Los mensajes de error del servidor y de red se agregan a PLAN §1. `encabezado=false` se rechaza en las
  dos capas: navegador y subida (400 JSON `EFX-ING-005`).

- 2026-10-18, S-05 (decisión del agente): la parte de consulta vuelve a constantes (`search` y
  `Query: `) en `InferenceEmbeddingService`. Es la opción más simple y la que pide PLAN §6; ninguna
  propiedad `elastiflix.embedding.query-*` estaba documentada en §7.
- 2026-10-18: el commit `f7d6358` de S-05 salió sin el estado de `progreso/` (falló el script que lo
  escribía). No se reescribió el commit publicado; el estado va en el commit siguiente.

- 2026-10-18, S-06 (decisión del agente): se conservan los últimos 20 trabajos en estado final y un
  trabajo `valido` vence a los 30 minutos sin actividad (ninguna consulta ni cambio). La expulsión corre al
  abrir un trabajo nuevo, así que no hace falta un hilo programado.

## Resumen de diseño

<Al cerrar la unidad: estructura final, decisiones relevantes y diferencias con los diseños `docs/diseno/<fase>.md` que se tocaron.>

## Revisión de convenciones

<Al cerrar la unidad, responde cada punto de la lista de revisión de `docs/convenciones-java.md`.>

## Bloqueos y solicitudes

- 2026-10-18: en esta máquina (Codespace) no hay `.env`. Sin él no corren `./mvnw verify -Pit` ni las
  pruebas de integración que tocan S-01 (`CatalogIndexIT`, `IngestIT`), y no se pueden hacer S-18
  (capturas con datos reales) ni S-19 (corrida en un proyecto Serverless sin el índice). Las tareas siguen
  con pruebas unitarias. Hace falta que Adolfo agregue el `.env` (o los secretos del Codespace) antes de
  cerrar la unidad.

## Herramientas de la sesión

- Para mirar las páginas: la app local con credenciales ficticias
  (`ELASTICSEARCH_URL=http://localhost:9200 ELASTICSEARCH_API_KEY=test SERVER_PORT=8089 java -jar elastiflix-web/target/elastiflix-web-0.1.0-SNAPSHOT.jar`)
  y Playwright 1.49.1 con Chromium 131, instalado fuera del repositorio. Las respuestas de
  `/cargar/archivo` y `/cargar/trabajos/<id>` se simulan con `page.route`.
