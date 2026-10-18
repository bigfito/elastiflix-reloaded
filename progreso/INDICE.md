# 📍 Índice de estado del plan

> Punto de entrada para retomar el trabajo. Lo mantiene el agente que ejecuta `PLAN.md`.
> Actualízalo **al terminar cada tarea** (y al empezar y cerrar cada fase), en el mismo commit que el
> archivo de la fase, y haz push.
> Reglas completas: `PLAN.md` §9 "Estado del trabajo y reanudación".

## Punto de reanudación

- **Fase actual:** Revisión 1
- **Estado de la fase:** en curso
- **Rama:** `cursor/revision-1-29a6`
- **Próximo paso:** R-30, coherencia de la documentación con el catálogo de 6 899
- **Última actualización:** 2026-10-18 18:19 UTC

## Fases

| Fase | Nombre | Compuerta | Estado | Archivo de estado | Verificación de fase |
|---|---|---|---|---|---|
| 0 | Andamiaje y calidad | — | terminado | `progreso/0.md` | 2026-10-17 en verde |
| 1 | Núcleo: conexión, catálogo y embeddings | 🔒 | terminado | `progreso/1.md` | 2026-10-18 en verde |
| 2 | Búsqueda | 🔒 | terminado | `progreso/2.md` | 2026-10-18 en verde |
| 3 | Carga de datos | 🔒 | terminado | `progreso/3.md` | 2026-10-18 en verde |
| 4 | Interfaz web | — | terminado | `progreso/4.md` | 2026-10-18 en verde |
| 5 | Pruebas integrales y smoke test | — | terminado | `progreso/5.md` | 2026-10-18 en verde |
| 6 | Documentación y cierre | — | terminado | `progreso/6.md` | 2026-10-18 en verde |
| — | Revisión 1 | — | en curso | `progreso/revision-1.md` | pendiente |

## Aprobaciones del usuario

| Fecha UTC | Qué se aprobó | Comentarios |
|---|---|---|
| 2026-10-17 | Decisiones sobre el plan (no es aún el visto bueno para programar) | Paquete `com.bigfito.elastiflix`; migración a alias sin aprobación previa; rutas en español; híbrida con `_msearch`; pruebas de integración contra Serverless; sin autenticación; borrar el índice anterior al publicar; registro individual directo al catálogo; CSV sin encabezado se rechaza; vectores siempre generados con inferencia; pósters de TMDB; commit + push por tarea; documentación al cerrar cada fase; estado actualizado en cada tarea; un solo usuario a la vez, sin controles de concurrencia; programa un agente de Cursor; vectores de documento con la combinación calibrada en 1.9 (los archivos no traen vectores y el formulario no los captura); atribución de TMDB en el pie y en «Acerca de»; sin restricción por costo de inferencia |
| 2026-10-17 | Visto bueno del plan e inicio de la Fase 0 | Adolfo (bigfito) pidió explícitamente ejecutar la Fase 0. El agente de Cursor arranca por la tarea 0.1. |
| 2026-10-17 | Fase 0 aprobada y fusionada en `main` | Adolfo revisó y aprobó la Fase 0. El PR #1 quedó fusionado en `main` (merge commit `275478f`). |
| 2026-10-18 | Diseño de la Fase 1 y sus cinco supuestos | Adolfo aprobó `docs/diseno/1.md` tal cual: lotes de 16, no crear catálogo vacío, `belongs_to_collection` como objeto, códigos de otras fases más tarde y tabla de calibración vacía hasta la tarea 1.9. |
| 2026-10-18 | Fase 1 aprobada y fusionada en `main` | Adolfo revisó y aprobó la Fase 1. El PR #2 quedó fusionado en `main` (merge commit `b85555b`). |
| 2026-10-18 | Diseño de la Fase 2 y sus seis supuestos | Adolfo aprobó `docs/diseno/2.md` tal cual: la búsqueda habla con Elasticsearch solo por `SearchTransport`; vectorial e híbrida piden `k` y parten la página en la aplicación; el estado vacío no cuenta el catálogo entero; una página fuera del total pero dentro de la ventana no es un error; las etiquetas «Todos», «Solo no adultos» y «Ver todos» llegan en la Fase 4; «fuera de su top» usa el `k` real. |
| 2026-10-18 | Fila full-text `Kolejne 365 dni` | Adolfo aceptó OR 1 · AND 0 como resultado esperado del `match` sobre `title`. El token `365` coincide con «The Next 365 Days» (`829560`). |
| 2026-10-18 | Fase 2 aprobada y fusionada en `main` | Adolfo revisó y aprobó la Fase 2. El PR #3 quedó fusionado en `main` (merge commit `6d98bb9`). |
| 2026-10-18 | Diseño de la Fase 3 y sus supuestos | Adolfo aprobó `docs/diseno/3.md` tal cual. Incluye borrar el índice anterior al publicar, el registro individual directo al alias, sin controles de concurrencia, rechazar un CSV sin encabezado y generar siempre los vectores con inferencia. |
| 2026-10-18 | Fase 3 aprobada y fusionada en `main` | Adolfo revisó y aprobó la Fase 3. El PR #4 quedó fusionado en `main` (merge commit `05ad8f8`). |
| 2026-10-18 | Fase 4 aprobada y fusionada en `main` | Adolfo revisó y aprobó la Fase 4, incluida la interfaz de las capturas. El PR #5 quedó fusionado en `main` (merge commit `c64aa8a`). |
| 2026-10-18 | Fase 5 aprobada y fusionada en `main` | Adolfo revisó y aprobó la Fase 5. El PR #6 quedó fusionado en `main` (merge commit `68c1a24`). |
| 2026-10-18 | Fase 6 aprobada y fusionada en `main` | Adolfo revisó y aprobó la Fase 6. El PR #7 quedó fusionado en `main` (merge commit `eb309d9`). |
| 2026-10-18 | Corrección de documentación fusionada en `main` | Adolfo aprobó el PR #8. Quedó fusionado en `main` (merge commit `22dac52`). Después se borraron las ramas remotas `cursor/*` de las fases 0 a 6. |
| 2026-10-18 | Criterios de §6 relajados, fusionados en `main` | El PR #9 (`cursor/revision-docs-29a6`, merge `dc44535`, commit `2a3aaea`) dejó de afirmar los totales de 4 322 y lee `_count`. La revisión 1 (R-05, D1) restituye los valores exactos sobre 6 899 películas. |
| pendiente | Capturas de R-21 | Sin confirmar. No es una aprobación. Las cinco imágenes están en `docs/capturas/`. |
| 2026-10-18 | Importación de `movies.json` al catálogo real | Adolfo eligió reemplazar el índice de `elastiflix-reloaded`. Arranque 05:44:29 UTC y publicación 05:49:22 UTC (unos 4 min 54 s). Arreglo JSON de 6899 registros, validación `valido`, 0 errores y 5055 advertencias (`EFX-ING-022`: 4960, `EFX-ING-021`: 95). Índice nuevo `elastiflix-reloaded-20261018-054429` con 6899 documentos. Se borró `elastiflix-reloaded-20261018-003535`. Una lectura posterior contó `title`, `plot`, `plot_256`, `plot_512` y `plot_1024` en 6899/6899. |

## Catálogo vigente

- **Alias:** `elastiflix-reloaded`
- **Índice:** `elastiflix-reloaded-20261018-054429`
- **Documentos:** 6899, con `title`, `plot` y los tres vectores en todos
- **Índice anterior:** `elastiflix-reloaded-20261018-003535` (4322 documentos), borrado al publicar
- **Pruebas:** `SmokeIT`, `CatalogStatusServiceIT`, `FullTextSearchIT`, `VectorSearchIT` y `HybridSearchIT` afirman la tabla de §6 (6 899 películas) y se omiten si `_count` cambió. No escriben en este alias.

## Última verificación

- **Comando:** `./mvnw verify -Pit`
- **Resultado:** BUILD SUCCESS a las 06:20:36 UTC (1 min 4 s). 168 pruebas, 0 fallos: 141 unitarias y 27 de integración. Checkstyle 0. `SmokeIT` 4/4. La escritura usó `elastiflix-it-021b536192a8` y lo borró. El alias real siguió en `elastiflix-reloaded-20261018-054429` con 6899 documentos.
- **Fecha:** 2026-10-18 06:20 UTC. La corrida anterior, de las 04:52 UTC, fue sobre `elastiflix-reloaded-20261018-003535` con 4322 documentos, antes de la importación de las 05:44 UTC.
