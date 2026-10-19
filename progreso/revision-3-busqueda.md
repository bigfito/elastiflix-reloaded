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

- **Tarea actual:** T-18
- **Subpaso en curso:** T-02, T-03, T-10 y T-12 terminadas
- **Próximo paso:** T-18: en `HybridSearchStrategy.join`, un `_msearch` con partes de más o de menos da
  `EFX-ES-007` y `KnnStrategiesTest.shouldFailWhenTheMultiSearchHasTheWrongNumberOfParts` lo afirma.
- **Última actualización:** 2026-10-19 02:22 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-02 (transporte) | terminada | `6aff8d6` | `ElasticsearchSearchTransport.call` atrapa también `JsonpMappingException` → `codeFor(0)` = `EFX-ES-001`, con la causa. Prueba nueva `ElasticsearchSearchTransportTest` (3 casos: `_search`, `_msearch` y `_count` con el cliente simulado lanzando `JsonpMappingException`; afirman `EFX-ES-001`). Rota a mano (sin `JsonpMappingException` en el multi-catch): los 3 casos fallan; revertido. `./mvnw -q -pl elastiflix-search -am verify` en verde (2026-10-19 02:17 UTC). |
| T-03 | terminada | `069c76a` | `SearchQueryValidator.requireKnnSettings`: `knn == null \|\| knn.field() == null` → `MISSING_MODE_SETTINGS` (`EFX-SRCH-010`); en un método aparte porque dentro de `requireKnn` PMD marcó `CyclomaticComplexity`. Prueba nueva `SearchQueryValidatorTest.shouldRejectWhenTheKnnSettingsHaveNoField` (código y «Faltan los ajustes de este modo.»). Rota a mano (solo `knn == null`): esa prueba falla (`assertCode:165`); revertido. `./mvnw -q -pl elastiflix-search -am verify` en verde (2026-10-19 02:17 UTC). |
| T-10 (`KnnStrategiesTest`) | terminada | `126b4b8` | Prueba nueva `KnnStrategiesTest.shouldGiveTheRejectedCodeWhenAPartOfTheMultiSearchIsAClientError`: el tercer ítem del `_msearch` llega con `status 400` (`illegal_argument_exception`) y la búsqueda híbrida lanza `ElasticsearchAccessException` con `EFX-ES-007`. Rota a mano (`HybridSearchStrategy.rejected` con `codeFor(0)` en lugar del estado del ítem): esa prueba falla (línea 129); revertido. `./mvnw -q -pl elastiflix-search -am test -Dtest=KnnStrategiesTest`: 5 pruebas, 0 fallos (2026-10-19 02:19 UTC). Evidencia aquí y no en el tablero `progreso/revision-3.md` (lo escribe Z). |
| T-12 (search) | terminada | se anota en el commit siguiente | Quitados `Rrf.term(int)` (la IT `HybridSearchIT` calcula `1 / (Rrf.RANK_CONSTANT + posición)` por su cuenta), `TextOperator.elasticsearchOperator()` con su campo y constructor (el enum queda `OR`, `AND`; `LexicalQueryBuilder.elasticsearch()` sigue siendo el único traductor), el componente `RankedHit.rrf` (queda `rrf()` como método, regla 1) y el parámetro `preference` de `VectorRequestBuilder.build` (y la `null` de `VectorSearchStrategy`). `./mvnw -q -pl elastiflix-search -am verify` en verde: search 67 unitarias, 0 fallos, Checkstyle y PMD en 0; `./mvnw -q -pl elastiflix-web -am test-compile` en verde. `grep -rn -e "Rrf.term" -e "term(int" -e "elasticsearchOperator" elastiflix-*/src/main` sin resultados; `VectorRequestBuilder.build` ya no tiene `preference` (2026-10-19 02:22 UTC). |
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
- T-12 (`RankedHit.rrf()`): la regla 1 pide conservar `rrf()` «como método que devuelve `score`». Devuelve
  `score` cuando el resultado tiene componentes (híbrida) y `null` si no los tiene, igual que el campo de antes
  (`RankedHit.scored` lo dejaba `null`). Así `SearchHit.rrf` sigue siendo «`null` fuera de la híbrida», como dice su
  Javadoc, y la web no cambia. `ResultCards` lee `SearchHit.rrf()`, no `RankedHit.rrf()`: en `src/main` solo lo
  llama `SearchService`.
- T-12: `HybridRequestBuilder.build` también recibe un `preference` que siempre llega `null` desde
  `HybridSearchStrategy` (lo genera dentro). La lista solo pide quitar el de `VectorRequestBuilder.build`, así que
  se deja; se anota en «Notas para el cierre».

## Notas para el cierre

- **T-12, regla 1:** `RankedHit.rrf()` quedó como método (devuelve `score` en la híbrida y `null` fuera). Si Z
  quiere, puede cambiar `SearchService.hit` para leer `score` según el modo y quitar `rrf()`; no hay otros usos en
  `src/main`.
- **T-12, fuera de la lista:** el parámetro `preference` de `HybridRequestBuilder.build` siempre llega `null` desde
  `HybridSearchStrategy`; solo lo usan las pruebas. Candidato para una revisión siguiente.
- **T-12, documentación:** `docs/diseno/2.md` no nombra `Rrf.term`, `TextOperator.elasticsearchOperator()`, el
  componente `rrf` de `RankedHit` ni el `preference` de `VectorRequestBuilder.build` (comprobado con `grep`); no
  hay que cambiarlo por T-12.

## Bloqueos y solicitudes

Vacío.
