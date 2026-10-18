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

- **Tarea actual:** B-01
- **Subpaso en curso:** pruebas de `CatalogBootstrapTest`
- **Próximo paso:** B-02, separar el lote de inferencia incompleto de `EFX-EMB-002`.
- **Última actualización:** 2026-10-18 19:05 UTC

## Tareas

> El hash de una tarea se completa en el commit de la tarea siguiente. La fila que se cierra puede decir «se anota en …» hasta ese commit.

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| B-01 | terminada | se anota en B-02 | `./mvnw -pl elastiflix-core test -Dtest=CatalogBootstrapTest checkstyle:check pmd:check` BUILD SUCCESS, 2026-10-18 18:59 UTC. 9 pruebas, 0 fallos. Checkstyle y PMD en 0. Alias en el índice nuevo: publicado, sin borrar. Índice concreto: borra el nuevo y `EFX-ES-004`. Ilegible o ausente: deja el nuevo y `EFX-IMP-006`. |
| B-02 | pendiente | | Lote con menos vectores que textos. |
| B-03 | pendiente | | `HttpStatus.PAYLOAD_TOO_LARGE` obsoleto. |
| B-04 | pendiente | | `Thread.sleep` en `ReindexTasks`. |
| B-05 | pendiente | | Prefijo del mockup `About.dc.html`. |

## Cambios sin commit

Vacío al cerrar B-01.

## Última verificación

- **Comando:** `./mvnw -pl elastiflix-core test -Dtest=CatalogBootstrapTest checkstyle:check pmd:check`
- **Resultado:** BUILD SUCCESS. 9 pruebas, 0 fallos. Checkstyle y PMD en 0.
- **Fecha:** 2026-10-18 18:59 UTC

## Decisiones

- Sesión iniciada 2026-10-18 18:55 UTC con Cursor, desde `main` en `2f6f9ed`. La rama remota `cursor/docs-sync-29a6` se borró.
- B-01: se reutiliza `EFX-IMP-006`. El texto ya dice que el índice nuevo se deja y que hay que revisar el alias. No hace falta un código nuevo. La migración lanza `ElasticsearchAccessException` con ese código, el mismo tipo que el resto de sus fallas.

## Resumen de diseño

Se completa al cerrar B-05.

## Revisión de convenciones

Se completa al cerrar B-05.

## Bloqueos y solicitudes

Vacío.
