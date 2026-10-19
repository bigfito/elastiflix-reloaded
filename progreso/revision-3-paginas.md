# Estado de trabajo — Revisión 3 · Carril E (Web: páginas)

> Carril E de la revisión 3. La especificación de cada corrección está en
> [`docs/revisiones/revision-3.md`](../docs/revisiones/revision-3.md); las reglas de los carriles, en su
> sección «Cómo trabajar en paralelo». Este archivo solo lo escribe el agente de este carril.

## Identificación

- **Unidad:** Revisión 3 · carril E
- **Nombre:** Web: páginas
- **Rama:** `revision-3-paginas` (creada desde `origin/main` en `6c403f4`)
- **Carpeta:** worktree `/workspaces/elastiflix-paginas`
- **Archivos del carril:** web salvo el paquete `ingest/`, `MultipartLimitsTest.java` y las aserciones de «Cargar datos» de `SmokeIT.java` (regla 8); plantillas salvo `ingest.html`; `static/css/**`, `static/img/**`, `home.js` y `results.js`
- **Herramienta de la última sesión:** Claude Code (Opus 5.5)

## Estado general

en curso

## Punto de reanudación

- **Tarea actual:** T-26 (Resultados y CSS)
- **Subpaso en curso:** por empezar
- **Próximo paso:** en `results.html`: `aria-describedby="q-error"` solo si hay error, `h1 id="r-title" aria-live="polite"` y `section aria-labelledby="r-title"`, separador `border-top` antes de APLICAR; en el CSS, `.drop:focus-within` y el foco visible de las pestañas de «Cargar datos».
- **Última actualización:** 2026-10-19 02:49 UTC

## Tareas

> Solo la parte de este carril. El hash se completa en el commit siguiente; nunca dejes «este commit».

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| T-06 | terminada | `4a21bfd` | Con `adult=true`, `ResultsPage.adultChoice` agrega la opción «Solo adultos» marcada (y «Todos» deja de estarlo), como `mk()` de S-11. Pruebas nuevas `ResultsPageTest.shouldKeepTheAdultOnlyFilterChecked` y `shouldNotOfferAdultOnlyByDefault`. `./mvnw -q -pl elastiflix-web -am verify -Dtest=ResultsPageTest -Dsurefire.failIfNoSpecifiedTests=false`: 13 pruebas, 0 fallos, Checkstyle y PMD en 0. Rotura a mano (`adultOnly` siempre `false`): falla `shouldKeepTheAdultOnlyFilterChecked`; revertido. |
| T-07 | terminada | `6db5cfd` | Ícono `static/img/favicon.svg` con `<link rel="icon">` en `fragments/head.html` (ver «Decisiones»). `LayoutPageTest` (ahora importa `NotFoundAdvice`): `shouldShowConnectionIndexAndTmdbOnTheHomePage` afirma el `<link>`; `shouldServeTheIconWithoutReadingTheCatalog` pide el ícono, espera 200 `image/svg+xml` y `verify(catalog, never()).status()`. `./mvnw -q -pl elastiflix-web -am verify -Dtest=LayoutPageTest -Dsurefire.failIfNoSpecifiedTests=false`: 3 pruebas, 0 fallos, Checkstyle y PMD en 0. Rotura a mano (quitar el ícono): falla con 404, y sin la aserción de estado falla `never()` (`NeverWantedButInvoked`, el 404 lee el catálogo); revertido. |
| T-12 (`changeHref`) | terminada | `e160ec0` | Se quitó `model.addAttribute("changeHref", "/")` de `ResultsPage.heading`; «Cambiar ajustes» sigue con `@{/}`. `git grep -n changeHref -- '*/src/main/*'` no devuelve nada (código 1). `./mvnw -q -pl elastiflix-web -am verify -Dtest=ResultsPageTest,ServedPagesTest -Dsurefire.failIfNoSpecifiedTests=false`: 20 pruebas, 0 fallos, Checkstyle y PMD en 0. Es una eliminación de código muerto: no hay comportamiento que romper a mano. El `./mvnw verify` completo va al cierre del carril. |
| T-13 («Acerca de») | terminada | `5e30db1` | Parte de E: `AboutController` recibe `IngestLimits` y pone `maxFileLabel` y `batchSize` en el modelo; `about.html` los usa en «Antes de subir» y «Índice temporal». Prueba nueva `AboutLimitsTest` (`ELASTIFLIX_INGEST_MAX_FILE_SIZE=200MB`, `ELASTIFLIX_INGEST_BATCH_SIZE=50`): muestra «de máximo 200 MB» y «lotes de 50» y no «150 MB» ni «lotes de 100»; `AboutPageTest` afirma los valores por defecto. Las dos declaran `@EnableConfigurationProperties(ElastiflixProperties.class)` e importan `IngestLimits` (regla 7). `./mvnw -q -pl elastiflix-web -am verify -Dtest='About*Test' -Dsurefire.failIfNoSpecifiedTests=false`: 2 pruebas, 0 fallos, Checkstyle y PMD en 0. Rotura a mano (cifras otra vez fijas en la plantilla): falla `AboutLimitsTest`; revertido. La parte de «Cargar datos» es del carril F. |
| T-14 | terminada | `36f85bc` | `LayoutAdvice.shell(HttpServletRequest)` guarda el cromo en `PageShells.REQUEST_ATTRIBUTE`; `WebExceptionHandler.elastiflix` lo reutiliza y solo llama a `catalog.status()` si falta. Prueba nueva `ErrorPageTest.shouldReadTheCatalogOnceForAnElastiflixError` (`/probe/elastiflix` → 500 con el pie y `verify(catalog, times(1)).status()`). `./mvnw -q -pl elastiflix-web -am verify -Dtest=ErrorPageTest,LayoutPageTest,IngestPageTest,ServedPagesTest -Dsurefire.failIfNoSpecifiedTests=false`: 23 pruebas, 0 fallos, Checkstyle y PMD en 0. Rotura a mano (ignorar el atributo y volver a leer): falla con `TooManyActualInvocations` (2 lecturas); revertido. |
| T-19 (`SearchController`) | terminada | `5a7a6c8` | Parte de E: Javadoc de clase y de `search` en `SearchController` fiel al comportamiento (solo `summary()` con punto junto al campo; el código al log en `WARN`). `git grep -n "con su código" elastiflix-web/src/main/java/com/bigfito/elastiflix/web/search` no devuelve nada. `./mvnw -q -pl elastiflix-web -am verify -Dtest=ResultsPageTest -Dsurefire.failIfNoSpecifiedTests=false`: 13 pruebas, 0 fallos, Checkstyle y PMD en 0. Solo cambia Javadoc: no hay comportamiento que romper. Las partes de A y C son de sus carriles. |
| T-20 (página de error) | terminada | `5eeb292` | Parte de E: `ErrorPages.filled` pone `summary() + "."`; `ErrorPageTest.shouldShowTheCatalogMessage` afirma `<p>No se pudo conectar con Elasticsearch.</p>`. Filas de PLAN §1 de la página de error y del 404 en «Notas para el cierre». `./mvnw -q -pl elastiflix-web -am verify -Dtest=ErrorPageTest,ServedPagesTest -Dsurefire.failIfNoSpecifiedTests=false`: 10 pruebas, 0 fallos, Checkstyle y PMD en 0. Rotura a mano (sin el punto): falla `shouldShowTheCatalogMessage`; revertido. La parte de `ingest.html` y `JobTexts` es del carril F. |
| T-21 | terminada | `74a89d5` | `FacetChoices.hasExtra` (`anyMatch(FacetChoice::extra)`) da `genresExtra` y `languagesExtra` al modelo; `results.html` los usa en lugar de `size() > 6`. Prueba nueva `ResultsPageTest.shouldNotOfferExpandWhenNothingIsHidden`: 6 géneros más `genero=Western` (marcado y sin conteo) dan 7 opciones, ninguna `extra`, y no aparece «Ver todos los géneros»; `shouldOfferExpandOnlyPastSixValues` sigue mostrándolo con 7 valores. `./mvnw -q -pl elastiflix-web -am verify -Dtest=ResultsPageTest -Dsurefire.failIfNoSpecifiedTests=false`: 14 pruebas, 0 fallos, Checkstyle y PMD en 0. Rotura a mano (volver a `genres.size() > 6`): falla `shouldNotOfferExpandWhenNothingIsHidden`; revertido. |
| T-22 | terminada | `f2b7b4a` | `elastiflix.css` pasa de 645 a 625 líneas: cada pareja repetida de la lista quedó en una regla (`.fh`, `.filters legend`, `.poster.missing`, `.extra`/`.show-extra .extra`, `.results h1`/`.results-head h1`, `.filters fieldset`, `.results-layout`, `.rrf-value`, `.filters .date-field`, `.hit-list`, `.mode-cards`, `.filters-head h2` y `.legend` dentro de `.results .legend`) y se quitaron las muertas (`.score-panel .eyebrow`, `.sizes`, `.page-size select`, `#job-ok[hidden]`, `.file-chip[hidden]`). Capturas antes y después: 60 de 60 archivos idénticos byte a byte (estilos calculados de todos los elementos y PNG de página completa; 15 pantallas de Resultados, Cargar, Acerca, Inicio y 404 en 1440 y 390 px). Método en «Decisiones». Cambio solo de CSS: lo que se rompería es lo que se ve, y la comparación lo cubre. |
| T-23 | terminada | `d2d3d9b` | Un solo helper: se quitó `CatalogTexts.grouped`/`rest` y la web usa `ingest.mapping.Thousands.of` (sin cambios) en el pie y «Acerca de» (`CatalogTexts`), en los votos (`ResultCards.count`) y en el título del conteo (`ResultsPage.countTitle`). Prueba nueva `ResultsPageTest.shouldGroupThousandsInVotesAndCount` («7.9 · 21 911 votos» y `<h1>1 234 resultados</h1>`); `shouldRenderFullText` pasa a «1 000 votos»; `LayoutPageTest` y `AboutPageTest` siguen afirmando «4 322 películas». `./mvnw -q -pl elastiflix-web -am verify -Dtest=ResultsPageTest,LayoutPageTest,AboutPageTest,ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false`: 26 pruebas, 0 fallos, Checkstyle y PMD en 0. Rotura a mano (votos con `Integer.toString`): fallan `shouldGroupThousandsInVotesAndCount` y `shouldRenderFullText`; revertido. |
| T-25 (`PosterUrls`) | terminada | (hash en el commit siguiente) | Parte de E: `PosterUrls` recibe `ElastiflixProperties` por constructor; sin `ObjectProvider` ni `Poster.fallback()`. `ResultsPageTest` declara `@EnableConfigurationProperties(ElastiflixProperties.class)` (regla 7) y fija `ELASTIFLIX_POSTER_BASE_URL=https://posters.test/w92`; `shouldRenderFullText` afirma `src=\"https://posters.test/w92/t.jpg\"`. `./mvnw -q -pl elastiflix-web -am verify -Dtest=ResultsPageTest,ServedPagesTest,ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false`: 29 pruebas, 0 fallos, Checkstyle y PMD en 0. Rotura a mano (volver al CDN de respaldo): falla `shouldRenderFullText`; revertido. `IngestLimits` es del carril F. |
| T-26 (Resultados y CSS) | pendiente | — | Accesibilidad |
| T-27 (`ErrorPageTest`) | pendiente | — | Pruebas que no pueden fallar, débiles o con higiene pendiente |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw -pl elastiflix-web -am verify` (y al final `./mvnw verify` completo)
- **Resultado:** —
- **Fecha:** —

## Decisiones

- 2026-10-19 02:10 UTC: Claude Code toma el carril E en el worktree `/workspaces/elastiflix-paginas`. El
  primer `./mvnw -q -pl elastiflix-web -am verify` sobre `6c403f4` terminó en verde (3 min 48 s, CPU compartida).
- **T-07 (decisión abierta):** se elige el ícono, no excluir recursos del advice. `static/img/favicon.svg` (el
  logo del header: cuadro durazno `#FFCDB2` de radio 10 con el triángulo `#2E2A3B`) y
  `<link rel="icon" type="image/svg+xml">` en `fragments/head.html`. Con el ícono declarado el navegador ya
  no pide `/favicon.ico`, y el ícono lo sirve el manejador de recursos estáticos, que no pasa por
  `NotFoundAdvice`. Va en `static/img/` y no en la raíz de `static/` porque esa carpeta es la que la tabla
  de carriles asigna a E.
- **T-20 (decisión, parte de E):** se agrega el punto en `ErrorPages.filled` (no en `ErrorCode`, que es de
  A y cuyo `summary()` afirman otras pruebas, regla 2), y se anotan en «Notas para el cierre» las filas de
  PLAN §1 de las páginas de error, que no tienen mockup.
- **T-22 (cómo se comparó):** en lugar de comparar a ojo, un script de Playwright (fuera del repo, en
  `/tmp/claude-1000/lane-paginas/cap/`) abre la aplicación del carril (jar en el puerto 8095, contra el
  Elasticsearch real de `.env`), sirve `elastiflix.css` con `page.route` (el de `HEAD` o el nuevo), bloquea
  los pósters de TMDB para que el resultado no dependa de la red, espera las fuentes y guarda, por pantalla y
  ancho, el `getComputedStyle` de **cada** elemento (y de sus `::before`/`::after`) y una captura PNG de
  página completa. 15 pantallas × 2 anchos (1440 y 390 px): Inicio; Resultados full-text con «Ver todos»
  abierto, con filtros, vectorial, híbrida, sin resultados, sin resultados por filtros, consulta vacía,
  fechas invertidas y `candidatos < k`; Cargar (pestañas registro, JSON y CSV); Acerca; 404. Dos corridas con
  el CSS viejo dieron 60 de 60 archivos idénticos byte a byte (el método es determinista); el CSS nuevo contra
  el viejo, también 60 de 60.
- Además de las reglas que nombra la lista, se unieron las otras parejas con el mismo selector que la
  hoja repetía (`.results`, `.results section`, `.filters`, `.score-panel`, `.score-line`, `.hit-main`),
  con la misma comparación.
- **T-19 (decisión del texto, parte de E):** el Javadoc de `SearchController` dice ahora que el error de
  validación se muestra junto al campo solo con su «qué pasó» (`summary()`) y punto final, y que el código
  `EFX-*` va al log en `WARN`; el de `search` dice «muestra qué pasó junto al campo y registra el código».

## Notas para el cierre

- **T-06 · PLAN §1, fila nueva:** `Results.dc.html` › filtro «Contenido para adultos» | El mockup solo
  ofrece «Todos» y «Solo no adultos» | Si la URL trae `adult=true`, se agrega la opción «Solo adultos»,
  marcada, con su conteo (como `mk()` de las facetas, S-11), para que APLICAR no quite el filtro sin avisar.
- **T-13 (parte de E):** «Acerca de» ya no escribe 150 MB ni 100 a mano: `AboutController` pasa
  `maxFileLabel` y `batchSize` de `IngestLimits`. Si Z actualiza `docs/diseno/4.md` (modelo de cada vista),
  `about` suma esos dos atributos. La parte de F (`@Value`, `max-request-size`) y su nota de PLAN §7 son de F.
- **T-14:** `docs/diseno/4.md` (cromo y errores): `LayoutAdvice.shell` deja el `PageShell` en el atributo de
  petición `PageShells.REQUEST_ATTRIBUTE`, y `WebExceptionHandler` lo reutiliza; solo lee el catálogo si la
  petición no lo trae.
- **T-20 (parte de E) · PLAN §1, filas nuevas** (las páginas de error no tienen mockup):
  - Página de error de la demo (`error.html`) | — | Etiqueta «No se pudo completar», título con el código
    (`EFX-…`) y debajo el «qué pasó» del catálogo **con punto final** (`<summary()>.`, por ejemplo «No se
    pudo conectar con Elasticsearch.»); enlace «Volver a buscar».
  - Error sin código `EFX` (400, 405… del contenedor) | — | Título «Error <estado HTTP>» y el texto
    «Ocurrió un error inesperado. Vuelve a intentarlo.»
  - Página 404 (`not-found.html`) | — | Etiqueta «404», título «Esta página no existe», texto «La dirección
    no corresponde a ninguna pantalla de la demo.» y los enlaces Buscar · Cargar datos · Acerca de la demo.
  - Si `docs/diseno/4.md` describe la página de error, decir que el mensaje es `summary()` más el punto.
- **T-22:** el criterio pide anotar la comparación de capturas en `progreso/revision-3.md`, que es de Z:
  copiar de aquí la evidencia de la fila T-22 y la decisión «T-22 (cómo se comparó)».
- **T-23:** la web ya no tiene helper propio de miles: `CatalogTexts`, `ResultCards` (votos) y
  `ResultsPage` (título del conteo) usan `ingest.mapping.Thousands`, como `JobTexts`. Fuera del carril E
  queda una cifra sin agrupar: el rango de la paginación («Mostrando 1–25 de 1234 resultados») lo arma
  `Pagination.range()` en `elastiflix-search` (carril B). Si Z quiere cerrarlo, es un cambio de B; no se
  tocó aquí.
- **T-25 (parte de E) · regla 1:** `PosterUrls` ya no usa `ElastiflixProperties.Poster.fallback()`. Cuando F
  quite el relleno de `IngestLimits`, Z puede quitar `Poster.fallback()` (y el relleno de `null` de
  `ElastiflixProperties`, si A no lo hizo) de `elastiflix-core`: en la web ya nadie lo usa.

## Bloqueos y solicitudes

Vacío.
