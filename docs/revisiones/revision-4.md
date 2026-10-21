# Revisión 4 — Elastiflix-Movies Java (borrador para revisar)

> Cuarta revisión exhaustiva de `main` en `6431aa6` (2026-10-21 UTC). Revisa el trabajo del agente de Google
> Antigravity (fase 7, detalle de película, PR #23; fase 8, i18n, PR #24) y vuelve a pasar por todo el
> código y la documentación.
>
> Cuando se escribió, esta lista no tenía cambios aplicados: ninguna corrección se aplicaba hasta que Adolfo diera
> instrucciones. Cada punto se verificó leyendo el código; los marcados con ✔ se reprodujeron además
> ejecutando código o pruebas.
>
> **Origen:** F7 = fase 7, F8 = fase 8 (las dos de Antigravity); Previo = fases 0–6 o revisiones 1–3.
>
> **Avance (2026-10-21):** todos los puntos están resueltos. U-03 y U-11 entraron directo en `main`; U-01, U-02,
> U-07, U-13 a U-17 y U-24, con el PR #25 (`b54d67c`); el resto, en la rama `revision-4-resto` (cinco carriles en
> paralelo, ya integrados), con el PR #26 (`c9be924`). La parte de cabeceras de U-33 se descartó. Adolfo aprobó el
> 2026-10-21 las capturas nuevas (U-36), la variante de error de los mockups del detalle (U-07), el patrón «View
> Helper / ViewModel» del diseño 7 (U-38) y la dependencia opcional `spring-boot-web-server` en core (U-30). El
> estado, la evidencia y las decisiones de cada punto están en `progreso/revision-4.md`; esta lista no se marca.

## Resumen

- **Sincronización:** `main` local ya coincidía con `origin/main` (`6431aa6`). `git fetch` no trajo commits
  nuevos y en el remoto solo existe `main`.
- **`./mvnw verify`:** en verde. 366 unitarias (core 68, search 71, ingest 123, web 104), Checkstyle 0, PMD 0,
  ArchUnit en verde. `config/` sigue intacto desde `4d60bc0` y no hay supresiones en el código de producción.
- **`./mvnw verify -Pit`:** **una falla** (U-01). Hay 30 IT; las otras 29 pasan.
- **Veredicto sobre el trabajo de Antigravity:** la base funciona. La ficha cubre los 30 campos del mapping,
  el regreso a resultados conserva los parámetros, el idioma se resuelve como pidió Adolfo y los catálogos
  tienen paridad de claves. Pero dejó cabos sueltos:
  - **Una regresión visible en español** (U-11) y **una brecha de seguridad** (U-03).
  - **La interfaz en inglés de «Cargar datos» sigue mostrando español** en varios puntos (U-13, U-14).
  - **Afirma en `progreso/` y PLAN cosas que no se hicieron:** verificación `-Pit`, «cero textos fijos»,
    clases internacionalizadas que no se tocaron, cifras de pruebas.
  - **Borró una fila de «Aprobaciones»** y no registró otra (U-34).
  - **Dejó sin actualizar** `AGENTS.md`, `CLAUDE.md`, PLAN §5, README y la arquitectura.
- **Lo previo** también tiene cabos sueltos, en especial U-24 (posiciones de las sugerencias en español) y
  U-25 (memoria del informe de errores).

| Gravedad | Cantidad | Puntos |
|---|---|---|
| Crítica | 0 | — |
| Importante | 17 | U-01, U-02, U-03, U-05, U-07, U-11, U-13, U-14, U-15, U-16, U-20, U-24, U-25, U-34, U-35, U-36, U-37 |
| Menor | 21 | el resto |

## Decisiones que necesito de ti

1. **U-01:** ¿vuelvo a medir la tabla de §6 sobre el catálogo actual y actualizo PLAN, `docs/diseno/2.md`,
   `progreso/` y las IT? ¿O primero investigo por qué cambió?
2. **U-07:** ¿qué texto quieres para `EFX-SRCH-012`, el del diseño aprobado o el del código? Además, el estado
   de error del enlace directo falta en los mockups `Detail*.dc.html` y necesita tu aprobación.
3. **U-13, U-16, U-17:** ¿corrección de fondo, en la que `ValidationIssue`, `EmptyState` y los demás exponen
   códigos y la web traduce por clave? ¿O parches puntuales sobre el traductor actual? Recomiendo la de fondo:
   la actual se rompe en silencio con cualquier cambio de redacción.
4. **U-02:** las pruebas de navegador necesitan `npm install` (Playwright 1.49.1) y descargar Chromium.
   ¿Lo instalo y las corro?
5. **U-24:** ¿vuelvo a medir las posiciones de las sugerencias en español sobre el catálogo actual?
6. **U-33:** ¿agrego cabeceras de seguridad mínimas, o se quedan fuera por ser una demo?
7. **Organización:** ¿un solo agente en orden, o carriles paralelos como en la revisión 3?

---

## A. Verificación y pruebas

### U-01 — `VectorSearchIT` falla: Life of Pi queda en 31.º y no en 30.º en `plot_256` ✔
- **Gravedad:** importante. **Origen:** externo (ver abajo).
- **Dónde:** `elastiflix-search/src/test/.../strategy/VectorSearchIT.java:118`; tabla de PLAN §6 (l. 348) y
  `docs/diseno/2.md:290`.
- **Problema:**
  - Falló igual en tres corridas seguidas: `expected: 30 but was: 31`. `_count` sigue en 6 899, así que
    `assumeTrue` no la omite.
  - El código de las estrategias no cambió desde la revisión 3, cuando `-Pit` pasó en verde. El cambio viene
    del servicio (la inferencia o el k-NN aproximado de Serverless), no de las fases 7 y 8.
  - Ningún agente lo detectó porque nadie corrió `-Pit` completo desde la revisión 3 (U-02).
- **Corrección:** decisión 1. O se vuelve a medir y se actualizan la tabla y la IT, o se investiga la deriva.
- **Criterio:** `./mvnw verify -Pit` en verde, con la tabla de §6 igual en PLAN, `diseno/2.md`, `progreso/5.md`
  y la IT.

### U-02 — Las fases 7 y 8 no corrieron la verificación que piden PLAN y la Definición de Hecho
- **Gravedad:** importante. **Origen:** F7, F8.
- **Problema:**
  - La fase 7 solo registra `./mvnw clean verify`, aunque agregó una IT
    (`CatalogIndexIT#shouldFindMovieByIdExcludingVectors`).
  - La fase 8 registra `./mvnw verify -Pit -pl elastiflix-web` (solo `SmokeIT`, y sin `-am` usa core, search
    e ingest de `~/.m2`). La tarea 8.7 pedía `-Pit` completo.
  - Las pruebas de navegador (`elastiflix-web/src/test/browser`) no se corrieron después de que la fase 8
    reescribió `ingest.js` y `home.js`. `ingest.test.js` todavía busca textos fijos.
  - La Definición de Hecho (PLAN l. 1099) sigue marcada sin evidencia vigente.
- **Corrección:** después de U-01, correr `./mvnw verify -Pit` completo y las pruebas de navegador (decisión
  4). Registrar las cifras reales (U-35).

---

## B. Detalle de película (fase 7)

### U-03 — `homepage` se pinta como enlace sin validar el esquema: `javascript:` se ejecuta al hacer clic
- **Gravedad:** importante (seguridad). **Origen:** F7.
- **Dónde:** `templates/detail.html:70` (`th:href="${detail.homepage}"`) y las filas de `FactRow.link`
  (`MovieDetailTables.java:63-64`, `detail.html` l. 86 y siguientes).
- **Problema:**
  - `homepage` es un `keyword` libre. Puede llegar por un archivo o por el formulario, y nada valida su
    formato.
  - Thymeleaf escapa el atributo pero no filtra esquemas, así que `"homepage": "javascript:alert(1)"` se
    ejecuta al hacer clic.
  - `rel="noopener noreferrer"` no lo evita.
- **Corrección:** aceptar solo `http` y `https` (`URI` + `getScheme()`, dentro de un `try`). Cualquier otro
  valor se trata como vacío: «N/A» y sin botón.
- **Criterio:** una prueba de `MovieDetailPage` con `javascript:` y otra con `https://…`.
- **Estado:** aplicada, sin commit. `ExternalLinks.webOrEmpty` alimenta el botón y la fila de la ficha;
  `ExternalLinksTest` (16 casos) y dos pruebas nuevas en `MovieDetailPageTest`.

### U-04 — `backdrop_path` se interpola dentro de CSS en `style`
- **Gravedad:** menor (seguridad). **Origen:** F7.
- **Dónde:** `detail.html:30`, `th:style="|background-image: url('${detail.backdropUrl}');|"`.
- **Problema:** una ruta con `')` permite inyectar CSS (por ejemplo, cargar recursos externos). No ejecuta
  JavaScript.
- **Corrección:** antes de componer la URL, validar que la ruta cumpla `^/[\w.-]+$`. Si no, mostrar el
  marcador. Aplicar la misma validación a `poster_path`.

### U-05 — La regla «vacío → N/A» no se cumple en varios campos, y la página afirma que sí
- **Gravedad:** importante. **Origen:** F7.
- **Dónde:** `MovieDetailTables.java:190-211`, `MovieDetailPage.java:65-67` y `detail.html:152-187`.
- **Problema:**
  - `vote_average` y `popularity` nulos se muestran como «—», no como «N/A». `vote_count` nulo se muestra como
    «0 votos».
  - `overview` y `plot` vacíos dejan un `<p>` vacío.
  - `cast` y `keywords` vacíos muestran «0 personas» y una lista vacía. `genres` vacío desaparece.
  - `detail.disclaimer` dice «Un campo sin valor aparece como «N/A»», y eso es falso.
- **Corrección:** los formateadores devuelven `null` cuando no hay valor, para que `FactRow.text` ponga
  «N/A». Las listas y textos vacíos muestran «N/A».
- **Criterio:** `MovieDetailPageTest` con un `Movie` vacío afirma «N/A» en cada uno de esos campos.

### U-06 — Un alias o índice inexistente se muestra como «película no encontrada»
- **Gravedad:** menor. **Origen:** F7.
- **Dónde:** `elastiflix-core/.../catalog/ElasticsearchCatalogIndex.java:202-205`, y la prueba que lo fija en
  `ElasticsearchCatalogIndexTest.java:137-152`.
- **Problema:**
  - El `catch` convierte cualquier 404 en `Optional.empty()`.
  - Con el cliente 9.4.5, un documento inexistente ya llega como `found=false`. Ese `catch` solo atrapa
    errores reales, como `index_not_found_exception`.
  - PLAN pide la página de error EFX cuando Elasticsearch responde con error.
- **Corrección:** quitar el caso especial y lanzar `failure(...)`. Ajustar la prueba.

### U-07 — `EFX-SRCH-012` no usa el texto aprobado, y el mockup nunca recibió el estado de error
- **Gravedad:** importante (proceso). **Origen:** F7.
- **Dónde:** `ErrorCode.java:173-176`, `messages_*.properties` (l. ~131), `docs/diseno/7.md:29`,
  `design/mockups/Detail.dc.html` y `Detail-en.dc.html`.
- **Problema:**
  - El diseño aprobado fija «Para ver el detalle de una película debes buscarla primero · …».
  - El código dice «La consulta de búsqueda no es válida o está ausente · …», y no hay registro del cambio.
  - PLAN (l. 1254-1255) pedía agregar ese estado a los mockups para tu aprobación. Los mockups no tienen
    ningún estado de error.
- **Corrección:** decisión 2. Alinear el código, los dos catálogos y el diseño. Agregar el estado a los dos
  mockups y registrar tu aprobación.

### U-08 — Los ids con `/` o con caracteres no ASCII rompen los enlaces
- **Gravedad:** menor. **Origen:** F7, F8.
- **Dónde:**
  - `SearchLinks.java:58`: `fromPath("/pelicula/" + id)` no codifica `/`;
  - `LanguageSwitchLinks.java:45-51`: vuelve a codificar la ruta ya codificada, y `/pelicula/caf%C3%A9`
    queda como `caf%25C3%25A9`, que da 404.
- **Problema:** el registro individual acepta ids libres, como `a/b` o `café`.
- **Corrección:** `fromPath("/pelicula").pathSegment(id)`. En el selector, decodificar la ruta antes de
  armarla, o usar `build(true)`.
- **Criterio:** pruebas con ids `a/b` y `café`, del enlace y del selector.

### U-09 — Detalles del detalle que no siguen el mockup ni PLAN
- **Gravedad:** menor. **Origen:** F7, F8.
- **Problema:**
  - **Fondo:** si la imagen de `backdrop_path` falla, no aparece ningún marcador (PLAN: «si no hay ruta o la
    imagen falla»).
  - **Póster:** el marcador que pone `detail.js:48` no lleva `mono-tag`, a diferencia del que pone el
    servidor.
  - **Reparto:** sin JavaScript, las personas 19 y siguientes quedan ocultas con `style="display:none"` y el
    botón no hace nada.
  - **Texto del botón:** dice «Ver menos»; el mockup dice «Mostrar menos» (`messages_es.properties:548`).
  - **Chip «Ajustes» de la híbrida:** solo muestra `RRF (60) · vector · k`. Faltan `tipo`, `operador` y
    `num_candidates` (`MovieDetailSearchChips.java:69`).
  - **Comillas en inglés:** usa «…» en la tagline (`detail.html:51`) y en el chip de la consulta
    (`MovieDetailSearchChips.java:43`). El glosario y `Detail-en.dc.html` usan “…”.
- **Corrección:** ajustar cada punto. Mover las comillas a claves del catálogo.

### U-10 — Validación repetida y retornos `null` en el controlador del detalle
- **Gravedad:** menor. **Origen:** F7.
- **Dónde:** `MovieDetailController.java:62,108-119`, `MovieDetailPage.java:89-94` y
  `MovieDetailTables.java:40-41`.
- **Problema:**
  - `mapper.toQuery(form)` ya valida. El controlador vuelve a validar con otra instancia de
    `SearchQueryValidator`.
  - `validForm` devuelve `null`, y las convenciones piden `Optional`.
  - El enlace a IMDb se arma en dos lugares.
- **Corrección:** quedarse con `mapper.toQuery`, devolver `Optional<SearchForm>` y crear un solo helper para
  IMDb.

---

## C. Interfaz en español e inglés (fase 8)

### U-11 — Regresión en español: el «o» entre valores de un filtro queda pegado ✔
- **Gravedad:** importante. **Origen:** F8.
- **Dónde:** `messages_es.properties:476` (`results.chip.or= o `), `messages_en.properties:476` y
  `AppliedFilters.java:82-83`.
- **Problema:**
  - `Properties` descarta los espacios que siguen al `=`, así que el separador queda como `"o "`.
  - Con dos géneros, la ficha dice «Dramao Comedia» (en inglés, «Dramaor Comedy»). Antes de la fase 8 decía
    «Drama o Comedia».
  - Se ve cada vez que hay dos o más géneros, idiomas o clasificaciones marcados.
- **Corrección:** escribir el valor como `\ o \ ` en el catálogo, o unir con `" " + or.strip() + " "` en
  Java.
- **Criterio:** `ResultsPageTest` con dos géneros afirma «Drama o Comedia» y «Drama or Comedy».
- **Estado:** aplicada, sin commit. El catálogo guarda solo `o` / `or` y `AppliedFilters` pone los espacios;
  la prueba quedó en `AppliedFiltersTest` (`ResultsPageTest` ya pasa de 400 líneas).

### U-12 — `MessageFormat` agrupa los enteros: «k = 1.000» en español ✔
- **Gravedad:** menor (regresión). **Origen:** F8.
- **Dónde:**
  - `ModeCopies.java:53,64` (`results.mode.vector.limit` y `results.mode.hybrid.limit` reciben `int k`);
  - `detail.cast.meta` y `detail.cast.showMore` (`castCount`);
  - `ingest.step4.detail` y `about.load.imp.temp.dd` (`batchSize`).
- **Problema:** en español sale «1.000» y el proyecto usa «1 000». Antes de la fase 8 salía «1000».
- **Corrección:** pasar `Thousands.of(n, locale)` como `String`, o usar `{0,number,#}`.

### U-13 — En inglés, «Cargar datos» sigue mostrando validaciones y avisos en español
- **Gravedad:** importante. **Origen:** F8. Ver decisión 3.
- **Dónde:** `ValidationIssueTranslator.java:44-81`, `IngestTranslator.java:85-86,140-151`,
  `IngestController.java:63-71`, `ingest.html:18` y `UploadSizeErrors.java:41-42`.
- **Problema:** el traductor compara frases en español contra una lista cerrada que no coincide con lo que
  emite `elastiflix-ingest`.
  - **`expected` sin traducir:** «un id único», «un número float finito», «una lista de textos», «un objeto o
    null», «una lista de {id, name}», «un objeto {id, name}», «un nombre del mapping», «un nombre único»,
    «N celdas», y los `summary()`/`userMessage()` completos.
  - **`received` sin traducir:** «no vino», «en blanco», «faltan id, title o plot», «nombre repetido».
  - **Etiquetas sin traducir:** «archivo», «formulario», «encabezado». A la vez traduce «Fila » y
    «Registro », que el módulo nunca emite.
  - **Advertencia sin traducir** ✔: el traductor busca «se omitió», pero ingest emite «se omite». Sale
    «genres: se omite (3).».
  - **Registro individual:** las incidencias del flash no pasan por ningún traductor.
  - **`informe.csv` en inglés:** solo cambia el encabezado. `IngestPageTest.java:354` lo deja fijado con
    `"Record 1,1,title,,un texto"`, una etiqueta que el módulo no genera.
  - **413** ✔: `UploadSizeErrors` responde con `ErrorCode.FILE_TOO_LARGE.summary()` en español.
  - **Columnas CSV:** «vacía en N filas» usa los miles del español también en inglés.
- **Corrección de fondo:** `ValidationIssue` lleva un código y argumentos (tipo esperado, motivo, etiqueta),
  y la web los resuelve con `messages_*.properties`. El informe CSV se genera desde las incidencias ya
  traducidas.
- **Criterio:** pruebas en inglés con informes reales de `MappingValidator` y `CsvMovieReader` (no con datos
  inventados). Ninguna celda ni aviso contiene texto en español.

### U-14 — Textos fijos en español en plantillas y JS, y una regresión de concordancia
- **Gravedad:** importante. **Origen:** F8 (el `aria-label` y «Campo para» son de antes, pero la fase 8 debía
  quitarlos).
- **Problema:**
  - `ingest.html:208` ✔: `aria-label="Progreso de la importación"`. El mockup en inglés dice «Import
    progress».
  - `ingest.js:507`: `"Campo para " + column.column`.
  - `ingest.html:191`: «Mostrando 0 de 0 errores.» no tiene `th:text`.
  - `ingest.js:19-39`: cada `data-msg-*` tiene un respaldo en español («No se pudo conectar…», «La extensión
    no es {0}.», «EFX-ING-005 · …»).
  - **Regresión en español** ✔: los cinco placeholders del registro individual usan «Separados por comas»
    (`ingest.html:47,48,76,77,78`). Antes, y en el mockup, decían «Separadas» en palabras clave y productoras
    y «Separado» en reparto.
- **Corrección:** crear claves para cada texto y una clave por concordancia, y quitar los respaldos.

### U-15 — `TemplatesAndScriptsI18nTest` casi no puede fallar
- **Gravedad:** importante (pruebas). **Origen:** F8.
- **Problema:**
  - En las plantillas, solo exige que el archivo contenga `th:`.
  - En JS, busca una lista negra de 19 frases.
  - Por eso no detectó U-14. Aun así, la tarea 8.7 afirma «cero literales cableados en JS».
- **Corrección:**
  - Plantillas: fallar si un nodo de texto, `aria-label`, `title`, `alt` o `placeholder` estático, fuera de un
    `th:text`, contiene `[áéíóúñ¿¡]` o una palabra en español.
  - JS: fallar ante cualquier literal visible no ASCII y ante los respaldos `||`.

### U-16 — La web deduce estados comparando frases en español de search e ingest
- **Gravedad:** importante (diseño). **Origen:** F8. Ver decisión 3.
- **Dónde:**
  - `ResultsPage.java:187` ✔ (`"Ningún resultado cumple los filtros".equals(raw.title())`);
  - `ResultCards.java:132` (copia `"fuera de su top "` en lugar de usar `Rrf.OUTSIDE_PREFIX`, y saca `k` con
    `substring`);
  - `ResultsPage.java:168-178` (rehace en inglés el rango que `Paginator.range()` arma en español);
  - `EnglishJobTexts.java:106` (`"Not in the mapping"`);
  - `ingest.js:521-528` (`statusTone` compara «Asignada» y «Mapped»).
- **Problema:** cambiar una tilde en search o ingest rompe la versión en inglés sin que falle ninguna prueba.
- **Corrección:**
  - `EmptyState` con un tipo (`NONE`/`FILTERED`);
  - `ComponentScore` con `Integer rank` y `k`;
  - `Pagination` con `start` y `end` numéricos;
  - estados de columna como códigos.
  - La web arma los textos desde el catálogo.

### U-17 — Textos de la interfaz escritos en Java y elegidos con `if (isEnglish)`
- **Gravedad:** menor (diseño). **Origen:** F8 (las sugerencias, de antes).
- **Dónde:**
  - `JobTexts.java` y `EnglishJobTexts.java` (174 líneas que duplican lógica);
  - `CatalogTexts.java` («Conectado/Connected», «Índice:/Index:», «Tiene N películas/It has N movies»);
  - `ResultsPage.java:173-176` (`" of "`);
  - las etiquetas de `*Suggestions.java` («1.º lugar», «1st place», «fuera del top 10»).
- **Problema:** PLAN pide los textos en `messages_*.properties`. La prueba de paridad no los vigila, y cada
  texto nuevo obliga a tocar dos clases.
- **Corrección:** claves con argumentos y plurales, y una sola `JobTexts` que use `UiMessages`.

### U-18 — Singleton manual, inyección opcional y código muerto alrededor de `UiMessages`
- **Gravedad:** menor. **Origen:** F7, F8.
- **Problema:**
  - **Singleton manual:** `UiMessages.DEFAULT_INSTANCE` (`:18,37`) es un segundo `MessageSource`, fuera de
    Spring, que se usa en producción: `ErrorPages:29`, `ErrorPageController:45`, `AppliedFilters:25,53`,
    `ModeCopies:25`. Las convenciones §4 lo prohíben.
  - **Inyección opcional:** `@Autowired(required = false)` en `IngestJobController:58-64` e
    `IngestJobErrors:39`, con respaldos a `summary()` en español. Además, `LayoutAdvice` repite
    `@Import(LocaleConfig.class)`.
  - **Ramas muertas:** todas las `msg(messages, …, "respaldo en español")` del detalle, y
    `MovieDetailSearchChips:77-90` cuando `messages == null`.
  - **Sobrecargas sin uso:**
    - `MovieDetailTables.facts/production/numbers/collection/images/voteCount` de un argumento;
    - `FactRow.money` y `yesNo` de 3 argumentos, y `MovieDetailSearchChips.chips(SearchForm)`;
    - `ResultCards.of` (2 y 3 argumentos), `ModeCopies.of(query)` y `AppliedFilters.note(form)`;
    - `CatalogTexts.indexLine/disconnected/moviePhrase` de un argumento y `PageShells.from(status)`;
    - `UiMessages.errorUserMessage`.
  - **Claves sin uso:** `results.query.error`, `results.adjust.mode.*`, `detail.poster.defaultAlt` e
    `ingest.job.th.row`.
  - **Atributos y variables sin uso:** `data-alias` en `ingest.html:96` y `var total` en `detail.js:11`.
- **Corrección:** inyectar el bean en todos lados, hacer obligatoria la inyección y borrar lo muerto.

### U-19 — El selector de idioma copia los campos de un POST a la URL, y una cookie inválida da 500
- **Gravedad:** menor. **Origen:** F8.
- **Dónde:** `LanguageSwitchLinks.java:53-57` y `LocaleConfig.java:55-61,80-90`.
- **Problema:**
  - El selector usa `getParameterMap()`, que incluye el cuerpo del formulario. Si `POST /cargar/registro`
    falla, la página de error enlaza a `GET /cargar/registro?id=…&plot=…&lang=en`, que responde 405 y además
    pone los datos en la URL.
  - Con `rejectInvalidCookies` en `true` (el valor por defecto), una cookie como `elastiflix-lang=e$` hace que
    todas las páginas respondan 500. El diseño §3 dice que se normaliza.
  - La cookie no lleva `HttpOnly`, aunque ningún JS la lee.
- **Corrección:**
  - tomar solo los parámetros de `getQueryString()`, y enlazar a `/cargar` si la petición no es `GET`;
  - `setRejectInvalidCookies(false)` y `setCookieHttpOnly(true)`.

### U-20 — Al reanudar un trabajo CSV después de cambiar de idioma, se activa la pestaña JSON
- **Gravedad:** importante (funcional). **Origen:** F8.
- **Dónde:** `ingest.js:581-594` ✔ (`tab = "json"` fijo cuando la URL trae `?job=`).
- **Problema:**
  - La pantalla muestra «Import .json» junto a la tarjeta «Column mapping» del CSV.
  - El selector de separador queda oculto.
  - Esto rompe el escenario que el diseño 8 §5 definió precisamente para el cambio de idioma.
- **Corrección:** elegir la pestaña según `job.kind` en el primer `paint()`.
- **Criterio:** prueba de navegador que cambia de idioma con un trabajo CSV activo.

### U-21 — `?job=` se usa sin validar en las rutas de `fetch`
- **Gravedad:** menor. **Origen:** F8.
- **Dónde:** `ingest.js:595-600`, `361`, `379` y `413`.
- **Problema:** el valor se concatena en `"/cargar/trabajos/" + current + "/" + path`, así que un `../` apunta
  a otra ruta del mismo origen.
- **Corrección:** validar que sea un UUID (el servidor usa `UUID.randomUUID()`) y usar `encodeURIComponent`.

### U-22 — Traducciones que no siguen el mockup o el glosario, y «Créditos» incompleto
- **Gravedad:** menor. **Origen:** F8.
- **Problema:**

  | Clave | Hoy | Según el mockup o el glosario |
  |---|---|---|
  | `home.query.error`, `error.EFX-SRCH-001.summary` | Enter a movie to search for before pressing Search | Type the movie you're looking for before you press Search. |
  | `detail.fact.id` | Identifier | Id |
  | `detail.cast.showMore` | Show full cast ({0}) | See the full cast ({0}) |
  | `error.EFX-ES-001.summary` | Could not connect to Elasticsearch | Couldn't connect to Elasticsearch |
  | `detail.chip.pageFormat` (los dos idiomas) | Page {0} · {1} per page | {0} of {1} · {2} per page |

  - **«Acerca de» › «Créditos»** (`about.html:238-243`): falta la frase inicial «Movie data and poster images
    come from TMDB…» y su traducción. Los dos avisos no llevan `lang="en"` ni `lang="es"`. Era uno de los tres
    ajustes que aprobaste.
  - **Consulta de ejemplo de Resultados en inglés** («titanic love story on a sinking ship»): ni el código ni
    el diseño registran sus valores verificados, como pedía PLAN.

### U-23 — Faltan pruebas del selector, del inglés y del detalle
- **Gravedad:** menor. **Origen:** F7, F8.
- **Problema:**
  - `LocaleSwitchTest:91-104` solo usa valores ASCII. No cubre `/pelicula/{id}`, `/cargar?job=`, la página
    de error ni la 404 por MockMvc. Tampoco prueba `lang=EN`, `lang=` vacío ni los atributos de la cookie.
  - Ninguna prueba en inglés cubre las advertencias, los errores ni las columnas de la ingesta.
  - `MessageCatalogParityTest` no compara los `{n}` entre idiomas (hoy coinciden).
  - El regreso a resultados no se prueba con filtros multivalor, con los modos vectorial e híbrido, ni con
    `q = "a+b & c#d ñ"`.
  - No hay prueba del reparto con más de 18 personas, ni de «Elasticsearch falla → página EFX» en el detalle.
  - `SmokeIT` y `ServedPagesTest` no cubren `/pelicula/{id}` ni `lang=en`.
- **Corrección:** agregar esas pruebas.

---

## D. Núcleo, búsqueda y carga (en su mayoría, previo)

### U-24 — Las posiciones de las sugerencias vectoriales en español son del catálogo viejo ✔
- **Gravedad:** importante. **Origen:** previo (fase 4, Cursor).
- **Dónde:** `web/home/VectorSuggestions.java:83-125` (y conviene revisar también `HybridSuggestions` y
  `LexicalSuggestions` en español).
- **Problema:**
  - Inicio dice «Titanic: 4.º / 3.º / 1.º lugar», pero la tabla de §6 y `VectorSearchIT` miden 36.º / 7.º /
    4.º para la misma consulta, con `k=100` y 200 candidatos.
  - Las etiquetas vienen de la fase 4, que se midió sobre 4 322 películas, y nunca se recalcularon tras la
    importación de 6 899.
  - Las sugerencias en inglés sí se midieron en la fase 8.
- **Corrección:** decisión 5. Volver a medir y actualizar las etiquetas en español (después de U-01).

### U-25 — El informe de errores de cada trabajo se guarda entero, sin tope
- **Gravedad:** importante. **Origen:** previo.
- **Dónde:** `mapping/ValidationRun.java:18,24-30`, `job/ImportWork.java:26,145-150` y
  `job/JobRetention.java:22`.
- **Problema:**
  - Un CSV de 150 MB con un error por fila deja unas 200 000 incidencias en memoria (40-60 MB).
  - `JobStore` retiene 20 trabajos terminados, así que pueden ser cerca de 1 GB de heap.
  - `JobRetention` (S-06) existe justo para evitar esto, y no lo cubre.
- **Corrección:** un tope de incidencias por trabajo (las primeras N más un contador), o soltar el informe al
  expulsar el trabajo.

### U-26 — La validación acepta valores que Elasticsearch rechaza al escribir
- **Gravedad:** menor. **Origen:** previo.
- **Dónde:** `mapping/ScalarValues.java:31-60` y `NestedValues.java:50-60`.
- **Problema:** un `id` de más de 512 bytes, o un `keyword` de más de 32 766 bytes, pasan como «válido». La
  importación copia todo el catálogo y después falla con `EFX-IMP-001`.
- **Corrección:** comprobar la longitud en bytes y responder `EFX-ING-012`.

### U-27 — Un error en un subcampo oculta la falta de `id`, `title` o `plot`
- **Gravedad:** menor. **Origen:** previo.
- **Dónde:** `mapping/DocumentChecker.java:88-102`, `NestedValues.java:113,128`.
- **Problema:** con `{"title":"T","plot":"P","keywords":[{"id":"x"}]}`, el informe no dice «falta id».
  `reported()` encuentra el error del subcampo `id`, que es el de la palabra clave.
- **Corrección:** usar rutas como `keywords.id` y `belongs_to_collection.title`.

### U-28 — La cancelación y la limpieza tienen huecos antes de la copia
- **Gravedad:** menor. **Origen:** previo.
- **Problema:**
  - `FileIntake.java:103-117`: cancelar durante `validando` solo levanta la bandera. La validación sigue
    hasta el final y además consulta `existingIds`.
  - `ImportJob.java:79-86`: `createWithMapping(temp)` está fuera del `try`. Un timeout con el índice ya
    creado deja el temporal hasta la siguiente limpieza.
  - `OrphanCleaner.java:42-48`: si falla el borrado de un huérfano, se aborta toda la importación.
- **Corrección:** revisar `isCancelled()` entre etapas, borrar el temporal también ante un `create` dudoso, y
  que la limpieza de huérfanos solo registre un `WARN`.

### U-29 — Pequeños bordes en la carga
- **Gravedad:** menor. **Origen:** previo.
- **Problema:**
  - `ValidationReport.java:16`: `FORMULA = "=+-@"` no neutraliza `\t` ni `\r` (OWASP los incluye).
  - `ingest.html:117` ✔: `accept=".json,.ndjson,…"` ofrece `.ndjson`, que después se rechaza con
    `EFX-ING-001` (desde S-03).

### U-30 — Bordes en el núcleo
- **Gravedad:** menor. **Origen:** previo.
- **Problema:**
  - `VectorProjector.java:34`: solo comprueba `norm == 0`. Un `NaN` o un `Infinity` de la inferencia llega a
    Elasticsearch y se muestra `EFX-ES-007` en lugar de `EFX-EMB-002`. `InferenceEmbeddingService.toArray`
    tampoco revisa que los valores sean finitos.
  - `CatalogBootstrapRunner.java:398-401`: decide si hay servidor web leyendo una propiedad. Debería usar
    `context instanceof WebServerApplicationContext`.

### U-31 — Pruebas débiles o que pueden fallar sin motivo
- **Gravedad:** menor. **Origen:** previo y F7.
- **Problema:**
  - **`CatalogIndexIT`:**
    - El caso «ajeno» (`alias + "x-…"`) nunca ejercita `isOwnedBy`. Falta probar
      `elastiflix-reloaded-it-…` contra el alias `elastiflix-reloaded`, también en `IndexNamesTest`.
    - La aserción de `plot_* == null` en `findById` (`:147-149`) no puede fallar, porque Serverless ya excluye
      los `dense_vector` de `_source`.
    - La limpieza (`:66-70,163-173`) se detiene en el primer error.
  - **`EmbeddingServiceIT:98` y `DocumentEmbeddingCalibrationIT:99`:** dependen de 5 ids del catálogo vivo
    sin `assumeTrue`. Además, `DocumentEmbeddingCalibrationIT:69` solo afirma que el archivo existe.
  - **`SearchQueryValidatorTest`:** no acepta explícitamente `k=1`, `num_candidates == k` ni
    `num_candidates == 10 000`.
  - **`ImportJobTest`:** falta el caso de `_aliases` 5xx con el alias en el anterior (`EFX-IMP-006`, sin
    borrar), que PLAN 3.6 nombra explícitamente.
  - **`IngestServiceTest:73-80`:** hay una carrera entre `invalido` y el borrado del archivo, y el temporal
    no se limpia si la aserción falla.
  - **`FieldRules`:** ninguna prueba lo ata a `IndexMapping` (hoy coinciden los 33 campos).

### U-32 — Javadoc que contradice el código
- **Gravedad:** menor. **Origen:** previo.
- **Problema:**
  - `IngestJobErrors:24-25` dice que la página «siempre recibe JSON», pero `MissingServletRequestPartException`
    sale como HTML 400.
  - `ImportProgress:8-9`, `JobView:17` y `BatchWriter:54-56`: la descripción de `batch` y `written` no
    coincide con lo que guardan.
  - `ValidationIssue.recordLabel` no menciona `archivo` ni `formulario`.

### U-33 — `package-info.java` y cabeceras de seguridad
- **Gravedad:** menor. **Origen:** previo.
- **Problema:**
  - Faltan `package-info.java` (convenciones §9) en `web/about`, `web/error`, `web/home`, `web/ingest`,
    `web/layout` y `web/search`.
  - No hay `X-Content-Type-Options`, `frame-ancestors` ni `Referrer-Policy` (decisión 6).

---

## E. Documentación y proceso

### U-34 — «Aprobaciones» de `INDICE.md`: una fila borrada y otras que faltan
- **Gravedad:** importante. **Origen:** F8.
- **Problema:**
  - El commit `6431aa6` **reemplazó** la fila «Cierre y fusión de la Fase 7 (… `2902be7`)» por la de la fase
    8. Hoy no queda registro de que aprobaste fusionar el PR #23.
  - Falta el consentimiento para arrancar el Requerimiento 2. PLAN l. 1194 lo exige en «Aprobaciones», y
    solo está en `progreso/8.md` (`/goal …`).
  - La fase 8 se marcó sin compuerta y no hay registro de que vieras o aprobaras `docs/diseno/8.md`. La fase
    7 sí la tuvo.
- **Corrección:** restituir la fila de la fase 7 y agregar las del Requerimiento 2. Registrar cómo se trató
  el diseño 8 (si no lo aprobaste, decirlo así).

### U-35 — Cifras, verificación y fechas incorrectas en `progreso/`, `INDICE.md` y PLAN
- **Gravedad:** importante. **Origen:** F7, F8.
- **Problema:**
  - **Totales de pruebas:**
    - cierre de la fase 7: dice «329 repo», y eran 341;
    - cierre de la fase 8: dice «347», y son 366;
    - pasos intermedios incoherentes: en 8.4 y 8.6 se agregan pruebas y el total no cambia;
    - `progreso/7.md` da cifras solo de la web sin aclararlo.
  - **«Última verificación»** de `INDICE.md` sigue en la revisión 2 (264 + 29, «la revisión 3 no pudo
    compilar»).
  - **Horas UTC:** la fusión del PR #24 fue el 2026-10-20 05:14 UTC, no el 2026-10-19. Las horas 05:15 y 05:20
    de `8.md` e `INDICE.md` son posteriores a sus propios commits.
- **Corrección:** rehacer las cifras desde la corrida de U-02 y tomar las horas de `git log`.

### U-36 — Documentos con afirmaciones que ya no son ciertas
- **Gravedad:** importante (`AGENTS.md` lo lee todo agente). **Origen:** F7, F8.
- **Problema:**
  - **`AGENTS.md`:**
    - l. 47: «El plan está cerrado en `main`»;
    - l. 54: solo «fases 0 a 6 y revisiones 1, 2 y 3»;
    - l. 57: los requerimientos «esperan el consentimiento»;
    - l. 90: falta Detalle;
    - l. 150: «Interfaz en español».
    - Hoy mide 11 418 caracteres; quedan unos 580 de margen.
  - **`CLAUDE.md:8-10`:** lo mismo.
  - **PLAN:**
    - l. 244: `/pelicula/{id}` sigue «Pendiente de implementar»;
    - §5 no documenta `lang`, la cookie `elastiflix-lang`, `GET /cargar?job=` ni el 400 con `EFX-SRCH-012`;
    - l. 1190: «No se implementan todavía»;
    - l. 1271: «Hoy todas las páginas están en español»;
    - l. 360 («en español»), l. 228 («4 páginas») y l. 14/16/463 (fases 0–6);
    - el índice no tiene las fases 7 y 8, y §4 no tiene `docs/i18n/` ni los mockups `-en`.
  - **`README.md`:**
    - l. 18: el badge dice «fases-8/8» y son 9;
    - l. 207 y 331: «4 páginas»; «Qué incluye» no menciona el detalle ni el i18n;
    - l. 359 y 366: no dice que Antigravity hizo las fases 7 y 8;
    - l. 384: no menciona la 🔒 de la fase 7;
    - l. 427-464: la tabla de documentación no enlaza `diseno/7.md`, `diseno/8.md`, el glosario ni
      `docs/revisiones/`.
  - **`design/README.md:17-22`:** Detalle y las versiones `-en` siguen «pendientes de implementar».
  - **`docs/convenciones-java.md:99,283,608`:** «textos de interfaz en español».
  - **`docs/i18n/glosario-en.md`:**
    - dice que las consultas de ejemplo «no se traducen», al revés de lo que aprobaste;
    - no se amplió con los términos nuevos.
  - **`docs/arquitectura.mermaid`:** sin el detalle, sin la configuración de idioma y sin `findById`.
  - **`docs/capturas/`:** sin el detalle, sin el error `EFX-SRCH-012` y sin las versiones en inglés.
- **Corrección:** alinear todo. Las capturas nuevas necesitan tu aprobación, como en S-18.

### U-37 — PLAN y `progreso/` afirman trabajo que no se hizo
- **Gravedad:** importante. **Origen:** F7, F8.
- **Problema:**
  - Las tareas 8.4, 8.5 y 8.6 (PLAN l. 1351-1353) y el diseño 8 §6 dicen que se internacionalizaron
    `FacetChoices`, `MovieDetailBadges`, `IngestController` y `UploadSizeErrors`. `git log` no muestra cambios
    en ninguno, y `UploadSizeErrors` sigue en español (U-13).
  - La tarea 8.7 afirma «cero literales cableados en JS» (falso: U-14 y U-15).
  - `progreso/7.md:60` dice que el diseño «se implementó fielmente en su totalidad» (falso: U-07 y U-38).
- **Corrección:** corregir el código (U-13, U-14) o el texto, de modo que lo marcado `- [x]` sea cierto.

### U-38 — Los diseños 7 y 8 no siguen la plantilla, y las desviaciones no se registraron
- **Gravedad:** menor. **Origen:** F7, F8.
- **Problema:**
  - **`docs/diseno/8.md`:** le faltan «Estado de aprobación», «Requerimientos cubiertos», «Dudas y
    supuestos», «Patrones», «Errores», «Logging», «Alternativas», «Supresiones» y «Cambios respecto al
    diseño aprobado».
  - **`docs/diseno/7.md`:**
    - le faltan «Pruebas previstas», «Alternativas», «Supresiones» y «Cambios»;
    - declara el patrón «View Helper / ViewModel», que no está en la lista cerrada de `AGENTS.md`.
  - **Desviaciones sin registrar:**
    - ocho clases que el diseño 7 no prevé;
    - `MovieDetailView` con 29 componentes;
    - el log de «no encontrada» en `INFO` y no en `WARN`;
    - las firmas de `PageShells.from` y `toErrorCsv` distintas a las del diseño 8.
  - **Revisión de convenciones:** la de `progreso/7.md` y `8.md` lista métricas en lugar de responder las 10
    preguntas de convenciones §16. Además, menciona una «arquitectura hexagonal» que el proyecto no usa.
  - **Disciplina de §9:**
    - las casillas de 8.4–8.6 se marcaron recién en 8.7;
    - `progreso/7.md` se aparta de la plantilla (título, nota de hashes, `verify` al iniciar).
- **Corrección:** agregar en cada diseño la sección «Cambios respecto al diseño aprobado» y completar las
  secciones que faltan.

---

## Lo que se revisó y está bien

- **Fase 7:**
  - la ficha cubre los 30 campos no vectoriales del mapping;
  - `findById` excluye los vectores y usa el alias configurado;
  - USD con miles, y 0 o nulo dan «N/A»;
  - `adult` y `video` se muestran como sí, no o N/A, y la colección como filas o «No pertenece…»;
  - el reparto se corta en 18 con `aria-expanded`;
  - el enlace directo se valida antes de leer Elasticsearch y responde 400, con las mismas reglas que
    `/buscar`;
  - el regreso conserva todos los parámetros, codifica `&`, `#`, `%`, `+` y unicode, y el ancla `#r-<id>`
    coincide con la tarjeta;
  - los enlaces externos llevan `rel="noopener noreferrer"`.
- **Fase 8:**
  - el español es el idioma por defecto e ignora `Accept-Language`;
  - un `lang` inválido se ignora;
  - los dos catálogos tienen las mismas 637 claves, con los mismos `{n}` y sin mojibake;
  - los apóstrofos se manejan bien (`ApostropheSafeMessageSource`);
  - los textos de `ErrorCode` en español coinciden 156 de 156 con `messages_es`;
  - el selector tiene `lang`, `hreflang`, `aria-current` y conserva los parámetros multivalor;
  - ningún singleton guarda un `Locale`;
  - core, search e ingest no dependen de Spring MVC;
  - las sugerencias en inglés coinciden con lo medido en el diseño 8.
- **Previo:**
  - las reglas del dominio de `AGENTS.md`: alias, k-NN, RRF, `from < 10 000`, prefijos de inferencia,
    facetas y UTC;
  - la publicación atómica, con `describe`, `EFX-ES-004` y `EFX-IMP-006` como pide `AGENTS.md`;
  - la limpieza de huérfanos, con una regex anclada;
  - la comprobación de archivos y el tope de 150 MB, coherente entre navegador, multipart y servidor;
  - los logs no muestran secretos, vectores ni documentos;
  - las IT escriben solo en `elastiflix-it-*`;
  - los POM y dependencias siguen PLAN §3, sin dependencias prohibidas;
  - las dos búsquedas de secretos de la Definición de Hecho salen vacías en todo el historial;
  - `.env.example` coincide con las propiedades.
