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

terminado

## Punto de reanudación

- **Tarea actual:** ninguna; carril terminado
- **Subpaso en curso:** PR «Revisión 3 · carril B (Búsqueda)» abierto hacia `main`
- **Próximo paso:** esperar a que Adolfo fusione el PR. Después, el carril Z aplica las «Notas para el cierre».
- **Última actualización:** 2026-10-19 02:35 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-02 (transporte) | terminada | `6aff8d6` | `ElasticsearchSearchTransport.call` atrapa también `JsonpMappingException` → `codeFor(0)` = `EFX-ES-001`, con la causa. Prueba nueva `ElasticsearchSearchTransportTest` (3 casos: `_search`, `_msearch` y `_count` con el cliente simulado lanzando `JsonpMappingException`; afirman `EFX-ES-001`). Rota a mano (sin `JsonpMappingException` en el multi-catch): los 3 casos fallan; revertido. `./mvnw -q -pl elastiflix-search -am verify` en verde (2026-10-19 02:17 UTC). |
| T-03 | terminada | `069c76a` | `SearchQueryValidator.requireKnnSettings`: `knn == null \|\| knn.field() == null` → `MISSING_MODE_SETTINGS` (`EFX-SRCH-010`); en un método aparte porque dentro de `requireKnn` PMD marcó `CyclomaticComplexity`. Prueba nueva `SearchQueryValidatorTest.shouldRejectWhenTheKnnSettingsHaveNoField` (código y «Faltan los ajustes de este modo.»). Rota a mano (solo `knn == null`): esa prueba falla (`assertCode:165`); revertido. `./mvnw -q -pl elastiflix-search -am verify` en verde (2026-10-19 02:17 UTC). |
| T-10 (`KnnStrategiesTest`) | terminada | `126b4b8` | Prueba nueva `KnnStrategiesTest.shouldGiveTheRejectedCodeWhenAPartOfTheMultiSearchIsAClientError`: el tercer ítem del `_msearch` llega con `status 400` (`illegal_argument_exception`) y la búsqueda híbrida lanza `ElasticsearchAccessException` con `EFX-ES-007`. Rota a mano (`HybridSearchStrategy.rejected` con `codeFor(0)` en lugar del estado del ítem): esa prueba falla (línea 129); revertido. `./mvnw -q -pl elastiflix-search -am test -Dtest=KnnStrategiesTest`: 5 pruebas, 0 fallos (2026-10-19 02:19 UTC). Evidencia aquí y no en el tablero `progreso/revision-3.md` (lo escribe Z). |
| T-12 (search) | terminada | `19e53ff` | Quitados `Rrf.term(int)` (la IT `HybridSearchIT` calcula `1 / (Rrf.RANK_CONSTANT + posición)` por su cuenta), `TextOperator.elasticsearchOperator()` con su campo y constructor (el enum queda `OR`, `AND`; `LexicalQueryBuilder.elasticsearch()` sigue siendo el único traductor), el componente `RankedHit.rrf` (queda `rrf()` como método, regla 1) y el parámetro `preference` de `VectorRequestBuilder.build` (y la `null` de `VectorSearchStrategy`). `./mvnw -q -pl elastiflix-search -am verify` en verde: search 67 unitarias, 0 fallos, Checkstyle y PMD en 0; `./mvnw -q -pl elastiflix-web -am test-compile` en verde. `grep -rn -e "Rrf.term" -e "term(int" -e "elasticsearchOperator" elastiflix-*/src/main` sin resultados; `VectorRequestBuilder.build` ya no tiene `preference` (2026-10-19 02:22 UTC). |
| T-18 | terminada | `1b9a58e` | `HybridSearchStrategy.join`: un `_msearch` con partes de más o de menos lanza `ElasticsearchAccessException` con `EFX-ES-007` (`ELASTICSEARCH_REJECTED`) y registra un `ERROR` con las partes recibidas y esperadas (método `wrongParts`). `KnnStrategiesTest.shouldFailWhenTheMultiSearchHasTheWrongNumberOfParts` afirma ahora el código. Rota a mano (de vuelta a `rejected("respuesta", 0)`, que da `EFX-ES-001`): esa prueba falla (línea 143); revertido. `./mvnw -q -pl elastiflix-search -am verify` en verde: search 67 unitarias, 0 fallos (2026-10-19 02:25 UTC). La parte de `IndexMapping.field` es de A (T-12). |
| T-27 (search) | terminada | `dd4696b` | `ScoreJoinerTest`: se quitó la aserción de la fórmula del helper (`Rrf.term`, en T-12) y se agregó `shouldKeepTheFusedOrderWhenTheMovieIsOutsideBothLists`, que prueba el joiner: orden y `score`/`rrf()` de la lista fusionada, una película fuera de las dos listas con `fuera de su top k` en ambas, y puntuación y posición copiadas de cada lista. Rota a mano (`ScoreJoiner.index` con posiciones invertidas): fallan las dos pruebas de `ScoreJoinerTest` (líneas 36 y 70); revertido. `KnnStrategiesTest`: las dos líneas de 121 caracteres (l. 105 y la de `response`) quedaron partidas; la de l. 105 usa el helper `failure(status, type)`. Ninguna línea de `elastiflix-search` pasa de 120 caracteres (contadas en caracteres con Python). `./mvnw -q -pl elastiflix-search -am test -Dtest=ScoreJoinerTest,KnnStrategiesTest`: 2 + 5 pruebas, 0 fallos (2026-10-19 02:27 UTC). |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `git merge origin/main` (sin cambios: `origin/main` seguía en `6c403f4`), `./mvnw verify` y
  `./mvnw verify -Pit` (con `.env`), con el árbol de `dd4696b`.
- **Resultado:** en verde.
  - `./mvnw verify`: BUILD SUCCESS (2026-10-19 02:27–02:31 UTC). 269 unitarias (core 50, search 68, ingest 97,
    web 54), 0 fallos; Checkstyle y PMD en 0; ArchUnit en verde.
  - `./mvnw verify -Pit`: BUILD SUCCESS (2026-10-19 02:31–02:35 UTC). 269 unitarias y 29 de integración (core 9,
    search 14, ingest 2, web 4), 0 fallos y 0 omitidas.
- **Fecha:** 2026-10-19 02:35 UTC

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

## Resumen de diseño

- Sin clases nuevas en `src/main`. `HybridSearchStrategy` ganó `wrongParts` (T-18) y `SearchQueryValidator`,
  `requireKnnSettings` (T-03, aparte para no pasar la complejidad ciclomática de PMD). `ElasticsearchSearchTransport`
  atrapa `JsonpMappingException` como ya lo hacía `ElasticsearchCatalogIndex.call` en core (T-02).
- `RankedHit` perdió un componente y `TextOperator` su campo; `Rrf` quedó solo con constantes (T-12).
- Pruebas nuevas: `ElasticsearchSearchTransportTest` (3), una en `SearchQueryValidatorTest`, una en
  `KnnStrategiesTest` y una en `ScoreJoinerTest`; search pasó de 62 a 68 unitarias.

## Revisión de convenciones

- [x] ¿Diseño previo? No aplica: correcciones de la revisión 3 sin diseño nuevo; el cambio de T-18 va a
  `docs/diseno/2.md` por las «Notas para el cierre».
- [x] ¿El código coincide con el diseño? Sí, salvo T-18 (anotado para Z).
- [x] ¿`./mvnw verify` con 0 violaciones y arquitectura en verde? Sí (02:31 UTC).
- [x] ¿Una responsabilidad por clase? Sí; no se agregaron clases en `src/main`.
- [x] ¿Solo patrones de la lista cerrada? Sí; no se agregó ninguno.
- [x] ¿Cada error nuevo con código EFX? No hay códigos nuevos; se reutilizan `EFX-ES-001`, `EFX-ES-007` y
  `EFX-SRCH-010`.
- [x] ¿Ningún log con secretos, vectores ni documentos? Sí; el `ERROR` nuevo solo lleva código, alias y conteos.
- [x] ¿Supresiones justificadas? No se agregó ninguna.
- [x] ¿IT de escritura con alias propio? No se tocaron las IT de escritura; las de búsqueda solo leen.
- [x] ¿Un Junior lo entiende? Sí; los cambios son de una o dos líneas con su comentario.

## Notas para el cierre

- **T-12, regla 1:** `RankedHit.rrf()` quedó como método (devuelve `score` en la híbrida y `null` fuera). Si Z
  quiere, puede cambiar `SearchService.hit` para leer `score` según el modo y quitar `rrf()`; no hay otros usos en
  `src/main`.
- **T-12, fuera de la lista:** el parámetro `preference` de `HybridRequestBuilder.build` siempre llega `null` desde
  `HybridSearchStrategy`; solo lo usan las pruebas. Candidato para una revisión siguiente.
- **T-18, `docs/diseno/2.md` › «Errores que no son de validación»:** agregar después de «Un ítem fallido del
  `_msearch` usa el estado de ese ítem (revisión 2, S-20).» la frase: «Un `_msearch` que no devuelve exactamente
  tres partes da `EFX-ES-007` (revisión 3, T-18): Elasticsearch respondió, así que no es `EFX-ES-001`.» Y en
  «Cambios respecto al diseño aprobado», la fila de T-18 con ese mismo cambio.
- **T-12, documentación:** `docs/diseno/2.md` no nombra `Rrf.term`, `TextOperator.elasticsearchOperator()`, el
  componente `rrf` de `RankedHit` ni el `preference` de `VectorRequestBuilder.build` (comprobado con `grep`); no
  hay que cambiarlo por T-12.

## Bloqueos y solicitudes

Vacío.
