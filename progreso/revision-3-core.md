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

- **Tarea actual:** cierre del carril
- **Subpaso en curso:** T-27 terminada
- **Próximo paso:** `git merge origin/main`, `./mvnw verify` y `./mvnw verify -Pit` completos, estado `terminado` y PR hacia `main`.
- **Última actualización:** 2026-10-19 02:39 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-01 | terminada | `5f67007` | `copy` cancela también ante `JsonpMappingException`; `cancel` pide `wait_for_completion=false` y confirma con hasta 3 lecturas de `_tasks` de 20 s (WARN con el id si no se confirma); `hasFailures` mira `getValueType()`. `ReindexTasksTest` (14 pruebas, verde): `shouldCancelWhenTheTaskResponseIsUnreadable`, `shouldRetryTheCancelConfirmationUntilTheTaskStops`, `shouldStopConfirmingAfterThreeReads`, `shouldAcceptANullFailuresValue` y `shouldFailWhenTheTaskReportsFailures` (EFX-ES-006). Roturas a mano y revertidas: sin `JsonpMappingException` en `copy` → falla `shouldCancelWhenTheTaskResponseIsUnreadable`; una sola lectura → fallan `shouldStopConfirmingAfterThreeReads` y `shouldRetry…`; `wait_for_completion=true` → falla `shouldRetry…`; `getJsonArray` viejo → falla `shouldAcceptANullFailuresValue`. `./mvnw -q -pl elastiflix-core verify` exit 0 (2026-10-19 02:17 UTC). `shouldCancelWhenStopIsRequested` ahora espera 1 lectura (la de confirmación). |
| T-02 (inferencia) | terminada | `6144997` | `InferenceEmbeddingService.call` atrapa también `JsonpMappingException` y la traduce a `EFX-EMB-001` conservando la causa. Prueba `InferenceEmbeddingServiceTest.shouldTranslateAnUnreadableResponse` (cliente simulado lanza `JsonpMappingException`; afirma código y causa). Rotura a mano (quitar `JsonpMappingException` del multi-catch) → esa prueba falla; revertido. `-Dtest=InferenceEmbeddingServiceTest` 4/4 y `./mvnw -q -pl elastiflix-core verify -DskipTests` exit 0 (2026-10-19 02:19 UTC). La parte de transporte es del carril B. |
| T-10 (core) | terminada | `897fa8c` | Pruebas nuevas: `CatalogBootstrapTest.shouldKeepTheNewIndexWhenPublishGetsAServerError` (503 → EFX-IMP-006, sin borrar), `shouldKeepTheNewIndexWhenTheReindexFailsAndTheOriginalIsGone` (EFX-ES-006 y el nombre ya es alias → no borra) y su pareja `shouldDeleteTheNewIndexWhenTheReindexFailsAndTheOriginalRemains`; `ElasticsearchCatalogIndexTest` (nueva) `shouldPublishWithATenSecondTimeoutInOneCall` y `shouldIndexOneMovieOnlyThroughTheAlias` (petición construida desde la función capturada al cliente simulado); `InferenceEmbeddingServiceTest.shouldSendTheSearchInputTypeAndTheQueryPrefix`. Roturas a mano y revertidas: quitar `isClientRejection` en `confirmOrReject` → fallan `…PublishGetsAServerError` y `…PublishIsCutOff`; borrar siempre en `deleteNewIfOriginalRemains` → falla `…OriginalIsGone`; `PUBLISH_LIMIT` 30s → falla `shouldPublishWithATenSecondTimeoutInOneCall`; `requireAlias(false)` → falla `shouldIndexOneMovieOnlyThroughTheAlias`; `input_type` ingest o prefijo vacío → falla `shouldSendTheSearchInputTypeAndTheQueryPrefix`. Las tres clases en verde (5 + 13 + 2) y `./mvnw -q -pl elastiflix-core verify -DskipTests` exit 0 (2026-10-19 02:26 UTC). |
| T-12 (core) | terminada | `3fb5881` | Quitados `IndexMapping.field()` (lanzaba `IllegalArgumentException`; resuelve también la parte de T-18) y `IndexMapping.document()`; `IndexMappingTest` usa ahora `fields()` y `mappings()`, las firmas reales que usa ingest. Quitado `InferenceEmbeddingService.requireSameSize`: la comprobación quedó dentro de `call` y la cubre `shouldRejectAShortBatchFromTheClient` por la firma real (`embedDocuments`); se borraron sus dos pruebas directas. `CatalogIndex.readWithVectors` / `readAllWithVectors` se conservan (regla 1) con «Lo usan las pruebas de integración» en su Javadoc. `git grep -n "requireSameSize\|mapping.field(\|\.document()" -- '*/src/main'` sin resultados. `./mvnw -q -pl elastiflix-core verify` exit 0, 59 pruebas, 0 fallos (2026-10-19 02:28 UTC). |
| T-19 (`ErrorCode` y `CosineSimilarity`) | terminada | `f7183f8` | Acciones nuevas, neutras («se borra si…; si quedó, bórralo a mano»), sin tocar ningún `summary()`: `REINDEX_COUNT_MISMATCH`, `REINDEX_FAILED` (ahora «Se pide cancelar la tarea», porque T-01 hace la cancelación un intento), `ALIAS_PUBLISH_FAILED` (sin «lo limpia la próxima importación»), `BULK_REJECTED`, `IMPORT_COUNT_MISMATCH`, `IMPORT_CANCELLED`; `PUBLISH_UNCERTAIN` dice «índice nuevo»; la causa de `UNREADABLE_JSON` es «El archivo JSON está mal formado». Javadoc de `CosineSimilarity`: está en `src/main` porque la usan pruebas de core e ingest y producción no la usa. `ErrorCodeTest.shouldDescribeTheCleanupWithoutPromisingIt` afirma los 8 `userMessage()` completos y que ninguno dice «Se borró el índice temporal» ni «lo limpia la próxima importación»; `ErrorContextTest.shouldUseNeutralTextsForCopyAndPublish` también. Rotura: `ErrorCode` de `3fb5881` → fallan las dos pruebas; revertido. `grep -rn "Se borró el índice temporal" elastiflix-*/src/main` sin resultados en esta rama (el texto de `JobStatus.CANCELLED` es del carril C). `./mvnw -q -pl elastiflix-core verify -DskipTests` exit 0 (2026-10-19 02:32 UTC). |
| T-25 (`@Validated` y grupos) | terminada | `598ba6c` | Se conserva `@Validated` (lo pide `docs/convenciones-java.md` §6) con restricciones reales: `@NotBlank` en `elasticsearch.url`, `index.alias` e `inference.id`, con `@Valid` en esos tres grupos. Los grupos `elasticsearch` y `bootstrap` se conservan (regla 1: constructores congelados) y su razón queda en el Javadoc del record. No se tocó el relleno de `null` ni `Poster.fallback()`. Pruebas: `ElastiflixPropertiesTest.shouldRejectABlankUrlWhenBinding` (URL en blanco → el contexto falla nombrando `elastiflix.elasticsearch.url`) y `shouldFillTheBlankAliasAndInferenceIdBeforeValidating`; `ElasticsearchClientConfigTest` ahora pasa una URL ficticia. Roturas a mano y revertidas: quitar `@NotBlank` de `url`, quitar `@Valid` del grupo o quitar `@Validated` → falla `shouldRejectABlankUrlWhenBinding`. `./mvnw -q -pl elastiflix-core verify -DskipTests` exit 0 (2026-10-19 02:36 UTC). |
| T-27 (core) | terminada | sobre `598ba6c` | `CatalogIndexIT`: se quitó la aserción `listOwnedIndexes(alias).isEmpty()` (siempre verdadera; el índice con fecha ya lo cubren `shouldListOnlyTheDatedIndexesOfTheAlias` e `IngestIT`). `ElastiflixPropertiesTest`: se quitó `shouldUseTheCalibratedPairWithoutEmbeddingProperties` (repetía la primera) y la limitación de `@DefaultValue` va a «Decisiones». `EnvironmentIT`: ahora prueba el post-procesador sobre el entorno real de `.env` (lo acepta) y sobre una copia sin URL (`EFX-CFG-001`); rotura a mano (quitar la comprobación de URL del post-procesador) → `EnvironmentIT` falla; revertido. `ElasticsearchClientConfigTest` → `ElasticsearchJsonConfigTest` (`git mv`). Línea de 126 caracteres de `InferenceEmbeddingServiceTest` partida; ninguna línea de más de 120 en `elastiflix-core/src` ni en `ErrorContextTest`. `./mvnw -q -pl elastiflix-core verify -Pit -Dit.test=EnvironmentIT -Dtest=ElastiflixPropertiesTest` exit 0 (2026-10-19 02:39 UTC). |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw -pl elastiflix-core -am verify` (y al final `./mvnw verify` completo)
- **Resultado:** —
- **Fecha:** —

## Decisiones

- Sesión iniciada 2026-10-19 02:10 UTC con Claude Code (Claude Opus 5.5): carril libre, rama creada desde `origin/main` (`6c403f4`).
- **T-19 · textos:** acción neutra «se borra si…; si quedó, bórralo a mano» en los seis códigos que prometían un borrado; `REINDEX_FAILED` dice «Se pide cancelar la tarea» porque desde T-01 la cancelación es un intento confirmado con lecturas. Los `summary()` no cambian (regla 2).
- **T-27 · `ElastiflixPropertiesTest`:** sus pruebas no distinguen si se quita alguno de los dos `@DefaultValue` (`embedding` del record raíz o los de `Embedding`), porque los constructores compactos rellenan los mismos valores. No se puede distinguir sin quitar el relleno de `null`, que usan `IngestLimits` y `PosterUrls` (regla 1). Se deja así y se quitó la prueba repetida.
- **T-27 · `EnvironmentIT`:** se eligió probar el post-procesador (no quitar la prueba), porque `PLAN.md` (tarea 0.6) la nombra como la IT de humo del entorno.
- **T-25 · `@Validated`:** se eligió agregar restricciones en lugar de quitar `@Validated` y el starter, porque `docs/convenciones-java.md` §6 (congelado) pide `@ConfigurationProperties` sobre `record` con `@Validated`. Los grupos `elasticsearch` y `bootstrap` se documentan en el Javadoc en lugar de quitarse: quitarlos cambia el constructor de `ElastiflixProperties`, congelado por la regla 1, y `elasticsearch` ahora lleva la validación de la URL.

## Notas para el cierre

<Cambios de documentación que aplicará el carril Z: filas de PLAN §1 (texto exacto), propiedades de §7,
«Cambios respecto al diseño aprobado» de `docs/diseno/<fase>.md`, tablas de errores y README. También las
firmas viejas que la regla 1 obligó a conservar y que Z puede quitar.>

## Bloqueos y solicitudes

Vacío.
