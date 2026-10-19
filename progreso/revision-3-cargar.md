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

terminado

## Punto de reanudación

- **Tarea actual:** ninguna; carril terminado
- **Subpaso en curso:** PR abierto hacia `main`
- **Próximo paso:** que Adolfo revise y fusione el PR del carril F. Después, el carril Z aplica «Notas para el cierre».
- **Última actualización:** 2026-10-19 02:50 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-04 | terminada | `3c6319f` | `ingest.js`: bandera `actionError` que pone `act()` ante un 4xx o una caída de red; `paint()` ya no oculta `#server-error` mientras esté puesta y el siguiente clic la limpia. Prueba: `BASE_URL=http://localhost:8096 node --test ingest.test.js` → «T-04: un 409 al importar deja el aviso visible después del siguiente sondeo» ✔. Con `ingest.js` de `main` falla (`actual: false, expected: true`); se restauró la corrección. |
| T-05 | terminada | `12ca067` | `ingest.js`: `choose()` vacía `input.value` tras guardar el archivo; un `change` del separador o de la casilla de encabezado rehabilita «Validar archivo» si hay un archivo que pasó la comprobación del navegador; `upload()` quita la marca «rechazado» de la ficha. Prueba: «T-05: validar un CSV, cambiar el separador y validar el mismo archivo hace un segundo POST» ✔ (dos POST, con `,` y `;`). Con `ingest.js` sin T-05 falla en el input (`actual: 'C:\\fakepath\\peliculas.csv'`); sin el `change` del separador falla en el botón (`actual: false`); se restauró la corrección. |
| T-11 | terminada | `912bc3e` | Nuevo `src/test/browser/ingest.test.js` con `page.route()` sobre `/cargar/archivo` y `/cargar/trabajos/*`: 409 al importar (T-04), revalidar el mismo archivo con otro separador (T-05), `cancelado` → paso 4 detenido, doble clic en Importar → un POST, caída de red al importar y al subir → aviso de red. `suggestions.test.js` intercepta `/buscar`. `BASE_URL=http://localhost:8096 npm test` (app con `ELASTICSEARCH_URL=http://localhost:9200`, sin Elasticsearch): 10 de 10 ✔. Rotura a mano: sin `cancelado` en `stepState`, sin `importButton.disabled = true` en `act`, sin el aviso en el `catch` de `upload` y sin la bandera en el `catch` de `act` → los cuatro casos nuevos ✖; T-04 y T-05 ya se rompieron en sus filas; se restauró. README de la carpeta actualizado. |
| T-13 («Cargar datos» y `application.yml`) | terminada (parte F) | `666344c` | `IngestJobController` ya no usa `@Value`: el 413 propio sale de `limits.maxFileSize()`. Nuevo `UploadMultipartConfig` (`@Configuration`) crea el `MultipartConfigElement`: archivo = `elastiflix.ingest.max-file-size`, petición = ese valor + 1 MB; Spring Boot no crea el suyo. El YAML pierde `spring.servlet.multipart.*`. Pruebas: `IngestConfiguredLimitsTest` (MockMvc con `ELASTIFLIX_INGEST_MAX_FILE_SIZE=200MB` y `ELASTIFLIX_INGEST_BATCH_SIZE=50`: «Máximo 200 MB», «(máximo 200 MB)», «en lotes de 50.», `data-max-bytes="209715200"`; 200 MB pasa y 200 MB + 1 byte da 413 `EFX-ING-003`), `MultipartLimitsTest` (200 MB → 201 MB; por defecto 150/151 MB; el YAML ya no repite topes) e `IngestJobErrorTest` (tope de 32 B por `elastiflix.ingest.max-file-size`): `./mvnw -q -pl elastiflix-web -am test -Dtest=…` EXIT 0. Con el controlador de `main` fallan `IngestConfiguredLimitsTest:80` (`expected:<200> but was:<413>`) e `IngestJobErrorTest:55` (`expected:<413> but was:<400>`); se restauró. Con la app real y `ELASTIFLIX_INGEST_MAX_FILE_SIZE=200MB`, un JSON de 160 MB: jar de `main` → 413 `EFX-ING-003` (corta `max-request-size: 151MB`); jar de la rama → 200 con `jobId`. «Acerca de» es del carril E. |
| T-15 | terminada | `dec3535` | Decisión: manejador `@ExceptionHandler(RuntimeException.class)` → 500 JSON `EFX-IMP-007` en `IngestJobErrors` (sin `catch` genérico); `UploadSizeErrors` lleva `@Order(HIGHEST_PRECEDENCE)` para que el 413 no se vuelva 500. `IngestJobController` borra el temporal si `acceptFile` (o `transferTo`) lanza, con `finally` y bandera. Pruebas en `IngestJobJsonErrorsTest`: `shouldAnswerJsonWhenAnUncodedFailureHappens` (`IllegalStateException` → 500 JSON `EFX-IMP-007`) y `shouldDeleteTheUploadCopyWhenTheJobCannotStart` (el temporal ya no existe); `IngestJobErrorTest` sigue dando 413 con los dos advices. `./mvnw -q -pl elastiflix-web -am test -Dtest=…` EXIT 0 y `verify -DskipTests` EXIT 0. Rotura: sin el manejador, las dos pruebas ✖ (`Servlet Request processing failed: IllegalStateException`); sin el borrado, `…JobCannotStart:111` ✖ (`Expecting path … not to exist`); se restauró. |
| T-20 («Cargar datos» y `JobTexts`) | terminada (parte F) | `cd12a98` | Decisión: agregar el punto y registrar cada texto en PLAN §1 (no se cambia al texto del mockup, que habla de un escenario). `ingest.html`: la lista de rechazos queda `<código> · <campo> · <esperado>.`. `JobTexts`: cada advertencia real queda `<campo>: <mensaje> (N).` (con `(N)` solo si N > 1), con punto como la de vectores. Pruebas nuevas en `IngestPageTest`: `shouldEndEachRejectionWithAPeriod` y `shouldEndEachWarningWithAPeriod`; `./mvnw -q -pl elastiflix-web -am test -Dtest=IngestPageTest` EXIT 0 (12 pruebas). Con `ingest.html` y `JobTexts` de `main` fallan las dos (`…:184` sin el punto; `…:173` `but was:<spoken_languages: se omite (62)>`); se restauró. Las filas de PLAN §1 van en «Notas para el cierre». `error.html` es del carril E. |
| T-25 (`IngestLimits`) | terminada (parte F) | `254b3d3` | `IngestLimits` recibe `ElastiflixProperties` por constructor: sin `ObjectProvider` ni `new ElastiflixProperties(null, …)`. Las cuatro pruebas `@WebMvcTest` de carga (`IngestPageTest`, `IngestJobJsonErrorsTest`, `IngestJobErrorTest`, `IngestConfiguredLimitsTest`) declaran `@EnableConfigurationProperties(ElastiflixProperties.class)` (regla 7). `./mvnw -q -pl elastiflix-web -am test -Dtest='com/bigfito/elastiflix/web/**/*Test,…/architecture/*Test'` EXIT 0 (62 pruebas) y `verify -DskipTests` EXIT 0. Sin la anotación, `IngestPageTest` no arranca (`No qualifying bean of type '…ElastiflixProperties'`): la página ya no tiene un respaldo hecho para las pruebas; se restauró. `git grep 'new ElastiflixProperties\|ObjectProvider' elastiflix-web/src/main` solo deja `PosterUrls` (carril E). |
| T-26 (teclado de las pestañas) | terminada (parte F) | `230e505` | `ingest.html`: las pestañas no elegidas llevan `tabindex="-1"`. `ingest.js`: `selectTab()` mueve `tabIndex` 0/-1 con `aria-selected`; ←/→ (con vuelta), Inicio y Fin enfocan y activan la pestaña (activación automática del patrón de pestañas de WAI-ARIA). Prueba: «T-26: las pestañas usan tabindex itinerante y se recorren con flechas, Inicio y Fin» ✔ (`npm test`: 11 de 11). Con `ingest.html` e `ingest.js` sin T-26 ✖; se restauró. El `:focus-within` de `.drop` y el foco visible de las pestañas son CSS del carril E (regla 3). |
| T-27 (`IngestPageTest`) | terminada (parte F) | `6b1ecac` y el commit de cierre | `IngestPageTest`: `shouldShowTheAuthorizedCopy` y `shouldShowTheRejectedFileCopy` afirman el modelo (`nav`, `maxFileLabel`, `maxFileBytes`, `batchSize`) y lo que sale del `shell` (alias); `shouldRedirectAfterSavingOneMovie` verifica el flash `savedTitle` = «A», sin `rejected`, y que la página pinta «Se guardó <span>A</span>.»; nueva `shouldRedirectWithTheIssuesWhenTheMovieIsRejected` (flash `rejected` con el informe, sin `savedTitle`). `./mvnw -q -pl elastiflix-web -am test -Dtest=IngestPageTest` EXIT 0 (13 pruebas). Rotura: flash con el id en lugar del título → `shouldRedirectAfterSavingOneMovie:286` ✖ (`expected:<A> but was:<1>`); `maxFileLabel` fijo «149 MB» → `…AuthorizedCopy:83` y `…RejectedFileCopy:111` ✖; se restauró. Un lote escrito a mano como 100 no se distingue aquí (es el valor por defecto): lo detecta `IngestConfiguredLimitsTest` (50). Higiene (commit de cierre): en `IngestPageTest.shouldRunTheJobFlow` e `IngestConfiguredLimitsTest` la fachada simulada borra el temporal que recibe, así que ya no quedan `elastiflix-upload-*.bin` de 2 bytes en el tmp del sistema (25 antes y 25 después de correrlas). |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `git merge origin/main` (ya al día, `6c403f4`); `./mvnw verify`; `./mvnw verify -Pit`; pruebas de
  navegador con el jar de la rama (`ELASTICSEARCH_URL=http://localhost:9200 SERVER_PORT=8096`, sin Elasticsearch)
  y `BASE_URL=http://localhost:8096 npm test`
- **Resultado:** `verify` BUILD SUCCESS: unitarias 50 core + 62 search + 97 ingest + 63 web = 272, 0 fallas,
  Checkstyle y PMD en 0. `verify -Pit` BUILD SUCCESS: lo anterior más 29 IT (9 core, 14 search, 2 ingest, 4 web),
  0 fallas y 0 omitidas. Navegador: 11 de 11 ✔.
- **Fecha:** 2026-10-19 02:45 UTC

## Decisiones

- 2026-10-19 02:10 UTC: carril tomado con Claude Code (Opus 5.5), worktree `/workspaces/elastiflix-cargar`.
- T-04: bandera `actionError` (la opción que sugiere la lista); se sigue sondeando tras un 4xx para que la
  página refleje el estado real del trabajo. Un fallo de sondeo posterior no tapa el aviso de la acción.
- T-05: «Validar archivo» se rehabilita solo si el archivo elegido pasó la comprobación del navegador; no se
  borra el resultado anterior hasta el siguiente POST.
- T-13: se deriva `max-request-size` en lugar de documentar dos valores: un `MultipartConfigElement`
  (`UploadMultipartConfig`) con archivo = `elastiflix.ingest.max-file-size` y petición = ese valor + 1 MB.
  Spring Boot no crea el suyo si ya hay uno (`@ConditionalOnMissingBean`). Se comprobó con la app real.
- T-15: manejador de Spring para `RuntimeException` en `IngestJobErrors` (no `catch` genérico) y `@Order` en
  `UploadSizeErrors`; el JS no cambia. El temporal se borra con `finally` y bandera, porque `JM-ERR-01` prohíbe
  atrapar `RuntimeException`. El log del manejador incluye la traza de la excepción (no hay contenido de archivos).
- T-20: punto final y filas en PLAN §1 (no se reescriben las advertencias con las frases del mockup).
- T-26: activación automática al mover con flechas (patrón de pestañas de WAI-ARIA), con vuelta en los extremos.

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
- **T-20 · PLAN §1** (tabla de ajustes autorizados), tres filas nuevas, con el texto exacto:
  - `Ingest.dc.html` › registro individual guardado (revisión 3, T-20) | — | «Se guardó <título>.» en una nota
    `role="status"` sobre las pestañas, tras guardar una película con el formulario.
  - `Ingest.dc.html` › registro individual rechazado (revisión 3, T-20) | — | Una alerta con una línea por
    error: «<código> · <campo> · <lo que espera el mapping>.», por ejemplo «EFX-ING-012 · vote_count · un
    número entero.».
  - `Ingest.dc.html` › advertencias de la validación (revisión 3, T-20) | «62 películas tienen un idioma vacío
    dentro de spoken_languages: ese valor se omitirá…» (frases del escenario) | Las advertencias reales se
    agrupan por código y campo: «<campo>: <mensaje> (N).», por ejemplo «spoken_languages: se omite (62).» o
    «tagline: sin valor.» (sin «(N)» si es una sola). La de vectores conserva su frase completa y va primero.
- **T-25 · regla 7 (para E y Z):** desde este carril, `IngestLimits` necesita el bean `ElastiflixProperties`.
  Si una prueba `@WebMvcTest` del carril E (por ejemplo la de «Acerca de», T-13) importa `IngestLimits`, tiene
  que declarar también `@EnableConfigurationProperties(ElastiflixProperties.class)`. Z lo comprueba al
  integrar. El constructor de `ElastiflixProperties` con relleno de `null` ya no lo usa `IngestLimits`; si
  `PosterUrls` (E) también deja de usarlo, Z puede decidir quitarlo (regla 1).

## Bloqueos y solicitudes

Vacío.
