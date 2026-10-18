# Estado de trabajo — Bugs de la revisión de documentación

> Cinco correcciones pedidas después de fusionar el PR #11 en `main` (`2f6f9ed`).

## Identificación

- **Unidad:** bugs
- **Nombre:** Corrección de los cinco posibles bugs
- **Rama:** `cursor/bugs-revision-29a6`
- **Carpeta:** `/workspace`
- **Herramienta de la última sesión:** Cursor

## Estado general

terminado — el PR #12 se fusionó en `main` (`41c1188`)

## Punto de reanudación

- **Tarea actual:** B-05
- **Subpaso en curso:** cerrada
- **Próximo paso:** nada pendiente en esta unidad. El PR #12 se fusionó en `main` (`41c1188`) y la rama remota
  `cursor/bugs-revision-29a6` se borró. La revisión 2 (`progreso/revision-2.md`) revisó estas correcciones;
  B-04 continúa en S-01.
- **Última actualización:** 2026-10-18 19:42 UTC

## Tareas

> El hash de una tarea se completa en el commit de la tarea siguiente. La fila que se cierra puede decir «se anota en …» hasta ese commit.

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| B-01 | terminada | `f8d3a36` | `./mvnw -pl elastiflix-core test -Dtest=CatalogBootstrapTest checkstyle:check pmd:check` BUILD SUCCESS, 2026-10-18 18:59 UTC. 9 pruebas, 0 fallos. Checkstyle y PMD en 0. Alias en el índice nuevo: publicado, sin borrar. Índice concreto: borra el nuevo y `EFX-ES-004`. Ilegible o ausente: deja el nuevo y `EFX-IMP-006`. |
| B-02 | terminada | `19cfd70` | `./mvnw -pl elastiflix-core test -Dtest=InferenceEmbeddingServiceTest,ErrorCodeTest,VectorProjectorTest checkstyle:check pmd:check` BUILD SUCCESS, 2026-10-18 19:00 UTC. 8 pruebas, 0 fallos. Un lote con menos vectores que textos es `EFX-EMB-004`. La dimensión distinta sigue en `EFX-EMB-002`. |
| B-03 | terminada | `3929d4c` | `./mvnw -pl elastiflix-web -am test -Dtest=IngestJobErrorTest -Dsurefire.failIfNoSpecifiedTests=false checkstyle:check pmd:check` BUILD SUCCESS, 2026-10-18 19:01 UTC. 3 pruebas, 0 fallos. Sigue respondiendo 413 con `EFX-ING-003`. Checkstyle y PMD en 0. La compilación ya no avisa de `PAYLOAD_TOO_LARGE`. |
| B-04 | terminada | `54ccf60` | `./mvnw -pl elastiflix-core test -Dtest=ReindexTasksTest checkstyle:check pmd:check` BUILD SUCCESS, 2026-10-18 19:02 UTC. 4 pruebas, 0 fallos. Una lectura de `_tasks` con `wait_for_completion` y tope `30m`. No hay `Thread.sleep`. Checkstyle y PMD en 0. |
| B-05 | terminada | `8cbfa49` | Dos frases de `design/mockups/About.dc.html`: las consultas siguen con `Query: `; las sinopsis dicen `input_type` `ingest` y sin prefijo. Los estilos en línea no cambian. `./mvnw verify` BUILD SUCCESS, 2026-10-18 19:05 UTC, 23 s, 193 unitarias, 0 fallos. `./mvnw verify -Pit` BUILD SUCCESS, 2026-10-18 19:07 UTC, 1 min 8 s, 193 unitarias y 27 de integración, 0 fallos y 0 omitidas. Checkstyle y PMD en 0. |

## Cambios sin commit

Vacío al cerrar B-05.

## Última verificación

- **Comando:** `./mvnw verify` y `./mvnw verify -Pit`
- **Resultado:** Los dos en BUILD SUCCESS. `verify` (2026-10-18 19:05 UTC, 23 s): 193 unitarias, 0 fallos, Checkstyle y PMD en 0. `verify -Pit` (2026-10-18 19:07 UTC, 1 min 8 s): 193 unitarias y 27 de integración, 0 fallos y 0 omitidas, Checkstyle y PMD en 0. Después, el alias `elastiflix-reloaded` seguía en `elastiflix-reloaded-20261018-054429` con 6899 documentos.
- **Fecha:** 2026-10-18 19:07 UTC

## Decisiones

- Sesión iniciada 2026-10-18 18:55 UTC con Cursor, desde `main` en `2f6f9ed`. La rama remota `cursor/docs-sync-29a6` se borró.
- B-01: se reutiliza `EFX-IMP-006`. El texto ya dice que el índice nuevo se deja y que hay que revisar el alias. No hace falta un código nuevo. La migración lanza `ElasticsearchAccessException` con ese código, el mismo tipo que el resto de sus fallas.
- B-02: un código propio, `EFX-EMB-004`. Un solo texto no describía bien «dimensión distinta» y «el lote no trajo un vector por texto».
- B-03: en Spring Framework 7.0 (Boot 4.1.1) `PAYLOAD_TOO_LARGE` está obsoleto. El reemplazo es `CONTENT_TOO_LARGE`, el mismo 413.
- B-04: la copia sigue pidiendo `wait_for_completion: false` en `_reindex`. La espera pasa a una sola lectura de `_tasks` con `wait_for_completion: true` y tope `30m`, el mismo margen que las 3600 pausas de medio segundo. Las pruebas no duermen.
- B-05: el mockup aprobado cambia solo el texto del prefijo. Las tablas de calibración siguen listando `Document: ` como combinación medida.

## Resumen de diseño

- B-01: si `publish` falla, se vuelve a leer el alias. Alias en el índice nuevo: publicado. Índice concreto: se borra el nuevo y `EFX-ES-004`. Ilegible o ausente: se conserva el nuevo y `EFX-IMP-006`.
- B-02: `EFX-EMB-004` cuando el lote no trae un vector por texto. `EFX-EMB-002` sigue para dimensión, nulos, norma cero y forma inesperada.
- B-03: `HttpStatus.CONTENT_TOO_LARGE` responde 413.
- B-04: una lectura de `_tasks` con espera y tope `30m`. Sin `Thread.sleep`.
- B-05: `About.dc.html` dice lo mismo que `about.html` sobre el prefijo, con los estilos que ya tenía.

## Revisión de convenciones

- El diseño vive en `docs/diseno/1.md` y `docs/diseno/4.md`, ya aprobados. Estas correcciones actualizan esos documentos en el mismo commit que el código. No hay compuerta nueva.
- El código coincide con ese diseño: criterio de `describe`, `EFX-EMB-004`, `CONTENT_TOO_LARGE` y la espera en `_tasks`.
- `./mvnw verify` terminó con 0 violaciones. ArchUnit está en las 193 unitarias.
- Cada clase tocada sigue con una responsabilidad: publicar o rechazar el alias, exigir un vector por texto, traducir el archivo grande a 413, esperar la copia.
- No se agregó un patrón nuevo. La espera es una llamada del cliente que ya existía.
- El código nuevo es `EFX-EMB-004`, con qué pasó, causa probable y acción sugerida. B-01 reutiliza `EFX-IMP-006`.
- Los logs no incluyen secretos, vectores ni documentos. La copia registra origen, destino e id de tarea.
- No hay supresión de Checkstyle ni de PMD. `ReindexTasksTest.assertWait` avisa `unchecked` porque Mockito no conserva el genérico; el comentario está en el método y la nota en `docs/diseno/1.md`.
- Esta corrida no escribe en `elastiflix-reloaded`. Las de integración que escriben siguen con `elastiflix-it-<aleatorio>` y lo borran.
- Un junior lee la espera como una sola petición con tope, y el criterio del alias como tres casos.

## Bloqueos y solicitudes

Vacío.
