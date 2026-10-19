# Estado de trabajo — Revisión 3 · Carril B (Búsqueda)

> Carril B de la revisión 3. La especificación de cada corrección está en
> [`docs/revisiones/revision-3.md`](../docs/revisiones/revision-3.md); las reglas de los carriles, en su
> sección «Cómo trabajar en paralelo». Este archivo solo lo escribe el agente de este carril.

## Identificación

- **Unidad:** Revisión 3 · carril B
- **Nombre:** Búsqueda
- **Rama:** `revision-3-busqueda` (creada desde `origin/main` en `6c403f4`)
- **Carpeta:** worktree `/workspaces/elastiflix-busqueda`
- **Archivos del carril:** `elastiflix-search/**`
- **Herramienta de la última sesión:** Claude Code (Opus 5.5)

## Estado general

en curso

## Punto de reanudación

- **Tarea actual:** T-10 (`KnnStrategiesTest`)
- **Subpaso en curso:** T-02 y T-03 terminadas
- **Próximo paso:** T-10: agregar a `KnnStrategiesTest` un ítem 4xx del `_msearch` que afirme `EFX-ES-007`.
- **Última actualización:** 2026-10-19 02:17 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-02 (transporte) | terminada | `6aff8d6` | `ElasticsearchSearchTransport.call` atrapa también `JsonpMappingException` → `codeFor(0)` = `EFX-ES-001`, con la causa. Prueba nueva `ElasticsearchSearchTransportTest` (3 casos: `_search`, `_msearch` y `_count` con el cliente simulado lanzando `JsonpMappingException`; afirman `EFX-ES-001`). Rota a mano (sin `JsonpMappingException` en el multi-catch): los 3 casos fallan; revertido. `./mvnw -q -pl elastiflix-search -am verify` en verde (2026-10-19 02:17 UTC). |
| T-03 | terminada | se anota en el commit siguiente | `SearchQueryValidator.requireKnnSettings`: `knn == null \|\| knn.field() == null` → `MISSING_MODE_SETTINGS` (`EFX-SRCH-010`); en un método aparte porque dentro de `requireKnn` PMD marcó `CyclomaticComplexity`. Prueba nueva `SearchQueryValidatorTest.shouldRejectWhenTheKnnSettingsHaveNoField` (código y «Faltan los ajustes de este modo.»). Rota a mano (solo `knn == null`): esa prueba falla (`assertCode:165`); revertido. `./mvnw -q -pl elastiflix-search -am verify` en verde (2026-10-19 02:17 UTC). |
| T-10 (`KnnStrategiesTest`) | pendiente | — | Faltan pruebas unitarias de ramas importantes |
| T-12 (search) | pendiente | — | Código muerto y campos que nadie lee |
| T-18 | pendiente | — | Códigos de error equivocados en búsqueda y mapping |
| T-27 (search) | pendiente | — | Pruebas que no pueden fallar, débiles o con higiene pendiente |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw -q -pl elastiflix-search -am verify`
- **Resultado:** en verde con T-02 y T-03 (core 50 unitarias; search 66 unitarias, 0 fallos; Checkstyle y PMD
  en 0). La primera corrida del carril (02:10, `main` + la prueba de T-02 a medio escribir) solo falló en esa
  prueba nueva (`MsearchRequest` sin `searches`); el resto de `main` pasó.
- **Fecha:** 2026-10-19 02:17 UTC

## Decisiones

- Sesión iniciada 2026-10-19 02:10 UTC con Claude Code (Opus 5.5): carril libre (`git ls-remote` sin ramas
  `revision-3-*`); rama creada desde `origin/main` (`6c403f4`) en el worktree `/workspaces/elastiflix-busqueda`.

## Notas para el cierre

<Cambios de documentación que aplicará el carril Z: filas de PLAN §1 (texto exacto), propiedades de §7,
«Cambios respecto al diseño aprobado» de `docs/diseno/<fase>.md`, tablas de errores y README. También las
firmas viejas que la regla 1 obligó a conservar y que Z puede quitar.>

## Bloqueos y solicitudes

Vacío.
