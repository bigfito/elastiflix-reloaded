# Estado de trabajo — Revisión 4

> Archivo de estado para retomar el trabajo en otra sesión sin perder avance. Especificación:
> [`docs/revisiones/revision-4.md`](../docs/revisiones/revision-4.md). Reglas en `PLAN.md` §9.

## Identificación

- **Unidad:** Revisión 4
- **Nombre:** Correcciones de la cuarta revisión (tras las fases 7 y 8)
- **Rama:** `revision-4`
- **Carpeta:** `E:\02 - programming\03 - elastiflix-reloaded`
- **Herramienta de la última sesión:** Claude Code (Opus 5.5)

## Estado general

en curso

## Punto de reanudación

- **Tarea actual:** U-01 y U-24 (medir de nuevo §6 y las sugerencias)
- **Subpaso en curso:** preparar la medición contra el catálogo vigente
- **Próximo paso:** medir con `k = 100` y `num_candidates = 200` las posiciones de §6 y de las sugerencias de
  Inicio en los dos idiomas, y actualizar textos, IT y documentos
- **Última actualización:** 2026-10-21 17:55 UTC

## Tareas

> El hash de una tarea se completa en el commit de la tarea siguiente.

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| U-11 | terminada | `a1b8fa8` | `./mvnw verify` en verde (386 unitarias); `AppliedFiltersTest` en español e inglés |
| U-03 | terminada | `ec6ca83` | `./mvnw verify` en verde; `ExternalLinksTest` (16 casos) y `MovieDetailPageTest` |
| U-01 | pendiente | | |
| U-24 | pendiente | | |
| U-07 | pendiente | | |
| U-16 | pendiente | | |
| U-13, U-17 | pendiente | | |
| U-14, U-15 | pendiente | | |
| U-02 | pendiente | | |
| U-33 (cabeceras) | descartada | — | Adolfo decidió no agregar cabeceras de seguridad (2026-10-21) |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw verify`
- **Resultado:** en verde: 386 unitarias (core 68, search 71, ingest 123, web 124), Checkstyle 0, PMD 0
- **Fecha:** 2026-10-21 17:40 UTC

## Decisiones

- 2026-10-21: U-11 y U-03 se aplicaron directo en `main` a pedido de Adolfo («Sí, corrige U-11 … y U-03 …»
  y «Sí, haz commit y push en main …»).
- 2026-10-21: Adolfo respondió las decisiones de la lista:
  1. U-01: volver a medir la tabla vectorial de §6 («Si, hazlo»);
  2. U-07: el agente redacta el texto de `EFX-SRCH-012` según su objetivo («Genera uno acorde al objetivo»);
  3. U-13, U-16 y U-17: corrección de fondo («Corrígela de fondo»);
  4. U-02: instalar Playwright y Chromium y correr las pruebas de navegador («te doy vía libre»);
  5. U-24: volver a medir las sugerencias en español («Sí, vuelve a medirlas»);
  6. U-33: sin cabeceras de seguridad («No, no hace falta»).
- 2026-10-21: el resto de la revisión 4 va en la rama `revision-4`, con commit y push por tarea, y se integra
  con un PR. U-14 y U-15 entran con la decisión 3, porque son la misma traducción de «Cargar datos» y la
  prueba que la vigila. Los demás puntos esperan instrucciones de Adolfo.

## Resumen de diseño

Se completa al cerrar.

## Revisión de convenciones

Se completa al cerrar.

## Bloqueos y solicitudes

Vacío.
