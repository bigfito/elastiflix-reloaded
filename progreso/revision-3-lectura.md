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

- **Tarea actual:** T-17 (`document_count`)
- **Subpaso en curso:** T-12 terminada; empieza T-17
- **Próximo paso:** en `JsonDocuments.count`, rechazar con `EFX-ING-008` un `document_count` que no cabe en
  `long` (hoy `getLongValue()` lanza y sale `EFX-ING-013`), y agregar su prueba en `JsonDocumentsTest`.
- **Última actualización:** 2026-10-19 02:16 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-12 (lectores y validador) | terminada | se anota en el commit de T-17 | Quitados `DocumentChecker.warnVector` y `write` (el salto de `_id` pasó al recorrido de `checkObject`), el parámetro `warnedVectors`, `ValidationRun.warned`, `FieldRules.vector`, `MovieDocuments.IGNORED` y `dropBlankText`, la rama VECTOR de `CsvValues.read` (ya nunca devuelve `null`) y el nulo de `CsvMovieReader.put`. `MappingValidatorTest.shouldAcceptTheJsonFixture` ahora valida la salida real de `JsonDocuments` (sin vectores) y afirma solo la advertencia `EFX-ING-022` de `homepage`; la prueba nueva `shouldNotCopyVectorsNorTheDocumentId` pasa nodos crudos con `_id` y vectores. `./mvnw -q -pl elastiflix-ingest -am verify -o` → EXIT 0 (ingest: 98 pruebas, 0 fallas). Rotura a mano: `NestedValues.write` con `default -> scope.target().set(name, value)` → falla `MappingValidatorTest.shouldNotCopyVectorsNorTheDocumentId` (16 pruebas, 1 falla); revertido. `git grep -F` de `warnVector`, `warnedVectors`, `warned()`, `MovieDocuments.IGNORED`, `dropBlankText` y `FieldKind.VECTOR) {` en `elastiflix-ingest/src/main` → 0 líneas. (`CsvColumnRows.IGNORED` es otra constante, el texto «Se ignora» de la tabla de columnas, y se queda.) |
| T-17 (`document_count`) | pendiente | — | Detalles de la validación de archivos |
| T-24 (BOM y nombres de vectores) | pendiente | — | Duplicados y costo por sondeo en la carga |
| T-27 (`mapping/`) | pendiente | — | Pruebas que no pueden fallar, débiles o con higiene pendiente |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw -q -pl elastiflix-ingest -am verify -o`
- **Resultado:** en verde (ingest: 98 pruebas, 0 fallas)
- **Fecha:** 2026-10-19 02:15 UTC

## Decisiones

- Sesión iniciada 2026-10-19 02:10 UTC con Claude Code: carril libre (`git ls-remote --heads origin 'revision-3-*'` vacío); rama creada desde `origin/main` (`6c403f4`); reserva en `4eda3f5`.
- El primer `verify` del carril corrió con los cambios de T-12 a medio escribir (se editó mientras compilaba): core y search en verde; ingest con 1 violación de Checkstyle (`ReturnCount` en `DocumentChecker.copy`), que se corrigió dentro de T-12. No se vio ninguna falla de `main` en archivos de otros carriles.
- T-12: un vector que llegue al validador (solo si alguien llama a `MappingValidator` sin pasar por los lectores) se descarta en silencio: `FieldRules` lo sigue conociendo como `VECTOR` y `NestedValues` no lo copia. Se descartó tratarlo como `EFX-ING-010` (campo desconocido), porque el campo sí está en el mapping.

## Notas para el cierre

<Cambios de documentación que aplicará el carril Z: filas de PLAN §1 (texto exacto), propiedades de §7,
«Cambios respecto al diseño aprobado» de `docs/diseno/<fase>.md`, tablas de errores y README. También las
firmas viejas que la regla 1 obligó a conservar y que Z puede quitar.>

## Bloqueos y solicitudes

Vacío.
