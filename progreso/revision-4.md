# Estado de trabajo — Revisión 4

> Archivo de estado para retomar el trabajo en otra sesión sin perder avance. Especificación:
> [`docs/revisiones/revision-4.md`](../docs/revisiones/revision-4.md). Reglas en `PLAN.md` §9.

## Identificación

- **Unidad:** Revisión 4
- **Nombre:** Correcciones de la cuarta revisión (tras las fases 7 y 8)
- **Rama:** `revision-4-resto` (la primera tanda entró en `main` con el PR #25, merge `b54d67c`)
- **Carpeta:** `E:\02 - programming\03 - elastiflix-reloaded`
- **Herramienta de la última sesión:** Claude Code (Opus 5.5)

## Estado general

en verificación

## Punto de reanudación

- **Tarea actual:** cierre de la revisión 4 en `revision-4-resto`: los cinco carriles están integrados y la
  documentación está alineada con el código integrado
- **Subpaso en curso:** verificación integrada (la registra el integrador en «Última verificación»)
- **Próximo paso:** con `./mvnw verify -Pit` y las pruebas de navegador en verde sobre `revision-4-resto`, abrir el
  PR hacia `main`. Después, la aprobación de Adolfo de las 22 capturas (U-36) y de la variante de error
  `EFX-SRCH-012` de `Detail*.dc.html` (U-07), y su decisión sobre el patrón «View Helper / ViewModel» del diseño 7
  (U-38). Luego, coordinar con Adolfo el Requerimiento 3.
- **Última actualización:** 2026-10-21 UTC

## Tareas

> El hash de una tarea se completa en el commit de la tarea siguiente.

| Id | Estado | Commit | Evidencia |
|---|---|---|---|
| U-11 | terminada | `a1b8fa8` | `./mvnw verify` en verde (386 unitarias); `AppliedFiltersTest` en español e inglés |
| U-03 | terminada | `ec6ca83` | `./mvnw verify` en verde; `ExternalLinksTest` (16 casos) y `MovieDetailPageTest` |
| U-01 | terminada | `59d2662` | Medición del 2026-10-21 (k=100, 200 candidatos, 6 899 películas): Life of Pi 31.º · 7.º · 5.º (antes 30 · 7 · 4); Titanic y WALL·E sin cambios. `VectorSearchIT`, `FullTextSearchIT` e `HybridSearchIT` en verde (14 de 14) |
| U-24 | terminada | `59d2662` | Sugerencias en español: Titanic 36/7/4, Inception 94/13/8, Life of Pi 31/7/5, Spirited Away 13/4/1 (E.T. y WALL·E sin cambios). Conteos léxicos de los dos idiomas: dark knight 57/5, love story 107/3, el secreto 35/2, le fabuleux 26/1, heaven 10, next 15; Kolejne en title 3 (OR) y 0 (AND); híbrida Titanic 1.º vectorial, 1.º léxico. Las de inglés coinciden con la fase 8. `./mvnw -pl elastiflix-web verify` en verde |
| U-07 | terminada | `f70be66` | Texto nuevo en `ErrorCode`, `messages_es` y `messages_en`, y en `docs/diseno/7.md`. `MovieDetailControllerTest` (7), `MessageCatalogParityTest` (3) y `ErrorPageTest` (4) en verde |
| U-16 | terminada | `3d1933f` | `EmptyState` enum, `ComponentScore(score, rank, k)` y `Pagination(first, last)`; la web arma los textos con `results.pager.span` y `results.card.score.outsideTop`. `./mvnw verify` en verde (386 unitarias, Checkstyle 0, PMD 0) |
| U-13, U-17 (carga) | terminada | `56aeaf9` | `IngestPhrase` (57 frases) en ingest; `IngestTranslator` y `JobTexts` traducen por clave en los dos idiomas; informe CSV, rechazos del registro individual y 413 traducidos. `./mvnw verify` en verde: 513 unitarias (core 68, search 71, ingest 189, web 185), Checkstyle 0, PMD 0. `IngestTranslatorTest` pasa la salida real de `MappingValidator`, `CsvMovieReader` y `CsvColumnRows` en inglés sin una palabra en español |
| U-17 (cabecera y sugerencias) | terminada | `0703166` | `CatalogTexts` lee `layout.catalog.*` (solo quedan como constantes los dos avisos de TMDB, que se muestran juntos en los dos idiomas); las etiquetas de Inicio salen de `home.suggest.*` vía `SuggestionTags`, con las cifras medidas como datos. `./mvnw -pl elastiflix-web verify` en verde (185 unitarias, Checkstyle 0, PMD 0); `HomePageTest`, `LayoutPageTest` y `AboutPageTest` sin cambios |
| U-14, U-15 | terminada | `a26826e` | `ingest.html` sin textos fijos (rango de errores, `aria-label` del progreso, un placeholder por concordancia); `ingest.js` sin respaldos en español, con «Field for {0}» por `data-*` y el tono de cada columna desde `JobTexts.columnTones`. `TemplatesAndScriptsI18nTest` revisa texto suelto y atributos accesibles de las plantillas y todos los literales del JS; con los archivos de `main` falla con los 10 textos que tenían. `./mvnw verify` en verde: 514 unitarias (core 68, search 71, ingest 189, web 186), Checkstyle 0, PMD 0 |
| U-02 | terminada | `db440ca` | `./mvnw verify -Pit` en verde (514 unitarias y 30 de integración: core 68 + 10, search 71 + 14, ingest 189 + 2, web 186 + 4) y pruebas de navegador 11 de 11 (Playwright 1.49.1 con Chromium, contra la aplicación en el puerto 8089 sin Elasticsearch) |
| U-30 | terminada | `26aaa6d` | `VectorProjector` rechaza norma 0, `NaN` o infinita; `InferenceEmbeddingService.toArray`, valores no finitos (los dos `EFX-EMB-002`); `CatalogBootstrapRunner` con `WebServerApplicationContext` y `spring-boot-web-server` opcional en core. Pruebas de normas, de la respuesta de inferencia y del contexto con y sin servidor web. Integrada en `2c3ca5f` |
| U-34 | terminada | `2f71472` | `INDICE.md` › «Aprobaciones»: fila de la fase 7 restituida, arranque del Requerimiento 2 y diseño 8 sin aprobación registrada |
| U-33 | terminada (package-info); cabeceras descartadas | `a6d67d5` | `package-info.java` en `web/about`, `web/error`, `web/home`, `web/ingest`, `web/layout` y `web/search`; `./mvnw -B verify` en verde (web 186, Checkstyle 0, PMD 0). Adolfo decidió no agregar cabeceras de seguridad (2026-10-21) |
| U-35 | terminada | `4545358` | Cifras de pruebas (341 y 366) y horas UTC de las fases 7 y 8 rehechas desde `git log` en `progreso/7.md`, `8.md` e `INDICE.md` |
| U-06 | terminada | `6d827ad` | `findById`: solo `found=false` da vacío; `index_not_found_exception` → `EFX-ES-007`, 5xx o corte → `EFX-ES-001`. `ElasticsearchCatalogIndexTest` (404, 5xx, `IOException`) y `MovieDetailControllerTest` (página EFX, HTTP 500) |
| U-37 | terminada | `652424b` | PLAN 8.4 a 8.7, diseño 8 §6 y `progreso/7.md` y `8.md` ya no afirman trabajo que no se hizo |
| U-31 | terminada | `64713b5`, `731ea3f` | Core y search: `CatalogIndexIT` ejercita `isOwnedBy` y limpia todo, `IndexNamesTest`, `assumeTrue` en `EmbeddingServiceIT` y `DocumentEmbeddingCalibrationIT`, `SearchQueryValidatorTest` con `k=1` y los bordes de `num_candidates`. Ingest: `ImportJobTest` con `_aliases` 5xx (`EFX-IMP-006`), `IngestServiceTest` sin carrera y `FieldRulesMappingTest`. `./mvnw verify` en verde; las IT se compilaron pero el carril no las corrió contra Elasticsearch |
| U-08 | terminada | `9f99135`, `cccd473`, `a4aef6b` | Selector: cada segmento se codifica una sola vez (`LanguageSwitchLinksTest`: café, `a%2Fb`, `;`, espacio, escape mal formado, `//`). Enlace: `pathSegment(id)` (`SearchLinksTest`, MockMvc con `a/b` y `café`). `EncodedSlashConfig` deja pasar `%2F` en Tomcat (`ServedPagesTest` con el servidor real) |
| U-04 | terminada | `0c561c5` | `PosterUrls` solo acepta `^/[\w.-]+$`; otra ruta da marcador e «N/A» en «Imágenes». `PosterUrlsTest` y `MovieDetailPageTest` |
| U-25 | terminada | `d09cdc1` | `KeptReport` con los primeros 10 000 errores (`JobRetention.MAX_KEPT_ERRORS`) y las cifras del informe entero (`errorTotal`); advertencias solo agrupadas. `ReportCapTest` |
| U-19 | terminada | `39c0276` | `getQueryString()`; tras un método que no es `GET` ni `HEAD`, `/cargar` o `/`; en `/error`, ruta, método y parámetros originales; cookie `HttpOnly` y una ilegible se ignora. `LanguageSwitchLinksTest`, `LocaleSwitchTest` (sin el cambio, la cookie `e$` falla) y `ServedPagesTest` (400 y 405 reales). `./mvnw -B verify` en verde (web 200) |
| U-38 | terminada | `07ba4c4` | Diseños 7 y 8 con las secciones de la plantilla y «Cambios respecto al diseño aprobado»; revisión de convenciones de `progreso/7.md` y `8.md` con las 10 preguntas de §16. El patrón «View Helper / ViewModel» queda pendiente de la decisión de Adolfo |
| U-22 | terminada | `6ba0d1a`, `d8ea314`, `45865dc` | Textos en inglés del mockup (`EFX-SRCH-001`, `EFX-ES-001`, `detail.fact.id`, `detail.cast.showMore`); «Créditos» con la frase de TMDB y `lang` en cada aviso (`AboutPageTest`); medición de la consulta de ejemplo de Resultados en inglés en el diseño 8 §4. `detail.chip.pageFormat` sin el total N, a propósito (ver «Decisiones») |
| U-26 | terminada | `9fd42e8` | `KeywordBytes`: `id` > 512 bytes y `keyword` sin `ignore_above` > 32 766 bytes dan `EFX-ING-012` (`IngestPhrase.TEXT_UP_TO_BYTES`); `TextListValues` sale de `NestedValues`. `KeywordBytesTest` |
| U-18 | terminada | `1cc176b`, `fe74f68`, `8d0d72c`, `a4aef6b` | Sin `DEFAULT_INSTANCE` ni `defaultCatalog()`; sin `@Import` repetido; sin ramas muertas, sobrecargas y claves sin uso (`results.query.error`, `results.adjust.mode.*`, `detail.poster.defaultAlt`); sin `data-alias` ni `var total`; `AppliedFilters.chips(SearchForm)` se borró al integrar. `./mvnw -B verify` en verde en cada carril |
| U-10 | terminada | `057c0be` | `mapper.toQuery` valida una sola vez; `validForm` devuelve `Optional`; `ExternalLinks.imdb` es el único enlace a IMDb. `ExternalLinksTest` |
| U-27 | terminada | `4b0275d` | Rutas completas (`keywords.id`, `belongs_to_collection.title`) en los errores de los subcampos. `NestedPathTest` con el caso de la revisión |
| U-12 | terminada | `f497d1b`, `ff2d916`, `77083e1` | Cifras escritas con `Thousands` antes de `MessageFormat`: `k`, lote de «Acerca de», reparto y palabras clave del detalle, lote del paso 4 y números de lote. `ModeCopiesTest`, `AboutLimitsTest`, pruebas del detalle e `IngestBatchSizeTextTest` en los dos idiomas |
| U-36 | terminada | `9981533`, `3d04eb1` y los commits de este cierre | `AGENTS.md`, `CLAUDE.md`, PLAN, README, `design/README.md`, convenciones, glosario y arquitectura alineados con las fases 7 y 8; 22 capturas nuevas (11 páginas × 2 idiomas) pendientes de aprobación; el chip «Página» ya no repite la palabra |
| U-07 (mockup) | terminada, pendiente de aprobación | `0b113fb` | Variante «estado» › «Error EFX-SRCH-012 (pendiente de aprobación)» en `Detail.dc.html` y `Detail-en.dc.html`; PLAN y `design/README.md` lo registran |
| U-05 | terminada | `056059d`, `a4aef6b` | «N/A» en la cabecera (`vote_count` como «· N/A») y en `overview`, `plot`, `cast`, `keywords` y `genres`; título original y lema vacíos ocultan su línea. `MovieDetailPageTest` con un `Movie` vacío y la página renderizada |
| U-28 | terminada | `8821947` | Cancelación entre etapas de la validación, borrado del temporal ante un `create` con corte, timeout o 5xx, y huérfano que no se borra con `WARN` sin detener la importación. `ValidationCancelTest` y `TempCleanupTest` |
| U-29 | terminada | `3dd7565` | `FORMULA` con `\t` y `\r`; sin `.ndjson` en el selector de archivos. `MappingValidatorTest` e `IngestPageTest` |
| U-23 | terminada | `c04116d`, `9545364` | `LocaleSwitchTest` (no ASCII, detalle, error EFX, 404, `lang=EN`, `lang=` vacío, atributos de la cookie), `MessageCatalogParityTest` compara los `{n}`, `ServedPagesTest` y `SmokeIT` con el detalle y `lang=en`, `MovieDetailReturnLinkTest` (multivalor, vectorial, híbrida, `a+b & c#d ñ`). `./mvnw -B verify` en verde (web 221). `SmokeIT` se escribió pero el carril no la corrió |
| U-09 | terminada | `2b45750` | Marcador del fondo si la imagen falla, `mono-tag` en el póster de `detail.js`, reparto completo sin JavaScript, «Mostrar menos», chip «Ajustes» de la híbrida completo y `detail.quoted`. Pruebas de la vista y de la página; `detail.js` probado en Chromium con y sin JavaScript |
| U-32 | terminada | `2e3c812` | Javadoc fiel en `IngestJobErrors` (400 HTML sin la parte `archivo`, con prueba), `ImportProgress`, `JobView`, `WriteTotals` y `ValidationIssue.recordLabel` |
| U-20 | terminada | `c7389c7` | La pestaña sigue `job.kind` al reanudar `?job=`. Dos pruebas de navegador en `ingest.test.js` |
| U-21 | terminada | `6d0c7c3` | `?job=` solo con forma de UUID; `jobUrl()` codifica el id en cada ruta. Dos pruebas de navegador en `ingest.test.js` |

## Cambios sin commit

Vacío.

## Última verificación

- **Comando:** `./mvnw verify -Pit` (con `.env`) y, en `elastiflix-web/src/test/browser`, `npm test` con la
  aplicación en el puerto 8089
- **Resultado:** en verde: 635 unitarias (core 78, search 76, ingest 214, web 267) y 32 de integración (core 10,
  search 14, ingest 2, web 6), 0 fallos, 0 omitidas; Checkstyle 0 y PMD 0. Navegador 15 de 15 (con el árbol de
  `8393a27`, antes del arreglo del chip de página, que solo cambió dos claves del catálogo)
- **Fecha:** 2026-10-21 20:28–20:30 UTC, con el árbol de `8e829f0`

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
- 2026-10-21 (U-01, U-24): la medición usó una IT temporal (`MeasureTmpIT`, no versionada) con las mismas
  estrategias de búsqueda que la aplicación. El código de búsqueda no cambió desde la revisión 3, así que la
  deriva de Life of Pi viene del servicio (inferencia o k-NN aproximado de Serverless). Los conteos léxicos de
  las sugerencias también eran del catálogo de 4 322 películas, en los dos idiomas: se corrigieron con la
  misma medición. PLAN §1 registra que las etiquetas ya no siguen los valores históricos de los mockups.
- 2026-10-21 (U-07): texto de `EFX-SRCH-012`, según el objetivo que fijó Adolfo el 2026-10-19 («un error
  controlado que le dice que su consulta falló»): «Tu búsqueda falló y la ficha de la película no se puede
  abrir · El enlace no trae una búsqueda válida: falta la consulta o algún ajuste no es válido · Vuelve a
  Inicio, busca la película y ábrela desde los resultados», y su versión en inglés. Al principio el estado de
  error no se agregó a los mockups `Detail*.dc.html`, porque la página usa la plantilla de error común (T-20).
  *Corrección:* el carril de documentación lo agregó después (`0b113fb`): `Detail.dc.html` y `Detail-en.dc.html`
  tienen la variante «estado» › «Error EFX-SRCH-012 (pendiente de aprobación)», que reproduce la página de error
  común con los textos vigentes. Espera la aprobación de Adolfo.
- 2026-10-21 (U-13, U-17): la corrección de fondo de la traducción de la carga no cambia `ValidationIssue`
  (sus textos siguen en español, para los logs, el informe en español y el JSON que lee `ingest.js`). En su
  lugar, cada frase fija vive en un vocabulario de ingest, `IngestPhrase`, que sabe escribirla y reconocerla, y
  la web traduce por su clave. Así no hay copias de las frases en la web, el contrato JSON no cambia y dos
  pruebas lo vigilan: `IngestPhraseTest` (cada frase se reconoce a sí misma, sin plantillas repetidas) e
  `IngestTranslatorTest` (cada frase tiene clave en los dos catálogos y la salida real sale en inglés). Se
  borraron `ValidationIssueTranslator`, `CsvColumnTranslator` y `EnglishJobTexts`. `IngestJobController`,
  `IngestJobErrors` y `UploadSizeErrors` ya no aceptan `UiMessages` opcional (parte de U-18).
- 2026-10-21 (U-14, U-15): «ES»/«EN» del selector y el placeholder `es, en, ja…` siguen fijos a propósito: son
  nombres y códigos de idioma, iguales en el mockup en inglés. La prueba nueva encontró además un falso
  positivo propio (un `th:text` con otro elemento dentro) que se corrigió en la prueba, no en la plantilla.

- 2026-10-21 19:22 UTC: Adolfo pidió «Si, haz el PR y fusiona la rama.». Se abrió el PR #25 (`revision-4` →
  `main`) y se fusionó con el merge commit `b54d67c`.
- 2026-10-21: Adolfo pidió «Sí, borra la rama revision-4 en local y en el remoto.». Se borró en los dos lados
  después de comprobar que no tenía commits fuera de `main`.

- 2026-10-21: Adolfo pidió «Trabaja y resuelve todos los puntos de @docs/revisiones/revision-4.md». Se
  trabaja en la rama `revision-4-resto` con cuatro carriles en paralelo (worktrees de Claude Code):
  **detalle** (U-04, U-05, U-06, U-08 enlace, U-09, U-10, U-12 y U-22 del detalle, U-18 del detalle, U-23 del
  detalle), **web e idioma** (U-08 selector, U-12, U-18, U-19, U-22, U-23, U-33 `package-info`), **carga**
  (U-12 de carga, U-18 `data-alias`, U-20, U-21, U-25 a U-29, U-31 de ingest, U-32) y **núcleo y búsqueda**
  (U-30, U-31 de core y search). Los carriles no escriben `progreso/`, PLAN, README ni `docs/`; la
  documentación (U-07 estado del mockup, U-34 a U-38) va al final, sobre el código integrado.
- 2026-10-21: los cinco carriles (detalle, web e idioma, carga, núcleo y búsqueda, y documentación) se integraron
  en `revision-4-resto` con los merges `2c3ca5f`, `75d86aa`, `8f065a1`, `d0ef738` y `8393a27`, más los cruces de
  `a4aef6b` y `3d04eb1`. Después se alinearon PLAN, README, los diseños 1, 3, 7 y 8, la arquitectura, el glosario y
  este archivo con el código integrado.
- 2026-10-21, decisiones del integrador (registradas aquí para que Adolfo las revise en el PR):
  1. **`detail.chip.pageFormat` sin el total N.** El chip «Página» del detalle muestra «2 · 50 por página». El
     mockup pide «[n] de [N] · [25] por página», pero el detalle no conoce el total de páginas sin agregar un
     parámetro al contrato de PLAN §5. Antes repetía la palabra («Página Página…»).
  2. **`EncodedSlashConfig` para los ids con `/`.** En lugar de prohibir `/` en los ids (el registro individual
     acepta cualquier texto), el enlace codifica el id como un solo segmento (`/pelicula/a%2Fb`) y Tomcat deja
     pasar `%2F` (`encodedSolidusHandling=passthrough`).
  3. **`spring-boot-web-server` opcional en core.** `CatalogBootstrapRunner` pregunta
     `context instanceof WebServerApplicationContext`, y en Spring Boot 4 esa interfaz vive en ese artefacto. Se
     declara opcional y sin versión (BOM de Boot) para que search e ingest no la hereden; registrada en el diseño 1
     y en PLAN §3, como pide `docs/convenciones-java.md` §12.
  4. **Tope de 10 000 errores en el informe descargable.** Cada trabajo guarda como mucho 10 000 errores
     (U-25), así que el informe CSV trae como mucho esos; la página muestra el total real.

## Resumen de diseño

La revisión 4 no tiene un diseño propio: la especificación es `docs/revisiones/revision-4.md` y cada cambio se
registró en «Cambios respecto al diseño aprobado» del diseño de su fase.

- **Núcleo (diseño 1):** `EFX-EMB-002` también para un valor o una norma no finitos; `CatalogBootstrapRunner`
  pregunta al contexto si hay servidor web, con `spring-boot-web-server` opcional en core; `findById` solo da vacío
  con `found=false`.
- **Búsqueda (diseño 2):** `EmptyState` con tipo, `ComponentScore(score, rank, k)` y `Pagination(first, last)`;
  tabla vectorial de §6 medida de nuevo.
- **Carga (diseño 3):** vocabulario `IngestPhrase` para traducir por clave; tope de 10 000 errores por trabajo
  (`KeptReport`); límites en bytes de `EFX-ING-012` (`KeywordBytes`); rutas completas en los subcampos; cancelación
  entre etapas, limpieza ante un `create` dudoso y huérfanos que no detienen la importación.
- **Detalle (diseño 7):** `PosterUrls` como único constructor de URL de imágenes; «N/A» en todos los campos vacíos;
  ids en un solo segmento con `EncodedSlashConfig`; detalles del mockup (fondo, reparto sin JavaScript,
  `detail.quoted`, chip «Ajustes»); una sola validación con `Optional`; chip «Página» sin el total.
- **Idioma (diseño 8):** selector que solo copia la URL y codifica una vez; cookie `HttpOnly`, `SameSite=Lax` y
  tolerante a valores ilegibles; un solo `UiMessages` (el bean); cifras escritas con `Thousands` antes de
  `MessageFormat`; textos del mockup en inglés y «Créditos» completo.

No se agregaron patrones ni códigos `EFX` nuevos. Clases nuevas de producción en toda la revisión: `IngestPhrase`,
`PhraseMatch`, `KeptReport`, `KeywordBytes` y `TextListValues` (ingest); `ExternalLinks`, `SuggestionTags` y
`EncodedSlashConfig` (web), más los `package-info.java` de la web.

## Revisión de convenciones

Respuestas a `docs/convenciones-java.md` §16:

- [x] **¿Diseño previo y aprobado?** No aplica como fase: la revisión trabaja sobre una lista
  (`docs/revisiones/revision-4.md`) y las decisiones de Adolfo registradas en «Decisiones». Los cambios de cada fase
  quedaron en «Cambios respecto al diseño aprobado» de los diseños 1, 3, 7 y 8 (y en el diseño 2 para U-01 y U-16).
- [x] **¿El código coincide con el diseño?** Sí, con los cambios registrados en esas secciones. Quedan dos
  desviaciones visibles a propósito: el chip «Página» sin el total N (mockup) y la variante de error del mockup del
  detalle, que espera aprobación.
- [x] **¿`./mvnw verify` con 0 violaciones y arquitectura en verde?** Cada carril cerró con `./mvnw -B verify` en
  verde (Checkstyle 0, PMD 0, ArchUnit en verde). La corrida sobre el árbol integrado la registra el integrador en
  «Última verificación». `config/` no cambió.
- [x] **¿Una responsabilidad por clase?** Sí. Las clases nuevas se dicen en una frase: `KeptReport` guarda el
  recorte del informe y sus cifras; `KeywordBytes` sabe el tope en bytes de cada `keyword`; `TextListValues` valida
  listas de textos; `EncodedSlashConfig` deja pasar `%2F` en Tomcat.
- [ ] **¿Solo patrones de la lista cerrada?** La revisión no agregó ninguno. Sigue abierto el «View Helper /
  ViewModel» que declaró el diseño 7 (fase 7), pendiente de la decisión de Adolfo (U-38).
- [x] **¿Cada error nuevo con código `EFX` y sus tres partes?** No hay códigos nuevos. `EFX-ING-012` y
  `EFX-EMB-002` cubren casos nuevos con su texto de siempre; `EFX-SRCH-012` tiene texto nuevo (U-07) en `ErrorCode`
  y en los dos catálogos.
- [x] **¿Logs sin secretos, vectores ni documentos?** Sí. Los logs nuevos o cambiados son el `WARN` de un huérfano
  que no se pudo borrar (nombre del índice y código) y el `WARN` del detalle sin búsqueda válida (código e `id`, sin
  los parámetros de la consulta).
- [x] **¿Supresiones justificadas y registradas?** Solo dos `@SuppressWarnings("unchecked")` nuevos, en pruebas de
  `ElasticsearchCatalogIndexTest` (U-06), con el comentario de siempre; registrados en el diseño 7. Ninguna en
  producción.
- [x] **¿IT de escritura con alias propio que se borra?** Sí. `CatalogIndexIT` escribe en su propio alias y ahora
  intenta borrar todos sus índices aunque falle uno; `SmokeIT` solo lee `elastiflix-reloaded`.
- [x] **¿Un Junior entendería las clases principales?** Sí: clases pequeñas con Javadoc en español que dice el
  porqué (por ejemplo, el de `EncodedSlashConfig` y `JobRetention.MAX_KEPT_ERRORS`).

## Bloqueos y solicitudes

Sin bloqueos. Esperan a Adolfo:

- aprobar las 22 capturas de `docs/capturas/` (U-36);
- aprobar la variante «Error EFX-SRCH-012» de `Detail.dc.html` y `Detail-en.dc.html` (U-07);
- decidir sobre el patrón «View Helper / ViewModel» del diseño 7 (U-38);
- revisar las cuatro decisiones del integrador (ver «Decisiones») en el PR.
