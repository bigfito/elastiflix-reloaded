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

- **Tarea actual:** cierre del carril
- **Subpaso en curso:** T-12, T-17, T-24 y T-27 terminadas
- **Próximo paso:** `git merge origin/main`, `./mvnw verify` y `./mvnw verify -Pit` en verde, estado
  `terminado`, push y PR «Revisión 3 · carril D (Carga: lectura)» hacia `main`.
- **Última actualización:** 2026-10-19 02:22 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-12 (lectores y validador) | terminada | `be2a064` | Quitados `DocumentChecker.warnVector` y `write` (el salto de `_id` pasó al recorrido de `checkObject`), el parámetro `warnedVectors`, `ValidationRun.warned`, `FieldRules.vector`, `MovieDocuments.IGNORED` y `dropBlankText`, la rama VECTOR de `CsvValues.read` (ya nunca devuelve `null`) y el nulo de `CsvMovieReader.put`. `MappingValidatorTest.shouldAcceptTheJsonFixture` ahora valida la salida real de `JsonDocuments` (sin vectores) y afirma solo la advertencia `EFX-ING-022` de `homepage`; la prueba nueva `shouldNotCopyVectorsNorTheDocumentId` pasa nodos crudos con `_id` y vectores. `./mvnw -q -pl elastiflix-ingest -am verify -o` → EXIT 0 (ingest: 98 pruebas, 0 fallas). Rotura a mano: `NestedValues.write` con `default -> scope.target().set(name, value)` → falla `MappingValidatorTest.shouldNotCopyVectorsNorTheDocumentId` (16 pruebas, 1 falla); revertido. `git grep -F` de `warnVector`, `warnedVectors`, `warned()`, `MovieDocuments.IGNORED`, `dropBlankText` y `FieldKind.VECTOR) {` en `elastiflix-ingest/src/main` → 0 líneas. (`CsvColumnRows.IGNORED` es otra constante, el texto «Se ignora» de la tabla de columnas, y se queda.) |
| T-17 (`document_count`) | terminada | `c230222` | `JsonDocuments.count` rechaza con `EFX-ING-008` un entero `BIG_INTEGER` (no cabe en `long`) antes de llamar a `getLongValue()`. Prueba nueva `JsonDocumentsTest.shouldRejectACountThatDoesNotFitInALong` (`document_count` de 25 dígitos). `./mvnw -q -pl elastiflix-ingest -am test -o -Dtest=JsonDocumentsTest` → 7 pruebas, 0 fallas; `./mvnw -q -pl elastiflix-ingest -am verify -o` → EXIT 0. Rotura a mano: sin la comprobación de `BIG_INTEGER` → falla `shouldRejectACountThatDoesNotFitInALong` con «expected: DOCUMENT_COUNT_MISMATCH but was: UNREADABLE_JSON»; revertido. Las otras partes de T-17 (`existingIds`, orden del informe, vencimiento) son del carril C. |
| T-24 (BOM y nombres de vectores) | terminada | `11ba7c4` | `CsvMovieReader.BOM` se escribe `'\uFEFF'` (antes, el carácter invisible literal); `grep -P '\x{FEFF}'` en `elastiflix-ingest/src/main` → 0. Nombres de vectores: `CsvHeader` usa `VectorFields.isVector` (se quitó su `VECTORS`), `FieldRules` los registra recorriendo `VectorFields.NAMES` (hecho en T-12, `be2a064`) y `MovieDocuments.IGNORED` ya no existe (T-12). En `elastiflix-ingest/src/main` de este carril solo `VectorFields` escribe `plot_256`; el quinto sitio, `IndividualRecord.VECTORS`, es del carril C. `./mvnw -q -pl elastiflix-ingest -am verify -o` → EXIT 0. Roturas a mano: BOM `'\uFEFE'` → falla `CsvMovieReaderTest.shouldMatchTheJsonWhenTheCsvHasAHeader` (el fixture CSV empieza con BOM); `CsvHeader` sin reconocer los vectores → falla la misma prueba (espera `EFX-ING-020`); ambos revertidos. |
| T-27 (`mapping/`) | terminada | se anota en el commit de cierre del carril | `MappingValidatorTest.shouldAcceptTheJsonFixture`: resuelta en T-12 (`be2a064`); ya no pasa nodos crudos con vectores por tres capas, sino la salida de `JsonDocuments`, y la prueba nueva `shouldNotCopyVectorsNorTheDocumentId` cubre el validador con nodos crudos (falla si `NestedValues` copia un vector, comprobado en T-12). `NumberRangeTest`, l. 48: partida en dos líneas (`content` y `file`). `awk 'length>120'` sobre las pruebas de `csv/`, `json/`, `mapping/` y `file/` de ingest → 0 líneas. `./mvnw -q -pl elastiflix-ingest -am test -o -Dtest=NumberRangeTest,MappingValidatorTest` → 11 + 16 pruebas, 0 fallas. Es un cambio de formato: no hay comportamiento que romper. |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw -q -pl elastiflix-ingest -am verify -o`
- **Resultado:** en verde (ingest: 99 pruebas, 0 fallas)
- **Fecha:** 2026-10-19 02:21 UTC

## Decisiones

- Sesión iniciada 2026-10-19 02:10 UTC con Claude Code: carril libre (`git ls-remote --heads origin 'revision-3-*'` vacío); rama creada desde `origin/main` (`6c403f4`); reserva en `4eda3f5`.
- El primer `verify` del carril corrió con los cambios de T-12 a medio escribir (se editó mientras compilaba): core y search en verde; ingest con 1 violación de Checkstyle (`ReturnCount` en `DocumentChecker.copy`), que se corrigió dentro de T-12. No se vio ninguna falla de `main` en archivos de otros carriles.
- T-12: un vector que llegue al validador (solo si alguien llama a `MappingValidator` sin pasar por los lectores) se descarta en silencio: `FieldRules` lo sigue conociendo como `VECTOR` y `NestedValues` no lo copia. Se descartó tratarlo como `EFX-ING-007` (campo desconocido), porque el campo sí está en el mapping.

## Notas para el cierre

<Cambios de documentación que aplicará el carril Z: filas de PLAN §1 (texto exacto), propiedades de §7,
«Cambios respecto al diseño aprobado» de `docs/diseno/<fase>.md`, tablas de errores y README. También las
firmas viejas que la regla 1 obligó a conservar y que Z puede quitar.>

## Bloqueos y solicitudes

Vacío.
