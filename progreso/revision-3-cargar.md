# Estado de trabajo — Revisión 3 · Carril F (Web: Cargar datos)

> Carril F de la revisión 3. La especificación de cada corrección está en
> [`docs/revisiones/revision-3.md`](../docs/revisiones/revision-3.md); las reglas de los carriles, en su
> sección «Cómo trabajar en paralelo». Este archivo solo lo escribe el agente de este carril.

## Identificación

- **Unidad:** Revisión 3 · carril F
- **Nombre:** Web: Cargar datos
- **Rama:** `revision-3-cargar` (creada desde `origin/main` en `6c403f4`)
- **Carpeta:** worktree `/workspaces/elastiflix-cargar`
- **Archivos del carril:** paquete `ingest/` de web y sus pruebas, `MultipartLimitsTest.java`, las aserciones de «Cargar datos» de `SmokeIT.java` (regla 8), `ingest.html`, `ingest.js`, `application.yml` de web y `src/test/browser/**`
- **Herramienta de la última sesión:** Claude Code (Opus 5.5)

## Estado general

en curso

## Punto de reanudación

- **Tarea actual:** T-20
- **Subpaso en curso:** T-15 terminada
- **Próximo paso:** T-20: punto final en la lista de rechazos de `ingest.html` y filas de PLAN §1 para «Se guardó …», los rechazos y las advertencias de `JobTexts`.
- **Última actualización:** 2026-10-19 02:31 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-04 | terminada | `3c6319f` | `ingest.js`: bandera `actionError` que pone `act()` ante un 4xx o una caída de red; `paint()` ya no oculta `#server-error` mientras esté puesta y el siguiente clic la limpia. Prueba: `BASE_URL=http://localhost:8096 node --test ingest.test.js` → «T-04: un 409 al importar deja el aviso visible después del siguiente sondeo» ✔. Con `ingest.js` de `main` falla (`actual: false, expected: true`); se restauró la corrección. |
| T-05 | terminada | `12ca067` | `ingest.js`: `choose()` vacía `input.value` tras guardar el archivo; un `change` del separador o de la casilla de encabezado rehabilita «Validar archivo» si hay un archivo que pasó la comprobación del navegador; `upload()` quita la marca «rechazado» de la ficha. Prueba: «T-05: validar un CSV, cambiar el separador y validar el mismo archivo hace un segundo POST» ✔ (dos POST, con `,` y `;`). Con `ingest.js` sin T-05 falla en el input (`actual: 'C:\\fakepath\\peliculas.csv'`); sin el `change` del separador falla en el botón (`actual: false`); se restauró la corrección. |
| T-11 | terminada | `912bc3e` | Nuevo `src/test/browser/ingest.test.js` con `page.route()` sobre `/cargar/archivo` y `/cargar/trabajos/*`: 409 al importar (T-04), revalidar el mismo archivo con otro separador (T-05), `cancelado` → paso 4 detenido, doble clic en Importar → un POST, caída de red al importar y al subir → aviso de red. `suggestions.test.js` intercepta `/buscar`. `BASE_URL=http://localhost:8096 npm test` (app con `ELASTICSEARCH_URL=http://localhost:9200`, sin Elasticsearch): 10 de 10 ✔. Rotura a mano: sin `cancelado` en `stepState`, sin `importButton.disabled = true` en `act`, sin el aviso en el `catch` de `upload` y sin la bandera en el `catch` de `act` → los cuatro casos nuevos ✖; T-04 y T-05 ya se rompieron en sus filas; se restauró. README de la carpeta actualizado. |
| T-13 («Cargar datos» y `application.yml`) | terminada (parte F) | `666344c` | `IngestJobController` ya no usa `@Value`: el 413 propio sale de `limits.maxFileSize()`. Nuevo `UploadMultipartConfig` (`@Configuration`) crea el `MultipartConfigElement`: archivo = `elastiflix.ingest.max-file-size`, petición = ese valor + 1 MB; Spring Boot no crea el suyo. El YAML pierde `spring.servlet.multipart.*`. Pruebas: `IngestConfiguredLimitsTest` (MockMvc con `ELASTIFLIX_INGEST_MAX_FILE_SIZE=200MB` y `ELASTIFLIX_INGEST_BATCH_SIZE=50`: «Máximo 200 MB», «(máximo 200 MB)», «en lotes de 50.», `data-max-bytes="209715200"`; 200 MB pasa y 200 MB + 1 byte da 413 `EFX-ING-003`), `MultipartLimitsTest` (200 MB → 201 MB; por defecto 150/151 MB; el YAML ya no repite topes) e `IngestJobErrorTest` (tope de 32 B por `elastiflix.ingest.max-file-size`): `./mvnw -q -pl elastiflix-web -am test -Dtest=…` EXIT 0. Con el controlador de `main` fallan `IngestConfiguredLimitsTest:80` (`expected:<200> but was:<413>`) e `IngestJobErrorTest:55` (`expected:<413> but was:<400>`); se restauró. Con la app real y `ELASTIFLIX_INGEST_MAX_FILE_SIZE=200MB`, un JSON de 160 MB: jar de `main` → 413 `EFX-ING-003` (corta `max-request-size: 151MB`); jar de la rama → 200 con `jobId`. «Acerca de» es del carril E. |
| T-15 | terminada | se anota en el commit siguiente | Decisión: manejador `@ExceptionHandler(RuntimeException.class)` → 500 JSON `EFX-IMP-007` en `IngestJobErrors` (sin `catch` genérico); `UploadSizeErrors` lleva `@Order(HIGHEST_PRECEDENCE)` para que el 413 no se vuelva 500. `IngestJobController` borra el temporal si `acceptFile` (o `transferTo`) lanza, con `finally` y bandera. Pruebas en `IngestJobJsonErrorsTest`: `shouldAnswerJsonWhenAnUncodedFailureHappens` (`IllegalStateException` → 500 JSON `EFX-IMP-007`) y `shouldDeleteTheUploadCopyWhenTheJobCannotStart` (el temporal ya no existe); `IngestJobErrorTest` sigue dando 413 con los dos advices. `./mvnw -q -pl elastiflix-web -am test -Dtest=…` EXIT 0 y `verify -DskipTests` EXIT 0. Rotura: sin el manejador, las dos pruebas ✖ (`Servlet Request processing failed: IllegalStateException`); sin el borrado, `…JobCannotStart:111` ✖ (`Expecting path … not to exist`); se restauró. |
| T-20 («Cargar datos» y `JobTexts`) | pendiente | — | Textos visibles sin punto final o sin fila en PLAN §1 |
| T-25 (`IngestLimits`) | pendiente | — | Configuración y beans armados para las pruebas |
| T-26 (teclado de las pestañas) | pendiente | — | Accesibilidad |
| T-27 (`IngestPageTest`) | pendiente | — | Pruebas que no pueden fallar, débiles o con higiene pendiente |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw -pl elastiflix-web -am verify` (y al final `./mvnw verify` completo)
- **Resultado:** `main` de partida (`6c403f4`) en verde (EXIT 0)
- **Fecha:** 2026-10-19 02:13 UTC

## Decisiones

- 2026-10-19 02:10 UTC: carril tomado con Claude Code (Opus 5.5), worktree `/workspaces/elastiflix-cargar`.

## Notas para el cierre

- **T-11 · `docs/diseno/4.md`** (lista de cambios de la Fase 4, después de S-10): agregar «Revisión 3, T-04,
  T-05 y T-11: `ingest.js` conserva el aviso de un 4xx o de una caída de red al importar o cancelar hasta el
  siguiente clic (bandera `actionError`), vacía el `<input type=file>` al elegir y rehabilita «Validar
  archivo» al cambiar el separador o la casilla de encabezado. `src/test/browser/ingest.test.js` prueba esos
  estados con `page.route()`, sin Elasticsearch; `suggestions.test.js` intercepta `/buscar`.»

- **T-13 · PLAN §7** (tabla de `application.yml`): quitar las filas `spring.servlet.multipart.max-file-size` y
  `spring.servlet.multipart.max-request-size` y agregar debajo de la tabla: «Los topes del multipart no van en
  el YAML: `UploadMultipartConfig` (`elastiflix-web`) crea el `MultipartConfigElement` con el archivo hasta
  `elastiflix.ingest.max-file-size` y la petición hasta ese valor más 1 MB (los otros campos del formulario).
  Subir `ELASTIFLIX_INGEST_MAX_FILE_SIZE` sube los dos (revisión 3, T-13).» En la tarea 3.x de PLAN (l. 851,
  «`max-file-size` 150 MB, `max-request-size` 151 MB») decir que salen de `UploadMultipartConfig`. El
  requerimiento 3 (l. 1332) puede fijar ahí 25 MB y el margen en lugar del YAML.
- **T-13 · `docs/diseno/3.md`**: «Comprobación del archivo» (l. 294) y la fila `MultipartLimitsTest` de
  «Pruebas previstas» (l. 525): el YAML ya no tiene `max-file-size` ni `max-request-size`; `MultipartLimitsTest`
  comprueba `max-swallow-size: -1` y que `UploadMultipartConfig` deriva 200 MB → 201 MB (y 150 → 151 por
  defecto). Agregar en «Cambios respecto al diseño aprobado»: «Revisión 3, T-13: `IngestJobController` toma el
  tope de `IngestLimits` (sin `@Value`) y `UploadMultipartConfig` deriva los topes del multipart.»
- **T-15 · `docs/diseno/4.md`** (o la tabla de errores de la web): «Revisión 3, T-15: un endpoint de trabajos
  que lanza una excepción sin código responde 500 JSON `EFX-IMP-007` (`IngestJobErrors.unexpected`), no la
  página HTML de `/error`; si `acceptFile` lanza, el temporal de la subida se borra. `UploadSizeErrors` va
  primero (`@Order`).» En la tabla de errores de `docs/diseno/3.md`, a `EFX-IMP-007` agregar «también: falla
  sin código en un endpoint de carga».

## Bloqueos y solicitudes

Vacío.
