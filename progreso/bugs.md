# Estado de trabajo — Bugs de la revisión de documentación

> Cinco correcciones pedidas después de fusionar el PR #11 en `main` (`2f6f9ed`).

## Identificación

- **Unidad:** bugs
- **Nombre:** Corrección de los cinco posibles bugs
- **Rama:** `cursor/bugs-revision-29a6`
- **Carpeta:** `/workspace`
- **Herramienta de la última sesión:** Cursor

## Estado general

en curso

## Punto de reanudación

- **Tarea actual:** B-02
- **Subpaso en curso:** cerrada
- **Próximo paso:** B-03, reemplazar `HttpStatus.PAYLOAD_TOO_LARGE` por `CONTENT_TOO_LARGE`.
- **Última actualización:** 2026-10-18 19:00 UTC

## Tareas

> El hash de una tarea se completa en el commit de la tarea siguiente. La fila que se cierra puede decir «se anota en …» hasta ese commit.

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| B-01 | terminada | `f8d3a36` | `./mvnw -pl elastiflix-core test -Dtest=CatalogBootstrapTest checkstyle:check pmd:check` BUILD SUCCESS, 2026-10-18 18:59 UTC. 9 pruebas, 0 fallos. Checkstyle y PMD en 0. Alias en el índice nuevo: publicado, sin borrar. Índice concreto: borra el nuevo y `EFX-ES-004`. Ilegible o ausente: deja el nuevo y `EFX-IMP-006`. |
| B-02 | terminada | se anota en B-03 | `./mvnw -pl elastiflix-core test -Dtest=InferenceEmbeddingServiceTest,ErrorCodeTest,VectorProjectorTest checkstyle:check pmd:check` BUILD SUCCESS, 2026-10-18 19:00 UTC. 8 pruebas, 0 fallos. Un lote con menos vectores que textos es `EFX-EMB-004`. La dimensión distinta sigue en `EFX-EMB-002`. |
| B-03 | pendiente | | `HttpStatus.PAYLOAD_TOO_LARGE` obsoleto. |
| B-04 | pendiente | | `Thread.sleep` en `ReindexTasks`. |
| B-05 | pendiente | | Prefijo del mockup `About.dc.html`. |

## Cambios sin commit

Vacío al cerrar B-01.

## Última verificación

- **Comando:** `./mvnw -pl elastiflix-core test -Dtest=InferenceEmbeddingServiceTest,ErrorCodeTest,VectorProjectorTest checkstyle:check pmd:check`
- **Resultado:** BUILD SUCCESS. 8 pruebas, 0 fallos. Checkstyle y PMD en 0.
- **Fecha:** 2026-10-18 19:00 UTC

## Decisiones

- Sesión iniciada 2026-10-18 18:55 UTC con Cursor, desde `main` en `2f6f9ed`. La rama remota `cursor/docs-sync-29a6` se borró.
- B-01: se reutiliza `EFX-IMP-006`. El texto ya dice que el índice nuevo se deja y que hay que revisar el alias. No hace falta un código nuevo. La migración lanza `ElasticsearchAccessException` con ese código, el mismo tipo que el resto de sus fallas.
- B-02: un código propio, `EFX-EMB-004`. Un solo texto no describía bien «dimensión distinta» y «el lote no trajo un vector por texto».

## Resumen de diseño

Se completa al cerrar B-05.

## Revisión de convenciones

Se completa al cerrar B-05.

## Bloqueos y solicitudes

Vacío.
