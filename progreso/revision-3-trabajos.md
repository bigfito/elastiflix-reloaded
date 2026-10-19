# Estado de trabajo — Revisión 3 · Carril C (Carga: trabajos)

> Carril C de la revisión 3. La especificación de cada corrección está en
> [`docs/revisiones/revision-3.md`](../docs/revisiones/revision-3.md); las reglas de los carriles, en su
> sección «Cómo trabajar en paralelo». Este archivo solo lo escribe el agente de este carril.

## Identificación

- **Unidad:** Revisión 3 · carril C
- **Nombre:** Carga: trabajos
- **Rama:** `revision-3-trabajos` (creada desde `origin/main` en `6c403f4`)
- **Carpeta:** worktree `/workspaces/elastiflix-trabajos`
- **Archivos del carril:** paquetes `job/`, `service/` y `vector/` de ingest y `TestApplication.java`, salvo `ErrorContextTest.java`
- **Herramienta de la última sesión:** Claude Code (Opus 5.5)

## Estado general

en curso

## Punto de reanudación

- **Tarea actual:** T-27
- **Subpaso en curso:** ninguno
- **Próximo paso:** T-27: `ImportJobTest.shouldCancelTheCopyBeforeDeletingTheTemp` con `reindex` que lanza `EFX-ES-006` e `InOrder`; `JobFiguresTest.shouldGiveTheSummaryWithoutTheCode` con `isEqualTo(summary())`; `@TempDir` en `JobThreadFailureTest`; partir las líneas de más de 120 en `JobThreadFailureTest` y `JobRetentionTest`.
- **Última actualización:** 2026-10-19 02:47 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-08 | terminada | sobre `1801d02` | `JobFigures.checks` recibe el estado: solo hay comprobaciones en `valido`, `importando` y `publicado`. `./mvnw -q -pl elastiflix-ingest -am test -Dtest=JobFiguresTest` → 8 pruebas, 0 fallos; con el filtro de estado quitado a mano fallan `shouldShowNoChecksWhenTheJobIsStillValidating` y `shouldShowNoChecksWhenTheJobFailed` (revertido). Checkstyle y PMD de ingest en 0 (`verify -DskipTests`). |
| T-09 | terminada | `585e978` (sobre `f0da59b`) | `FileIntake` pasa el paso detenido a `JobStore.invalid(jobId, informe, paso)`: 2 para `EFX-ING-001` a `004` y 3 en los demás; `ImportWork.describe()` pasa el trabajo al paso 3. Nueva `FileIntakeTest` (3 pruebas: `EFX-ING-002` → paso 2; `EFX-ING-012` → paso 3; validando → paso 3). Rompiendo a mano: `stepOf` siempre 3 → falla `shouldStopStepTwoWhenTheFileHasTwoExtensions`; sin `step = VALIDATION_STEP` en `describe` → falla `shouldMoveToStepThreeWhenTheMappingValidationStarts`; el informe de campos con paso 2 → falla `shouldStopStepThreeWhenAFieldHasTheWrongType` (revertidos). `./mvnw -q -pl elastiflix-ingest -am verify` → BUILD SUCCESS (exit 0). |
| T-10 (`ImportJob`, `ImportRunner`, `JobStore`) | terminada | `c53d542` (sobre `585e978`) | Nuevas `ImportJobEdgeTest` (7 pruebas: `REINDEX_COUNT_MISMATCH` en la copia, `CATALOG_NOT_FOUND`, `ALIAS_AMBIGUOUS`, 5 películas en 3 lotes de 2, cancelar entre lotes, cancelar justo antes de `swap`, `deleteTemp` que falla con su `WARN` capturado con `ListAppender`) e `ImportRunnerTest` (3: éxito → `publicado` en el paso 5; `IMPORT_CANCELLED` → `cancelado`; `BULK_REJECTED` → `fallido`). `ImportJobTest` ya tiene 375 líneas: las pruebas nuevas van en otra clase (ver «Decisiones»). Rompiendo a mano, una a la vez y revirtiendo: sin la comparación de conteos de `copy` → falla `shouldRollBackWhenTheCopyCountDoesNotMatch`; `Missing` y `Ambiguous` como `CATALOG_NOT_ALIAS` → fallan `shouldRefuseAMissingCatalog` y `shouldRefuseAnAmbiguousAlias`; un solo lote → falla `shouldWriteSeveralBatches`; sin `ensureRunning` en `BatchWriter.writeOne` → falla `shouldStopBetweenBatchesWhenCancelled`; sin `ensureRunning` antes de `swap` → falla `shouldStopRightBeforeTheSwapWhenCancelled`; sin el `LOG.warn` de `deleteTemp` → falla `shouldWarnAndKeepTheOriginalErrorWhenTheTempCannotBeDeleted`; sin `store.published` → falla `shouldPublishWhenTheImportSucceeds`; `importStopped` sin distinguir la cancelación → falla `shouldCancelWhenTheImportIsCancelled`. `./mvnw -q -pl elastiflix-ingest -am test -Dtest='ImportJobEdgeTest,ImportRunnerTest'` → exit 0. |
| T-12 (`ImportCounts`, `ImportProgress`) | terminada | `07efc7f` (sobre `c53d542`) | `ImportCounts` queda `(fileCount, batchSize)` e `ImportProgress` `(step, batch, vectorized, written, tempIndex)`; `WriteTotals.replaced` se queda porque `ImportJob.verifiedCount` lo usa para el conteo esperado. `git grep -nE 'counts\(\)\.replaceCount|ImportProgress.*replaced|progress\.replaced' -- 'elastiflix-*/src/main'` sin resultados (`replaceCount` de `ValidatedMovies`, `ImportWork` y `JobView` es la cifra de la validación y sí se lee). `./mvnw -q -pl elastiflix-ingest -am verify` → exit 0. |
| T-16 | terminada | `3a55699` (sobre `07efc7f`) | Estado final: `ImportJob.published` atrapa la `ElastiflixException` del aviso del paso 5 con un `WARN` y `run` vuelve sin excepción, así que `ImportRunner` marca `publicado`. Excepción original: nueva `service/JobThreads` arranca los hilos virtuales con `uncaughtExceptionHandler` que registra `EFX-IMP-007 hilo={} tipo={}` (sin el mensaje, que podría traer contenido del archivo). Nombres: `ingest-validate-<jobId>` e `ingest-import-<jobId>`. Pruebas: `ImportJobTest.shouldKeepThePublishedTempWhenTheListenerFails` (ahora sin excepción), `ImportRunnerTest.shouldStayPublishedWhenTheStepFiveNoticeFails` (`ImportJob` real, `JobStore` espiado que lanza en el paso 5 → `publicado`), `JobThreadsTest` (2: el log del manejador y el nombre con el id del trabajo vía `IngestService.acceptFile`). Rompiendo a mano y revirtiendo: relanzar en el `catch` del aviso → fallan las dos primeras; sin `uncaughtExceptionHandler` → fallan las dos de `JobThreadsTest`; nombre sin el id → falla `shouldNameTheValidationThreadAfterTheJob`. `./mvnw -q -pl elastiflix-ingest -am verify` → exit 0. |
| T-17 (`FileIntake` y `JobStore`) | terminada | `d7a9067` (sobre `3a55699`) | Parte C (la de `document_count` es del carril D). (1) `FileIntake` valida con `Set.of()` y solo con el archivo válido pide `existingIds` de los ids de las películas: `FileIntakeTest.shouldReportTheErrorsWithoutAskingTheCatalogWhenTheFileIsInvalid` (catálogo que lanza `EFX-ES-001` → `invalido` con `EFX-ING-012` y sin llamada) y `shouldCountReplacementsWhenTheFileIsValid`. (2) Nueva `ReportOrder.join`: encabezado y errores del archivo primero, después por número de fila, orden estable; `CsvRowNumbersTest.shouldReportWidthAndFieldErrorsTogether` pasa a `containsExactly` (filas 2, 4, 5). (3) `JobStore.view` y `beginImport` también llaman a `evict()`: `JobRetentionTest.shouldExpireAnIdleValidJobWhenAnotherJobIsViewed` y `shouldNotImportAnExpiredValidJob`. Rompiendo a mano y revirtiendo: `existingIds` antes de validar → falla la primera; sin el `sort` → falla `shouldReportWidthAndFieldErrorsTogether`; sin `evict()` en `view` / en `beginImport` → falla la prueba de cada uno. `./mvnw -q -pl elastiflix-ingest -am verify` → exit 0. |
| T-19 (Javadoc y `JobStatus`) | terminada | `dc9c84a` (sobre `d7a9067`) | Parte C (la de `ErrorCode`, `ErrorCodeTest`, `ErrorContextTest` y `CosineSimilarity` es del carril A; la de `SearchController`, del E). Javadoc fiel: `JobView` `@param step` empieza en 1; `ImportRunner` y `ImportJob.run` cuentan el borrado del temporal que puede fallar con `WARN` y el caso `EFX-IMP-006` (el temporal se deja); `BatchWriter` avisa antes y después de escribir cada lote; `JobStatus.CANCELLED`: «El índice temporal, si existía, se borra; si no se pudo borrar, queda un WARN con su nombre y hay que borrarlo a mano». Solo Javadoc: no hay comportamiento que romper. `grep -rn "Se borró el índice temporal" elastiflix-ingest/src/main elastiflix-web/src/main` → 0 líneas (las 3 que quedan están en `ErrorCode`, del carril A). `./mvnw -q -pl elastiflix-ingest -am verify -DskipTests` (Checkstyle y PMD) → exit 0. |
| T-24 (`job/`) | terminada | sobre `dc9c84a` | Parte C (BOM y nombres de vectores en `csv/`, `mapping/` y `movie/` son del carril D). Duplicados: `causeType` → nueva `CauseType.of`; `ensureRunning` queda solo en `ImportJob` (paquete) y lo llama `BatchWriter`; los pasos 1 a 5 → nueva `JobSteps` (ya no hay `*_STEP` en `ImportJob`, `BatchWriter`, `ImportWork` ni `FileIntake`); JSON/CSV por nombre → `JobFigures.isCsv`, que usan `kindOf` y `FileGate.unreadable`; `IndividualRecord.VECTORS` e `ImportSample.DROPPED` usan `VectorFields.NAMES`. `FileIntake` recibe el `MappingValidator` por constructor. Costo por sondeo: nueva `ReportFigures` (vista previa, total, registros inválidos y advertencias agrupadas) se calcula en `ready` e `invalid`; `view` ya no recorre el informe. Prueba: `JobFiguresTest.shouldReuseTheReportFiguresWhenThePagePolls` (dos consultas devuelven las mismas listas); con `ReportFigures.of` dentro de `view` falla (revertido). `git grep -nE '"plot_256"|_STEP =|causeType|new MappingValidator\(\)'` en `job/` y `service/` de `src/main` → sin resultados. `./mvnw -q -pl elastiflix-ingest -am verify` → exit 0. |
| T-27 (`job/`) | pendiente | — | Pruebas que no pueden fallar, débiles o con higiene pendiente |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw -pl elastiflix-ingest -am verify` (y al final `./mvnw verify` completo)
- **Resultado:** —
- **Fecha:** —

## Decisiones

- 2026-10-19 02:10 UTC: carril C tomado con Claude Code (Opus 5.5) en el worktree
  `/workspaces/elastiflix-trabajos`, rama `revision-3-trabajos` desde `origin/main` (`6c403f4`).
- T-09: `JobStore.invalid` recibe el paso detenido como tercer parámetro en lugar de agregar una sobrecarga,
  porque una sobrecarga deja `JobStore` en 21 métodos (PMD `TooManyMethods`). Ningún otro módulo lo usa.
- T-10: las pruebas nuevas de `ImportJob` van en `ImportJobEdgeTest` y no en `ImportJobTest`, que ya tiene
  375 líneas: juntas pasarían el tope de 400 líneas por archivo de `docs/convenciones-java.md` §5.
- T-24: no se pone tope a las incidencias guardadas por trabajo. PLAN 3.4 pide el informe completo exportable a
  CSV (`errorReport`), y con un tope ese CSV quedaría incompleto. El costo por sondeo se resuelve con
  `ReportFigures`, calculada una vez en `ready` e `invalid`.

## Notas para el cierre

- **T-17, `docs/diseno/3.md`:** en el diagrama de secuencia (aprox. l. 199), `existingIds` ocurre al final del
  paso 3 y solo si el archivo es válido. En «Revisión 2, S-06» (aprox. l. 612), la revisión de vencidos ya no
  ocurre solo «al abrir el siguiente trabajo»: también al consultar un trabajo (`JobStore.view`) y al
  importar (`beginImport`). Lo mismo en PLAN 3.0 («revisado al abrir el siguiente trabajo»). El informe de
  errores va en orden de fila, con el encabezado y los errores del archivo primero (`ReportOrder`).
- **T-24, `docs/diseno/3.md`:** `FileIntake` ya recibe el `MappingValidator` inyectado, como dice el diseño;
  si alguna tabla de clases de `job/` las enumera, agregar `JobSteps`, `CauseType`, `ReportFigures` y
  `ReportOrder` (paquete, sin API pública). Los hilos de la carga se llaman `ingest-validate-<jobId>` e
  `ingest-import-<jobId>` y los arranca `service/JobThreads` (T-16).

## Bloqueos y solicitudes

Vacío.
