# Estado de trabajo — Revisión 3 · Carril C (Carga: trabajos)

> Carril C de la revisión 3. La especificación de cada corrección está en
> [`docs/revisiones/revision-3.md`](../docs/revisiones/revision-3.md); las reglas de los carriles, en su
> sección «Cómo trabajar en paralelo». Este archivo solo lo escribe el agente de este carril.

## Identificación

- **Unidad:** Revisión 3 · carril C
- **Nombre:** Carga: trabajos
- **Rama:** `revision-3-trabajos` (creada desde `origin/main` en `6c403f4`)
- **Carpeta:** worktree `/workspaces/elastiflix-trabajos`
- **Archivos del carril:** paquetes `job/`, `service/` y `vector/` de ingest y `TestApplication.java`, salvo `ErrorContextTest.java`
- **Herramienta de la última sesión:** Claude Code (Opus 5.5)

## Estado general

en curso

## Punto de reanudación

- **Tarea actual:** T-09
- **Subpaso en curso:** ninguno
- **Próximo paso:** T-09: `FileIntake` indica el paso del rechazo (2 para `EFX-ING-001` a `004`, 3 en los demás) y `describe()` pasa el trabajo al paso 3.
- **Última actualización:** 2026-10-19 02:15 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-08 | terminada | sobre `1801d02` | `JobFigures.checks` recibe el estado: solo hay comprobaciones en `valido`, `importando` y `publicado`. `./mvnw -q -pl elastiflix-ingest -am test -Dtest=JobFiguresTest` → 8 pruebas, 0 fallos; con el filtro de estado quitado a mano fallan `shouldShowNoChecksWhenTheJobIsStillValidating` y `shouldShowNoChecksWhenTheJobFailed` (revertido). Checkstyle y PMD de ingest en 0 (`verify -DskipTests`). |
| T-09 | pendiente | — | Un rechazo de archivo detiene el paso equivocado |
| T-10 (`ImportJob`, `ImportRunner`, `JobStore`) | pendiente | — | Faltan pruebas unitarias de ramas importantes |
| T-12 (`ImportCounts`, `ImportProgress`) | pendiente | — | Código muerto y campos que nadie lee |
| T-16 | pendiente | — | Hilos de la carga: estado final, excepciones y nombres |
| T-17 (`FileIntake` y `JobStore`) | pendiente | — | Detalles de la validación de archivos |
| T-19 (Javadoc y `JobStatus`) | pendiente | — | Textos de error y Javadoc que no corresponden al comportamiento |
| T-24 (`job/`) | pendiente | — | Duplicados y costo por sondeo en la carga |
| T-27 (`job/`) | pendiente | — | Pruebas que no pueden fallar, débiles o con higiene pendiente |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw -pl elastiflix-ingest -am verify` (y al final `./mvnw verify` completo)
- **Resultado:** —
- **Fecha:** —

## Decisiones

- 2026-10-19 02:10 UTC: carril C tomado con Claude Code (Opus 5.5) en el worktree
  `/workspaces/elastiflix-trabajos`, rama `revision-3-trabajos` desde `origin/main` (`6c403f4`).

## Notas para el cierre

Vacío por ahora.

## Bloqueos y solicitudes

Vacío.
