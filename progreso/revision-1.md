# Estado de trabajo — Revisión 1

> Correcciones de la revisión de `main` en `dc44535`. Una tarea por cada `R-NN`.

## Identificación

- **Unidad:** Revisión 1
- **Nombre:** Correcciones de la revisión exhaustiva
- **Rama:** `cursor/revision-1-29a6`
- **Carpeta:** `/workspace`
- **Herramienta de la última sesión:** Cursor

## Estado general

en curso

## Punto de reanudación

- **Tarea actual:** R-06
- **Subpaso en curso:** commit y push
- **Próximo paso:** R-07. Los endpoints JSON de la carga responden 413 y 404 en JSON.
- **Última actualización:** 2026-10-18 17:13 UTC

## Tareas

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| R-01 | terminada | `2f7df05` | `./mvnw -pl elastiflix-search,elastiflix-core test` código 0. Checkstyle y PMD de esos módulos en 0. `k=10, tamano=25, pagina=1` devuelve 10; `k=150, tamano=100, pagina=2` devuelve 50; full-text página 400 aceptada y el conteo de páginas se corta en 10 000. |
| R-02 | terminada | `125d049` | `ImportJobTest` 11 pruebas, 0 fallos. Alias en el temporal: sin `deleteIndex(temp)` y paso 5. Alias en el anterior: `EFX-ES-004` y se borra el temporal. `describe` falla o el alias no existe: `EFX-IMP-006` y no se borra nada. Checkstyle y PMD de core e ingest en 0. |
| R-03 | terminada | `f24bb4d` | Variantes CSV: `_id` ≠ `id` (`EFX-ING-010`), fecha `16/07/1999`, `adult=yes` y `genres=Drama` (`EFX-ING-012`), subcampo `extra` en keywords (`EFX-ING-007`). `[REC]` y `007` se conservan. `movies-sample.csv` con 0 errores. Checkstyle y PMD de ingest en 0. |
| R-04 | terminada | `90b96ce` | MockMvc ve las tarjetas, la tabla «Se recibió / El mapping espera», «Archivo JSON», «Importación bloqueada» y el paso 4. `SmokeIT` espera el paso 4. Captura `docs/capturas/cargar.png`. Checkstyle y PMD en 0. |
| R-05 | terminada | `46aeda4` | Tabla de §6 medida sobre 6 899: dark knight 57/5, love story 107/3, Kolejne 3/0, el secreto 35/2, le fabuleux 26/1, heaven 10/0, next 15/0, parasite 1/0. Titanic 36/7/4, Life of Pi 30/7/4, WALL·E 1/1/1. `assumeTrue(count == 6899)`. FullText 8, Vector 4, Hybrid 2, CatalogStatus 1 y Smoke 4, 0 omitidas. |
| R-06 | terminada | se anota en R-07 | MockMvc: títulos Puntuación, Similitud y Puntuación en cada lista; «Mostrando 1–1 de 1 resultados»; chips «Géneros: Drama»; `<select id="page-size">`; fecha invertida con el texto del mockup y el formulario visible. Captura `docs/capturas/resultados.png`. Checkstyle y PMD en 0. |
| R-07 | pendiente | | |
| R-08 | pendiente | | |
| R-09 | pendiente | | |
| R-10 | pendiente | | |
| R-11 | pendiente | | |
| R-12 | pendiente | | |
| R-13 | pendiente | | |
| R-14 | pendiente | | |
| R-15 | pendiente | | |
| R-16 | pendiente | | |
| R-17 | pendiente | | |
| R-18 | pendiente | | |
| R-19 | pendiente | | |
| R-20 | pendiente | | |
| R-21 | pendiente | | |
| R-22 | pendiente | | |
| R-23 | pendiente | | |
| R-24 | pendiente | | |
| R-25 | pendiente | | |
| R-26 | pendiente | | |
| R-27 | pendiente | | |
| R-28 | pendiente | | |
| R-29 | pendiente | | |
| R-30 | pendiente | | |

## Cambios sin commit

Vacío al cerrar R-06.

## Última verificación

- **Comando:** `./mvnw -pl elastiflix-web -am test -Dtest=ResultsPageTest,ErrorPageTest,ErrorCodeTest` y `checkstyle:check pmd:check`
- **Resultado:** BUILD SUCCESS. ResultsPageTest 5, ErrorPageTest 2, ErrorCodeTest 3, 0 fallos. Checkstyle y PMD en 0. La página en vivo de «dark knight» dice «57 resultados» y «Mostrando 1–25 de 57 resultados».
- **Fecha:** 2026-10-18 17:12 UTC

## Decisiones

- Sesión iniciada 2026-10-18 16:23 UTC con Cursor, desde `main` en `dc44535`.
- D1: restaurar valores exactos de §6 con `Assumptions.assumeTrue` (R-05).
- D2: se acepta la ingesta en memoria, quitando vectores al leer y liberando al terminar (R-12).
- D3: «Acerca de» se rotula como medido en el catálogo de 4 322, sin recalcular (R-30).

## Resumen de diseño

Pendiente al cerrar la revisión.

## Revisión de convenciones

Pendiente al cerrar la revisión.

## Bloqueos y solicitudes

Ninguno.
