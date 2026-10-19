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

- **Tarea actual:** T-02 (inferencia)
- **Subpaso en curso:** T-01 terminada
- **Próximo paso:** en `InferenceEmbeddingService.call` agregar `JsonpMappingException` al multi-catch (EFX-EMB-001) y su prueba en `InferenceEmbeddingServiceTest`.
- **Última actualización:** 2026-10-19 02:17 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-01 | terminada | sobre `dcfe652` | `copy` cancela también ante `JsonpMappingException`; `cancel` pide `wait_for_completion=false` y confirma con hasta 3 lecturas de `_tasks` de 20 s (WARN con el id si no se confirma); `hasFailures` mira `getValueType()`. `ReindexTasksTest` (14 pruebas, verde): `shouldCancelWhenTheTaskResponseIsUnreadable`, `shouldRetryTheCancelConfirmationUntilTheTaskStops`, `shouldStopConfirmingAfterThreeReads`, `shouldAcceptANullFailuresValue` y `shouldFailWhenTheTaskReportsFailures` (EFX-ES-006). Roturas a mano y revertidas: sin `JsonpMappingException` en `copy` → falla `shouldCancelWhenTheTaskResponseIsUnreadable`; una sola lectura → fallan `shouldStopConfirmingAfterThreeReads` y `shouldRetry…`; `wait_for_completion=true` → falla `shouldRetry…`; `getJsonArray` viejo → falla `shouldAcceptANullFailuresValue`. `./mvnw -q -pl elastiflix-core verify` exit 0 (2026-10-19 02:17 UTC). `shouldCancelWhenStopIsRequested` ahora espera 1 lectura (la de confirmación). |
| T-02 (inferencia) | pendiente | — | Búsqueda e inferencia no traducen una respuesta ilegible a un código EFX |
| T-10 (core) | pendiente | — | Faltan pruebas unitarias de ramas importantes |
| T-12 (core) | pendiente | — | Código muerto y campos que nadie lee |
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
