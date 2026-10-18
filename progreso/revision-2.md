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

- **Tarea actual:** S-18
- **Subpaso en curso:** ninguno
- **Próximo paso:** S-18 necesita el catálogo real (capturas con datos) y la aprobación de Adolfo, y S-19 un
  proyecto Serverless sin el índice: las dos esperan el `.env`. Mientras tanto se avanza con S-20 a S-27 y
  S-18/S-19 quedan para el final. Siguiente: S-20, 401/403 → `EFX-ES-001` y `codeFor` en la híbrida.
- **Última actualización:** 2026-10-19 02:00 UTC

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
| S-07 | terminada | `49af619` | `ScalarValues.fitsInt` = integral y `canConvertToInt`; `CsvValues` lee enteros con `BigInteger` y no recorta; celdas en blanco por `isBlank`; `IdTexts` y el `id` de `keyword` usan `bigIntegerValue`; `NestedValues.writeId` exige `canConvertToLong`; `decimal` exige `float` finito. `MovieForms.read` → `MovieForm.Read`/`Invalid`, captura `InvalidFormatException` y el controlador vuelve a `/cargar` con el rechazo `EFX-ING-012` y el campo. `./mvnw verify`: BUILD SUCCESS, 235 unitarias (core 48, search 58, ingest 90, web 39), 0 fallos, Checkstyle y PMD en 0. Pruebas: `NumberRangeTest` (celda CSV de 25 dígitos, `1e3`, `12.0`, `runtime` de 11 dígitos, `float` `1e39` en CSV; número JSON de 25 dígitos, `1e39` y `1e400`; id de 25 dígitos como texto; id de keyword enorme; sin `trim`) e `IngestPageTest.shouldRejectAnOutOfRangeFormNumber` (MockMvc, `vote_count=1e3` y `runtime=12345678901` → 302 con `EFX-ING-012`, sin llamar a `saveOne`). Mutaciones, una a la vez y revertidas: entero sin rango → `shouldRejectOutOfRangeCsvCells` y `...JsonNumbers`; CSV con `BigDecimal` exacto → `shouldRejectOutOfRangeCsvCells`; `float` sin rango → las dos; id con `longValue` → `shouldKeepALongNumericIdAsText`; CSV con `trim` → `shouldNotTrimCsvTexts`; keyword sin rango → `shouldRejectAHugeKeywordId`; formulario sin captura → `shouldRejectAnOutOfRangeFormNumber`. |
| S-08 | terminada | `d94a6cf` | `CsvMovieReader` arma la lista de filas reales (`CsvRead.labels`), `MovieReaderFactory` la pasa en `LoadedDocuments.labels` y `FileIntake` llama a `validateAndRelease(documentos, filas, ids)`; `DocumentChecker` recibe la etiqueta en vez del número. Las filas de ancho correcto se validan aunque otra tenga `EFX-ING-014` (desde S-03 la lectura y la factoría ya no vacían la lista). `./mvnw verify`: BUILD SUCCESS, 237 unitarias (core 48, search 58, ingest 92, web 39), 0 fallos, Checkstyle y PMD en 0. `CsvRowNumbersTest` (por `JobStore` → `FileIntake`): `csv-bad-date.csv` da «2 · release_date»; una fila corta y dos fechas malas, con una fila en blanco, dan 3 errores en las filas 2, 4 y 5. Mutaciones, una a la vez y revertidas: el validador numera desde 1 → fallan las dos; un error de ancho corta la validación → `shouldReportWidthAndFieldErrorsTogether`. |
| S-09 | terminada | `9f698e2` | `readUtf8` usa `decode(buffer, { stream: true })`. Prueba de navegador nueva `elastiflix-web/src/test/browser/utf8.test.js` (Playwright 1.49.1, `node --test`, contra la app local con credenciales ficticias): un JSON con 65 535 bytes `a` y una «é» (C3 A9) que empieza en el byte 65 535 deja el paso 1 «completado» y «Validar archivo» habilitado; un byte `0xFF` se sigue rechazando con «El contenido no es texto UTF-8.». Antes de la corrección, la primera prueba falla con «El contenido no es texto UTF-8.»; después, `BASE_URL=http://localhost:8089 npm test`: pass 2, fail 0. `./mvnw verify`: BUILD SUCCESS, 237 unitarias, 0 fallos. Cómo correr las pruebas de navegador: `elastiflix-web/src/test/browser/README.md`. |
| S-10 | terminada | `3ec7c5f` | `home.js`: cada `.chip` cancela la navegación, pone su `q` en la caja y hace `form.requestSubmit()` (pasa por la validación de consulta y de k-NN); se quitó `keepChosenSettings`. Prueba de navegador `suggestions.test.js`: Híbrida + `multi_match` + `plot_1024` + `k=50` → la URL de `/buscar` trae `modo=hibrida`, `k=50`, `vector=plot_1024`, `tipo=multi_match` y el `q` de la sugerencia; sin JavaScript el `href` del servidor sigue llevando a `/buscar`. Antes de la corrección la primera falla (`k` 100 en lugar de 50); después, `BASE_URL=http://localhost:8089 npm test`: pass 4, fail 0. `./mvnw verify`: BUILD SUCCESS, 237 unitarias, 0 fallos. |
| S-11 | terminada | `f650cdb` | `FacetChoices.of`: primero los valores de la faceta y al final, con conteo 0, los marcados que falten; un marcado no es `extra`. `./mvnw verify`: BUILD SUCCESS, 239 unitarias (core 48, search 58, ingest 92, web 41), 0 fallos. MockMvc: `ResultsPageTest.shouldKeepASelectedValueThatTheFacetNoLongerHas` (facetas vacías, «Ningún resultado cumple los filtros», `genero=Drama` → `<label>` sin `extra` con «Drama» marcado y «0») y `shouldNotHideASelectedValue` (séptimo género marcado sin `extra`). Mutaciones, una a la vez y revertidas: sin agregar los marcados que faltan → la primera; un marcado puede quedar oculto → la segunda. |
| S-12 | terminada | `1099445` | `about.html` y `elastiflix.css`: `.about .mode-cards { flex-direction: row }`, `.mode-icon` de 44 px en las tres tarjetas y en seis encabezados (`.section-head`), paneles `.note` (BM25 celeste, k-NN y precisión verdes, RRF lila, «Cuando no hay resultados» rosa), `.about .eyebrow` en `#A9502A` con icono. Evidencia: captura de página completa con Playwright (1440 px) sobre la app local; las tres tarjetas miden 260 px de alto y comparten `y=451` (en fila; antes 280 px apiladas); la etiqueta tiene `color: rgb(169, 80, 42)`. `AboutPageTest` afirma el panel BM25, el panel «Cuando no hay resultados», `mode-cards` y un `mode-icon`. `./mvnw verify`: BUILD SUCCESS. La aprobación visual queda en S-18. |
| S-13 | terminada | `10e1232` | `results.html`, `elastiflix.css`, `results.js` y `SearchController` (`queryError`): leyendas `.fh`, paginación `.pg` (44×44, actual en `#FFCDB2`, iconos de Anterior y Siguiente), marcador de póster con SVG y `aria-label` (plantilla y `results.js`; sin `onerror` en línea), posición sobre el póster, `h3` Bricolage 21 px, meta mono 12 px, estrella, lupa y flecha en «Buscar», embudo en «Filtros», icono en la nota de facetas (el del mockup es «actualizar», no embudo), `p.limit` con icono, caja de consulta con `aria-invalid`/`aria-describedby`/borde rojo y `h1` oculto en el caso de error. Evidencia: capturas de página completa (Playwright, 1440 px) de Full-text, Vectorial e Híbrida (página 2 de 4, género marcado, pósteres faltantes) y del error de consulta vacía, a partir del HTML que genera MockMvc con la fachada simulada, servido con el CSS y el JS de la app. MockMvc: `ResultsPageTest.shouldShowThePosterMarkWhenThePathIsEmpty` (`posterPath` vacío → `<div class="poster missing" role="img" aria-label="Póster de Titanic"><svg`) y `shouldMarkTheQueryBoxWhenTheQueryIsEmpty`. `./mvnw verify`: BUILD SUCCESS, 242 unitarias (core 48, search 58, ingest 92, web 44), 0 fallos. Capturas con datos reales y aprobación: S-18. |
| S-14 | terminada | `fe4a9e2` | Una prueba por fila, cada una comprobada con una mutación (una a la vez, revertida): `ResultsPageTest.shouldKeepTheFiltersInEveryPageLink` (Anterior, Siguiente, «4» y tamaño 50 con `genero` y `desde`; se quitó la aserción de «Tienes cambios sin aplicar.») ← `SearchLinks.page` sin filtros; `HomePageTest` con `href` completos vectorial e híbrido ← sugerencia sin `candidatos`; `IngestPageTest`: `encabezado=false` e `importar` de `importando` a `publicado` (hechas en S-03); `ServedPagesTest` (nuevo, Tomcat real con fachadas simuladas): pie con TMDB en Resultados, Cargar, Acerca y 404, y `/error` real con 400 (`/buscar?q=%zz`) y 405 (DELETE `/acerca`) ← pie sin TMDB y `h1` vacío; `IndividualRecordTest` con `eq("demo")` y `ArgumentCaptor<Movie>` de 1024/512/256 ← indexar sin vectores; `ImportJobTest` verifica `createWithMapping(temp)` y `writeAll(eq(temp), anyList())` ← escribir en el índice anterior; `InferenceEmbeddingServiceTest.shouldRejectAShortBatchFromTheClient` con `ElasticsearchClient` simulado ← sin exigir el tamaño; `KnnStrategiesTest` (nuevo, estrategias reales con `SearchTransport` simulado): `embedQuery` `times(1)` en el vacío con filtros (vectorial e híbrida), ítem fallido del `_msearch` y 2 partes ← volver a vectorizar / ítem fallido como vacío; `CatalogIndexIT.shouldListOnlyTheDatedIndexesOfTheAlias` (y `shouldNotCreateAnIndexWhenTheAliasIsMissing` para `require_alias` de S-04; pendientes de correr con `-Pit`); `MappingValidatorTest.shouldAcceptTheJsonFixture` afirma que los nodos crudos traen vectores y las películas validadas no ← dejar pasar vectores en `DocumentChecker` y `MovieDocuments`; `PlotVectorizerTest.shouldRejectAnIncompleteBatch` reemplaza la tautología ← sin exigir el tamaño; `JsonDocumentsTest` y `CsvMovieReaderTest` comparan a través de `MappingValidator`. Cambios de código que pedían las pruebas: `ErrorPageController` titula «Error <estado>» un error sin código EFX (parte de S-22) y `PlotVectorizer.attach` usa `EFX-EMB-004` (parte de S-21). `./mvnw verify`: BUILD SUCCESS, 254 unitarias (core 49, search 62, ingest 92, web 51), 0 fallos. |
| S-15 | terminada | `cf177a4` | «Aprobaciones» de `progreso/INDICE.md` tiene una fila para D1, D2, D3 y el cambio del mockup de B-05. Cada una dice lo que consta: las tomó el agente y Adolfo las aceptó al fusionar el PR #10 (`dc9a315`) o el #12 (`41c1188`); no se inventa una aprobación explícita. `progreso/revision-1.md`: R-04, R-09, R-11, R-12, R-15, R-16, R-18, R-19 y R-21 pasan a `incompleta` con «Incompleta: continúa en S-NN (revisión 2).» (9 filas). PLAN 3.0 y 3.2 y `docs/diseno/3.md` (supuesto 9, «Lectores», «Alternativas descartadas», «Cambios» con D2 y la nota de `EFX-ES-007`) describen la ingesta en memoria; `grep -n -i streaming PLAN.md docs/diseno/3.md` solo lo muestra como alternativa descartada o como el supuesto reemplazado. |
| S-16 | terminada | `62c089d` | PLAN §1: encabezado «decisiones del usuario del 2026-10-17 y 2026-10-18»; filas nuevas del prefijo de las sinopsis (R-10 y el mockup editado en B-05) y de «Medido en el catálogo original de 4 322 películas» (R-30, D3). Comparación de textos visibles de los mockups con las páginas servidas (script `textdiff.py` sobre el HTML; en Resultados, el HTML de MockMvc de los tres modos, y cada texto que arma el script del mockup buscado palabra por palabra en su fuente): Inicio sin diferencias (las sugerencias salen del script del mockup); Resultados: leyendas, notas de límite, «Ordenados por…», títulos de puntuación, «Todos», «Solo no adultos», «Géneros: » y «Mostrando » están en el mockup; Cargar datos: las diferencias son las filas de §1 de S-03; Acerca de: las de D3 y un punto de más en «25 · 50 · 100 por página.», corregido (vuelve a ser `dt`/`dd` como en el mockup). Lo demás es dinámico (conexión, conteo). El texto sin conexión «El conteo no está disponible.» se registra con el pie en S-22. `./mvnw verify`: BUILD SUCCESS. |
| S-17 | terminada | se anota en S-18 | `README.md`, `PLAN.md` (nota del catálogo), `AGENTS.md` (línea del catálogo; 11 617 caracteres), `docs/diseno/1.md` (l. 23 y tabla de pruebas), `docs/diseno/5.md` (nota, punto 1 y tabla) y `progreso/5.md` (l. 88 y 112) dicen que las pruebas afirman los 6 899 documentos y los valores de §6 y se omiten con `assumeTrue` si el conteo cambió. `diseno/5.md` registra R-05/D1 en «Cambios». `grep -rn` del criterio sobre `README.md PLAN.md AGENTS.md docs progreso` solo encuentra la propia especificación (`docs/revisiones/revision-2.md`, que no se edita). |
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

- 2026-10-18, S-09 (decisión del agente): las pruebas de JavaScript que piden S-09 y S-10 son pruebas de
  navegador con Playwright en `elastiflix-web/src/test/browser/`, fuera de `./mvnw verify` (necesitan Node.js
  y Chromium). No agregan dependencias Maven; Playwright es dependencia de desarrollo de esa carpeta.

- 2026-10-19, S-14: al probar el `/error` real apareció un fallo fuera de la lista: `/buscar?k=abc`
  responde 500 en lugar de rechazar con `EFX-SRCH-002`. Se corrige en S-22 (mensajes y página de error).

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
