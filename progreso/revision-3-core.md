# Estado de trabajo — Revisión 3 · Carril A (Núcleo)

> Carril A de la revisión 3. La especificación de cada corrección está en
> [`docs/revisiones/revision-3.md`](../docs/revisiones/revision-3.md); las reglas de los carriles, en su
> sección «Cómo trabajar en paralelo». Este archivo solo lo escribe el agente de este carril.

## Identificación

- **Unidad:** Revisión 3 · carril A
- **Nombre:** Núcleo
- **Rama:** `revision-3-core` (creada desde `origin/main` en `6c403f4`)
- **Carpeta:** worktree `/workspaces/elastiflix-core`
- **Archivos del carril:** `elastiflix-core/**` y `ErrorContextTest.java` de ingest
- **Herramienta de la última sesión:** Claude Code (Claude Opus 5.5)

## Estado general

en curso

## Punto de reanudación

- **Tarea actual:** T-19 (ErrorCode y CosineSimilarity)
- **Subpaso en curso:** T-12 terminada
- **Próximo paso:** reescribir en `ErrorCode` la acción de `ALIAS_PUBLISH_FAILED`, `REINDEX_COUNT_MISMATCH`, `PUBLISH_UNCERTAIN`, `BULK_REJECTED`, `IMPORT_COUNT_MISMATCH`, `IMPORT_CANCELLED` y la causa de `UNREADABLE_JSON` (sin tocar `summary()`); afirmarlos en `ErrorCodeTest` y `ErrorContextTest`; corregir el Javadoc de `CosineSimilarity`.
- **Última actualización:** 2026-10-19 02:28 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-01 | terminada | `5f67007` | `copy` cancela también ante `JsonpMappingException`; `cancel` pide `wait_for_completion=false` y confirma con hasta 3 lecturas de `_tasks` de 20 s (WARN con el id si no se confirma); `hasFailures` mira `getValueType()`. `ReindexTasksTest` (14 pruebas, verde): `shouldCancelWhenTheTaskResponseIsUnreadable`, `shouldRetryTheCancelConfirmationUntilTheTaskStops`, `shouldStopConfirmingAfterThreeReads`, `shouldAcceptANullFailuresValue` y `shouldFailWhenTheTaskReportsFailures` (EFX-ES-006). Roturas a mano y revertidas: sin `JsonpMappingException` en `copy` → falla `shouldCancelWhenTheTaskResponseIsUnreadable`; una sola lectura → fallan `shouldStopConfirmingAfterThreeReads` y `shouldRetry…`; `wait_for_completion=true` → falla `shouldRetry…`; `getJsonArray` viejo → falla `shouldAcceptANullFailuresValue`. `./mvnw -q -pl elastiflix-core verify` exit 0 (2026-10-19 02:17 UTC). `shouldCancelWhenStopIsRequested` ahora espera 1 lectura (la de confirmación). |
| T-02 (inferencia) | terminada | `6144997` | `InferenceEmbeddingService.call` atrapa también `JsonpMappingException` y la traduce a `EFX-EMB-001` conservando la causa. Prueba `InferenceEmbeddingServiceTest.shouldTranslateAnUnreadableResponse` (cliente simulado lanza `JsonpMappingException`; afirma código y causa). Rotura a mano (quitar `JsonpMappingException` del multi-catch) → esa prueba falla; revertido. `-Dtest=InferenceEmbeddingServiceTest` 4/4 y `./mvnw -q -pl elastiflix-core verify -DskipTests` exit 0 (2026-10-19 02:19 UTC). La parte de transporte es del carril B. |
| T-10 (core) | terminada | `897fa8c` | Pruebas nuevas: `CatalogBootstrapTest.shouldKeepTheNewIndexWhenPublishGetsAServerError` (503 → EFX-IMP-006, sin borrar), `shouldKeepTheNewIndexWhenTheReindexFailsAndTheOriginalIsGone` (EFX-ES-006 y el nombre ya es alias → no borra) y su pareja `shouldDeleteTheNewIndexWhenTheReindexFailsAndTheOriginalRemains`; `ElasticsearchCatalogIndexTest` (nueva) `shouldPublishWithATenSecondTimeoutInOneCall` y `shouldIndexOneMovieOnlyThroughTheAlias` (petición construida desde la función capturada al cliente simulado); `InferenceEmbeddingServiceTest.shouldSendTheSearchInputTypeAndTheQueryPrefix`. Roturas a mano y revertidas: quitar `isClientRejection` en `confirmOrReject` → fallan `…PublishGetsAServerError` y `…PublishIsCutOff`; borrar siempre en `deleteNewIfOriginalRemains` → falla `…OriginalIsGone`; `PUBLISH_LIMIT` 30s → falla `shouldPublishWithATenSecondTimeoutInOneCall`; `requireAlias(false)` → falla `shouldIndexOneMovieOnlyThroughTheAlias`; `input_type` ingest o prefijo vacío → falla `shouldSendTheSearchInputTypeAndTheQueryPrefix`. Las tres clases en verde (5 + 13 + 2) y `./mvnw -q -pl elastiflix-core verify -DskipTests` exit 0 (2026-10-19 02:26 UTC). |
| T-12 (core) | terminada | sobre `897fa8c` | Quitados `IndexMapping.field()` (lanzaba `IllegalArgumentException`; resuelve también la parte de T-18) y `IndexMapping.document()`; `IndexMappingTest` usa ahora `fields()` y `mappings()`, las firmas reales que usa ingest. Quitado `InferenceEmbeddingService.requireSameSize`: la comprobación quedó dentro de `call` y la cubre `shouldRejectAShortBatchFromTheClient` por la firma real (`embedDocuments`); se borraron sus dos pruebas directas. `CatalogIndex.readWithVectors` / `readAllWithVectors` se conservan (regla 1) con «Lo usan las pruebas de integración» en su Javadoc. `git grep -n "requireSameSize\|mapping.field(\|\.document()" -- '*/src/main'` sin resultados. `./mvnw -q -pl elastiflix-core verify` exit 0, 59 pruebas, 0 fallos (2026-10-19 02:28 UTC). |
| T-19 (`ErrorCode` y `CosineSimilarity`) | pendiente | — | Textos de error y Javadoc que no corresponden al comportamiento |
| T-25 (`@Validated` y grupos) | pendiente | — | Configuración y beans armados para las pruebas |
| T-27 (core) | pendiente | — | Pruebas que no pueden fallar, débiles o con higiene pendiente |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw -pl elastiflix-core -am verify` (y al final `./mvnw verify` completo)
- **Resultado:** —
- **Fecha:** —

## Decisiones

- Sesión iniciada 2026-10-19 02:10 UTC con Claude Code (Claude Opus 5.5): carril libre, rama creada desde `origin/main` (`6c403f4`).

## Notas para el cierre

<Cambios de documentación que aplicará el carril Z: filas de PLAN §1 (texto exacto), propiedades de §7,
«Cambios respecto al diseño aprobado» de `docs/diseno/<fase>.md`, tablas de errores y README. También las
firmas viejas que la regla 1 obligó a conservar y que Z puede quitar.>

## Bloqueos y solicitudes

Vacío.
