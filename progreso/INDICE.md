# 📍 Índice de estado del plan

> Punto de entrada para retomar el trabajo. Lo mantiene el agente que ejecuta `PLAN.md`.
> Actualízalo **al terminar cada tarea** (y al empezar y cerrar cada fase), en el mismo commit que el
> archivo de la fase, y haz push.
> Reglas completas: `PLAN.md` §9 "Estado del trabajo y reanudación".

## Punto de reanudación

- **Fase actual:** 3
- **Estado de la fase:** en curso
- **Herramienta prevista:** Cursor (rama `cursor/fase-3-diseno-29a6`, PR #4)
- **Próximo paso:** Tarea 3.3, `CsvMovieReader`.
- **Última actualización:** 2026-10-18 02:14 UTC

## Fases

| Fase | Nombre | Compuerta | Estado | Archivo de estado | Verificación de fase |
|---|---|---|---|---|---|
| 0 | Andamiaje y calidad | — | terminado | `progreso/0.md` | 2026-10-17 en verde |
| 1 | Núcleo: conexión, catálogo y embeddings | 🔒 | terminado | `progreso/1.md` | 2026-10-18 en verde |
| 2 | Búsqueda | 🔒 | terminado | `progreso/2.md` | 2026-10-18 en verde |
| 3 | Carga de datos | 🔒 | en curso | `progreso/3.md` | — |
| 4 | Interfaz web | — | no iniciado | `progreso/4.md` | — |
| 5 | Pruebas integrales y smoke test | — | no iniciado | `progreso/5.md` | — |
| 6 | Documentación y cierre | — | no iniciado | `progreso/6.md` | — |

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

## Pendiente para la Fase 4

- Corregir en el mockup `design/mockups/Ingest.dc.html` el texto «Los vectores del archivo se usan tal cual». Los vectores siempre se generan con el servicio de inferencia; si el archivo los trae, se ignoran con advertencia.

## Última verificación

- **Comando:** `./mvnw -pl elastiflix-ingest,elastiflix-web -am verify`
- **Resultado:** BUILD SUCCESS. `FileCheckerTest` 8, `MultipartLimitsTest` 1, Checkstyle 0, PMD en verde. No se usó el perfil `it` ni el catálogo real.
- **Fecha:** 2026-10-18 02:10 UTC
