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

- **Tarea actual:** T-16
- **Subpaso en curso:** ninguno
- **Próximo paso:** T-16: que el aviso del paso 5 que lanza no deje el trabajo `fallido`, `uncaughtExceptionHandler` con `LOG.error("EFX-IMP-007 hilo={} tipo={}")` y nombres de hilo con el id del trabajo.
- **Última actualización:** 2026-10-19 02:33 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-08 | terminada | sobre `1801d02` | `JobFigures.checks` recibe el estado: solo hay comprobaciones en `valido`, `importando` y `publicado`. `./mvnw -q -pl elastiflix-ingest -am test -Dtest=JobFiguresTest` → 8 pruebas, 0 fallos; con el filtro de estado quitado a mano fallan `shouldShowNoChecksWhenTheJobIsStillValidating` y `shouldShowNoChecksWhenTheJobFailed` (revertido). Checkstyle y PMD de ingest en 0 (`verify -DskipTests`). |
| T-09 | terminada | `585e978` (sobre `f0da59b`) | `FileIntake` pasa el paso detenido a `JobStore.invalid(jobId, informe, paso)`: 2 para `EFX-ING-001` a `004` y 3 en los demás; `ImportWork.describe()` pasa el trabajo al paso 3. Nueva `FileIntakeTest` (3 pruebas: `EFX-ING-002` → paso 2; `EFX-ING-012` → paso 3; validando → paso 3). Rompiendo a mano: `stepOf` siempre 3 → falla `shouldStopStepTwoWhenTheFileHasTwoExtensions`; sin `step = VALIDATION_STEP` en `describe` → falla `shouldMoveToStepThreeWhenTheMappingValidationStarts`; el informe de campos con paso 2 → falla `shouldStopStepThreeWhenAFieldHasTheWrongType` (revertidos). `./mvnw -q -pl elastiflix-ingest -am verify` → BUILD SUCCESS (exit 0). |
| T-10 (`ImportJob`, `ImportRunner`, `JobStore`) | terminada | `c53d542` (sobre `585e978`) | Nuevas `ImportJobEdgeTest` (7 pruebas: `REINDEX_COUNT_MISMATCH` en la copia, `CATALOG_NOT_FOUND`, `ALIAS_AMBIGUOUS`, 5 películas en 3 lotes de 2, cancelar entre lotes, cancelar justo antes de `swap`, `deleteTemp` que falla con su `WARN` capturado con `ListAppender`) e `ImportRunnerTest` (3: éxito → `publicado` en el paso 5; `IMPORT_CANCELLED` → `cancelado`; `BULK_REJECTED` → `fallido`). `ImportJobTest` ya tiene 375 líneas: las pruebas nuevas van en otra clase (ver «Decisiones»). Rompiendo a mano, una a la vez y revirtiendo: sin la comparación de conteos de `copy` → falla `shouldRollBackWhenTheCopyCountDoesNotMatch`; `Missing` y `Ambiguous` como `CATALOG_NOT_ALIAS` → fallan `shouldRefuseAMissingCatalog` y `shouldRefuseAnAmbiguousAlias`; un solo lote → falla `shouldWriteSeveralBatches`; sin `ensureRunning` en `BatchWriter.writeOne` → falla `shouldStopBetweenBatchesWhenCancelled`; sin `ensureRunning` antes de `swap` → falla `shouldStopRightBeforeTheSwapWhenCancelled`; sin el `LOG.warn` de `deleteTemp` → falla `shouldWarnAndKeepTheOriginalErrorWhenTheTempCannotBeDeleted`; sin `store.published` → falla `shouldPublishWhenTheImportSucceeds`; `importStopped` sin distinguir la cancelación → falla `shouldCancelWhenTheImportIsCancelled`. `./mvnw -q -pl elastiflix-ingest -am test -Dtest='ImportJobEdgeTest,ImportRunnerTest'` → exit 0. |
| T-12 (`ImportCounts`, `ImportProgress`) | terminada | sobre `c53d542` | `ImportCounts` queda `(fileCount, batchSize)` e `ImportProgress` `(step, batch, vectorized, written, tempIndex)`; `WriteTotals.replaced` se queda porque `ImportJob.verifiedCount` lo usa para el conteo esperado. `git grep -nE 'counts\(\)\.replaceCount|ImportProgress.*replaced|progress\.replaced' -- 'elastiflix-*/src/main'` sin resultados (`replaceCount` de `ValidatedMovies`, `ImportWork` y `JobView` es la cifra de la validación y sí se lee). `./mvnw -q -pl elastiflix-ingest -am verify` → exit 0. |
| T-16 | pendiente | — | Hilos de la carga: estado final, excepciones y nombres |
| T-17 (`FileIntake` y `JobStore`) | pendiente | — | Detalles de la validación de archivos |
| T-19 (Javadoc y `JobStatus`) | pendiente | — | Textos de error y Javadoc que no corresponden al comportamiento |
| T-24 (`job/`) | pendiente | — | Duplicados y costo por sondeo en la carga |
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

## Notas para el cierre

Vacío por ahora.

## Bloqueos y solicitudes

Vacío.
