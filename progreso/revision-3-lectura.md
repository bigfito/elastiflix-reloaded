# Estado de trabajo — Revisión 3 · Carril D (Carga: lectura)

> Carril D de la revisión 3. La especificación de cada corrección está en
> [`docs/revisiones/revision-3.md`](../docs/revisiones/revision-3.md); las reglas de los carriles, en su
> sección «Cómo trabajar en paralelo». Este archivo solo lo escribe el agente de este carril.

## Identificación

- **Unidad:** Revisión 3 · carril D
- **Nombre:** Carga: lectura
- **Rama:** `revision-3-lectura` (creada desde `origin/main` en `6c403f4`)
- **Carpeta:** worktree `/workspaces/elastiflix-lectura`
- **Archivos del carril:** paquetes `csv/`, `json/`, `mapping/`, `movie/` y `file/` de ingest y `src/test/resources/**` de ingest
- **Herramienta de la última sesión:** Claude Code (Claude Opus 5.5)

## Estado general

en curso

## Punto de reanudación

- **Tarea actual:** T-12 (lectores y validador)
- **Subpaso en curso:** carril reservado; primer `./mvnw -pl elastiflix-ingest -am verify`
- **Próximo paso:** corre `./mvnw -q -pl elastiflix-ingest -am verify` en `/workspaces/elastiflix-lectura` y
  empieza T-12: quita `DocumentChecker.warnVector` y `write`, `ValidationRun.warned`, el parámetro
  `warnedVectors`, `MovieDocuments.IGNORED`, `dropBlankText`, la rama VECTOR de `CsvValues` y el nulo de
  `CsvMovieReader.put`.
- **Última actualización:** 2026-10-19 02:10 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-12 (lectores y validador) | pendiente | — | Código muerto y campos que nadie lee |
| T-17 (`document_count`) | pendiente | — | Detalles de la validación de archivos |
| T-24 (BOM y nombres de vectores) | pendiente | — | Duplicados y costo por sondeo en la carga |
| T-27 (`mapping/`) | pendiente | — | Pruebas que no pueden fallar, débiles o con higiene pendiente |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw -pl elastiflix-ingest -am verify` (y al final `./mvnw verify` completo)
- **Resultado:** —
- **Fecha:** —

## Decisiones

- Sesión iniciada 2026-10-19 02:10 UTC con Claude Code: carril libre (`git ls-remote --heads origin 'revision-3-*'` vacío); rama creada desde `origin/main` (`6c403f4`).

## Notas para el cierre

<Cambios de documentación que aplicará el carril Z: filas de PLAN §1 (texto exacto), propiedades de §7,
«Cambios respecto al diseño aprobado» de `docs/diseno/<fase>.md`, tablas de errores y README. También las
firmas viejas que la regla 1 obligó a conservar y que Z puede quitar.>

## Bloqueos y solicitudes

Vacío.
