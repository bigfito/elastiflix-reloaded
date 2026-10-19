# Correcciones de la revisión 3 — Elastiflix-Movies Java

> Tercera revisión exhaustiva del código y la documentación de `main` en `27fcdef` (2026-10-19 UTC), después
> de la revisión 2 (S-01…S-27, PR #13) y del PR #14.
>
> **27 correcciones de código:** 0 críticas, 13 importantes y 14 menores. Cada una se verificó leyendo el
> código. Checkstyle y PMD siguen en 0 violaciones. La única marca de PMD es el falso positivo conocido de
> `JsonShapes`, que aparece al correr PMD sin classpath. La revisión **no pudo compilar ni ejecutar
> pruebas**: el entorno no tenía Maven Central ni JDK 25.
>
> **La documentación ya se alineó con el código** en el mismo commit que agregó esta lista. Lo que queda
> aquí exige cambiar código o pruebas. Cuando una corrección cambia un comportamiento documentado, dice qué
> documento actualizar.
>
> **Estado del trabajo:** `progreso/revision-3.md` y `progreso/INDICE.md`. Esta lista es la
> especificación; no marques nada aquí, solo en `progreso/`.
>
> **La revisión 2 quedó bien en lo esencial.** Cumplen su criterio:
>
> - S-02, S-04, S-07, S-08, S-09, S-10, S-11, S-12, S-13, S-18, S-20, S-23, S-24 y S-25;
> - S-01, salvo T-01;
> - S-03, salvo T-11 y T-20;
> - S-05, salvo T-27;
> - S-06, salvo T-17;
> - S-14, salvo T-27;
> - S-21, salvo T-19;
> - S-22, salvo T-14 y T-20.

---

## Cómo trabajar esta lista

Sirve igual para **Claude Code**, **Google Antigravity** o **JetBrains Junie**: todos leen `AGENTS.md` en
la raíz (Claude Code lo importa desde `CLAUDE.md`).

1. Aplica el protocolo de reanudación de `AGENTS.md` y lee `PLAN.md`, `AGENTS.md` y
   `docs/convenciones-java.md`. Las reglas no cambian: mismas convenciones, mismos umbrales y `config/`
   congelado.
2. El estado vive en `progreso/revision-3.md` (unidad «Revisión 3», tareas `T-01` … `T-27`) y en
   `progreso/INDICE.md`.
   - Si la unidad dice `no iniciado`, márcala `en curso` al empezar T-01 y anota la herramienta.
   - Si ya hay avance, continúa desde su «Próximo paso».
3. Trabaja en la rama `revision-3`, creada desde `main`. Si ya existe en `origin`, continúa en ella. No
   hagas push directo a `main`: al terminar, abre un PR hacia `main` y deja que el usuario lo fusione.
4. Atiende las correcciones **en el orden de la lista**.
5. **No marques una corrección como terminada si no cumples su criterio completo.**
   - La evidencia nombra la prueba que falla si se rompe el comportamiento.
   - Compruébalo rompiendo el código a mano y revirtiendo.
   - Si algo del criterio no se puede hacer, deja la corrección `bloqueada`, explica qué falta y pregunta.
6. **Al terminar cada corrección:**
   - cumple su criterio;
   - actualiza `progreso/revision-3.md` y `progreso/INDICE.md` con la evidencia y el **hash real** del
     commit anterior (nunca «este commit»);
   - haz un commit `T-NN: <resumen>`;
   - haz `git push` de la rama.
7. Si cambias algo de un diseño aprobado, anótalo en «Cambios respecto al diseño aprobado» de
   `docs/diseno/<fase>.md`. Si cambias o agregas un texto visible, agrégalo a la tabla de ajustes
   autorizados de PLAN §1.
8. **Las decisiones abiertas las tomas tú:** T-07, T-15, T-19 y T-20. Elige la opción más simple que cumpla
   el criterio y regístrala en «Decisiones» de `progreso/revision-3.md`.
   - Detente a preguntar al usuario solo si un criterio no se puede cumplir.
   - Detente también si la corrección cambia lo que se ve en una pantalla aprobada más allá de lo que pide
     la lista.
9. **Al final:**
   - `./mvnw verify` y `./mvnw verify -Pit` en verde, registrados con fecha, conteos y el commit sobre el
     que corrieron;
   - las pruebas de navegador (`elastiflix-web/src/test/browser/`) en verde;
   - `README.md`, PLAN y `progreso/` al día;
   - estado `terminado` en `progreso/revision-3.md` y en `progreso/INDICE.md`;
   - abre el PR, detente y entrega un resumen con la evidencia de cada `T-NN`.

---

## 🟠 Importantes

### T-01 — La copia del índice no se cancela ante una respuesta ilegible de `_tasks`
*(Resto de S-01)*

- **Dónde:** `elastiflix-core/.../catalog/ReindexTasks.java` (`copy`, l. 63-73; `cancel`, aprox. l. 129;
  `hasFailures`, aprox. l. 150).
- **Problema:**
  - `copy` solo atrapa `IOException | ElasticsearchException` antes de `cancel(taskId)`.
  - Una `JsonpMappingException` mientras se espera la tarea se salta la cancelación.
    `ElasticsearchCatalogIndex.call` la convierte en `EFX-ES-001`, y quien llama borra el temporal con el
    reindex todavía escribiendo, que es justo el caso que S-01 debía cerrar. `docs/diseno/1.md` promete
    cancelar ante cualquier error.
  - `cancel` espera con `wait_for_completion=true` por el mismo cliente de 30 s. Si cancelar tarda más, se
    registra un `WARN` y el temporal se borra con la tarea viva.
  - `hasFailures` usa `getJsonArray("failures")`, que lanza `ClassCastException` si `failures` llega
    `null`.
- **Corrección:**
  - Agregar `JsonpMappingException` al multi-catch de `copy`.
  - Cancelar con `wait_for_completion=false` y confirmar con hasta 3 lecturas de `_tasks` de 20 s. Si no
    se confirma, registrar un `WARN` con el id de la tarea.
  - En `hasFailures`, comprobar `getValueType()` antes de leer el arreglo.
- **Criterio:** `ReindexTasksTest` cubre cuatro casos:
  - `JsonpMappingException` durante la espera → se llama a `cancel`;
  - cancelación que no termina en la primera lectura → se reintenta y se confirma;
  - `failures: null` → sin excepción;
  - `failures: [..]` → `EFX-ES-006`.

### T-02 — Búsqueda e inferencia no traducen una respuesta ilegible a un código EFX
- **Dónde:**
  - `elastiflix-search/.../transport/ElasticsearchSearchTransport.java`, l. 59;
  - `elastiflix-core/.../embedding/InferenceEmbeddingService.java`, l. 102.
- **Problema:** las dos atrapan solo `IOException | ElasticsearchException`. Un documento o una respuesta
  de inferencia que el cliente no puede leer (`JsonpMappingException`) sube como `RuntimeException` sin
  código EFX y termina en una página 500 genérica. Core sí la atrapa (`ElasticsearchCatalogIndex.call`).
- **Corrección:** agregar `JsonpMappingException` al multi-catch: `codeFor(0)` en búsqueda y `EFX-EMB-001`
  en inferencia.
- **Criterio:** una prueba por clase con el cliente simulado lanzando `JsonpMappingException`. Las dos
  afirman el código EFX.

### T-03 — Validación k-NN: un vector de consulta sin campo da NPE
- **Dónde:** `elastiflix-search/.../SearchQueryValidator.java` (`requireKnn`, l. 47-60).
- **Problema:** no se comprueba `knn.field() == null`. El valor termina en un NPE en
  `QueryVector.forField`, `SearchLog.vectorField` y `AdjustmentLabels`, en lugar de `EFX-SRCH-010`.
- **Corrección:** `if (knn == null || knn.field() == null) → MISSING_MODE_SETTINGS`.
- **Criterio:** una prueba de `SearchQueryValidator` con `field` nulo da el código de ajustes faltantes.

### T-04 — «Cargar datos»: el aviso de un 409 o 404 al importar o cancelar se borra al instante
- **Dónde:** `elastiflix-web/src/main/resources/static/js/ingest.js` (`act`, l. 315-332; `paint`, l. 341).
- **Problema:** tras un 4xx o una caída de red, `act()` llama a `serverError(...)` y después a `poll()`. La
  respuesta de `poll()` llama a `paint()`, que oculta `#server-error`. El texto `EFX-IMP-*` desaparece en
  milisegundos.
- **Corrección:** que `paint()` no oculte un error que dejó `act()`. Por ejemplo, una bandera
  `actionError` que solo limpia el siguiente clic. Otra opción es no llamar a `poll()` tras un 4xx.
- **Criterio:** prueba de navegador (ver T-11): `page.route()` responde 409 a `/importar` y el aviso sigue
  visible después del siguiente sondeo.

### T-05 — «Cargar datos»: no se puede volver a validar el mismo archivo con otro separador
- **Dónde:** `static/js/ingest.js` (`upload`, l. 251-276; `choose`; el `change` del input, l. 135-137).
- **Problema:**
  - `upload()` deja «Validar archivo» deshabilitado y solo lo rehabilita si la subida falla.
  - El `<input type=file>` no se vacía tras elegir, así que «Cambiar archivo» con el mismo archivo no
    dispara `change`.
  - Un CSV con `;` validado con «Coma» queda sin salida hasta cambiar de pestaña.
- **Corrección:**
  - `input.value = ""` en `choose()` después de guardar el archivo;
  - rehabilitar «Validar archivo» cuando cambian el separador o la casilla de encabezado y hay archivo
    elegido.
- **Criterio:** prueba de navegador: validar un CSV, cambiar el separador y volver a validar el mismo
  archivo hace un segundo POST.

### T-06 — Resultados: el filtro `adult=true` se pierde sin aviso
- **Dónde:** `templates/results.html`, l. 62-66; `AppliedFilters.java`, l. 46-47.
- **Problema:** `adult=true` es válido (PLAN §5) y el chip muestra «Adultos: Solo adultos». Pero el
  formulario solo tiene «Todos» y «Solo no adultos», y marca «Todos». El siguiente APLICAR envía
  `adult=` y quita el filtro sin avisar. Es el mismo fallo que S-11 corrigió en las facetas.
- **Corrección:** con `adult=true`, agregar una opción «Solo adultos» marcada, como `mk()` en S-11.
  Registrar el texto en PLAN §1.
- **Criterio:** una prueba MockMvc con `adult=true` muestra la opción marcada.

### T-07 — Cada página pide `/favicon.ico`, y ese 404 consulta Elasticsearch
- **Dónde:** `templates/fragments/head.html` (sin `<link rel="icon">`); `static/` (sin ícono);
  `error/NotFoundAdvice.java` (`missing`).
- **Problema:** el navegador pide `/favicon.ico` en cada carga. El 404 pasa por
  `NotFoundAdvice.missing`, que llama a `catalog.status()` (`describe` y `count` sin caché), registra un
  INFO y renderiza la página 404. Con Elasticsearch caído, cada página suma una ronda de timeouts.
- **Corrección (decide):**
  - agregar un ícono (por ejemplo `static/favicon.svg` con el logo del mockup) con su `<link rel="icon">`;
  - o excluir los recursos estáticos del advice.
- **Criterio:**
  - MockMvc: `GET /favicon.ico` (o el ícono declarado) no llama a `catalog.status()`;
  - `LayoutPageTest` ve el `<link rel="icon">` si se elige el ícono.

### T-08 — «Archivo válido» aparece antes de terminar la validación y en trabajos fallidos
- **Dónde:** `elastiflix-ingest/.../job/JobFigures.java` (`checks`, l. 48-55); `ImportWork.java` (`view`);
  `FileIntake.java`, l. 96.
- **Problema:** `checks(errors, fileCount)` solo mira cifras. `fileCount` se fija al leer el archivo, antes
  de validar contra el mapping. Mientras el trabajo está `validando`, y también en `fallido` (por ejemplo
  `EFX-ES-001` al leer los ids existentes), la página ya dice «N registros leídos, con id, title y plot.
  Ningún id repetido.».
- **Corrección:** pasar el estado a `JobFigures.checks`. Solo hay comprobaciones en `valido`, `importando`
  y `publicado`.
- **Criterio:** `JobFiguresTest` en dos casos: `validando` y `fallido` con `fileCount > 0` → lista vacía;
  `valido` → las dos frases.

### T-09 — Un rechazo de archivo detiene el paso equivocado
- **Dónde:** `job/ImportWork.java` (`invalid`, l. 141-145; `validating`, l. 115); `FileIntake.java`.
- **Problema:**
  - `invalid()` siempre pone `step = 3`. `docs/diseno/3.md` dice que `EFX-ING-001` a `004` detienen el
    paso 2.
  - Con una extensión mala, una doble extensión, más de 150 MB o un archivo que no es UTF-8, la página
    muestra el paso 2 completado y el 3 detenido.
  - Además, `validating()` deja el paso en 2 durante toda la validación contra el mapping.
- **Corrección:**
  - `FileIntake` indica el paso: 2 para `EFX-ING-001` a `004` y 3 en los demás casos;
  - `describe()` pasa el trabajo al paso 3 al empezar la validación del mapping.
- **Criterio:** `ImportWorkTest` (o `FileIntakeTest`) en tres casos:
  - `EFX-ING-002` → paso 2 detenido;
  - `EFX-ING-012` → paso 3 detenido;
  - durante la validación → paso 3 en curso.

### T-10 — Faltan pruebas unitarias de ramas importantes
- **Problema y corrección:**

  | Clase | Prueba que falta |
  |---|---|
  | `ImportJobTest` | `REINDEX_COUNT_MISMATCH` en la copia. `CATALOG_NOT_FOUND` y `ALIAS_AMBIGUOUS`. Más de un lote (tamaño de lote menor que el archivo). Cancelar entre lotes y justo antes de `swap`. `deleteTemp` que falla (camino del `WARN`). |
  | `ImportRunner` / `JobStore` | `IMPORT_CANCELLED` → `cancelado` y éxito → `publicado`. Hoy solo lo cubre `IngestIT`, que necesita `.env`. |
  | `CatalogBootstrapTest` | `publish` con 5xx → `EFX-IMP-006` sin borrar. Reindex con `EFX-ES-006` cuando el original ya no es índice concreto. |
  | `ElasticsearchCatalogIndex` | `publish` con `timeout(10s)` e `indexOne` con `require_alias=true`, verificados en la petición con el cliente simulado. Hoy solo los cubre la IT. |
  | `KnnStrategiesTest` | Un ítem 4xx del `_msearch` → `EFX-ES-007`. |
  | `InferenceEmbeddingServiceTest` | `embedQuery` envía `input_type=search` y el prefijo `Query: `, comprobado con el cliente simulado. |

- **Criterio:** cada prueba de la tabla falla si se rompe el comportamiento que describe. Compruébalo
  rompiendo el código a mano y revirtiendo, y deja la evidencia en `progreso/revision-3.md`.

### T-11 — El manejo de estados de `ingest.js` no tiene prueba automática
*(Resto de S-03)*

- **Dónde:** `elastiflix-web/src/test/browser/` (hoy solo `encoding` de S-09 y `suggestions` de S-10).
- **Problema:**
  - `cancelado`, los botones deshabilitados al hacer clic, 404/409 y la caída de red solo se probaron a
    mano.
  - T-04 muestra que el hueco es real.
  - Además, `suggestions.test.js` navega a un `/buscar` real, que sin Elasticsearch termina en
    `EFX-ES-001`.
- **Corrección:**
  - Agregar `ingest.test.js` con `page.route()` que simule `/cargar/trabajos/*`. Casos:
    - 409 al importar → aviso visible (T-04);
    - `cancelado` → paso 4 detenido;
    - doble clic en Importar → un solo POST;
    - caída de red → aviso de red;
    - revalidar el mismo archivo (T-05).
  - Que `suggestions.test.js` intercepte `/buscar` o solo afirme la URL pedida.
- **Criterio:** `npx playwright test` en verde sin Elasticsearch. Cada caso falla si se revierte su
  corrección.

### T-12 — Código muerto y campos que nadie lee
*(Resto de S-25)*

- **Problema y corrección:**

  | Módulo | Qué sobra | Qué hacer |
  |---|---|---|
  | ingest | `DocumentChecker.warnVector` y `write`, `ValidationRun.warned` y el parámetro `warnedVectors`. JSON, CSV y el registro individual ya quitan los vectores antes de `validate`. | Quitarlos y ajustar las pruebas que pasan nodos crudos. |
  | ingest | `MovieDocuments.IGNORED` y `dropBlankText`. `DocumentChecker` nunca copia `_id`, vectores ni textos en blanco. | Quitarlos. |
  | ingest | La rama VECTOR de `CsvValues` y la comprobación de nulo en `CsvMovieReader.put`. | Quitarlas. |
  | ingest | `ImportCounts.replaceCount` e `ImportProgress.replaced`: nadie los lee desde S-03. | Quitarlos. |
  | core | `IndexMapping.field()` y `document()` (solo pruebas; `field` lanza `IllegalArgumentException`). | Quitarlos o mover su uso a las firmas reales. |
  | core | `CatalogIndex.readWithVectors` y `readAllWithVectors` (solo las IT). | Quitarlos o mover su uso a las firmas reales. |
  | core | `InferenceEmbeddingService.requireSameSize` (expuesto para dos pruebas que ya sobran). | Quitarlos o mover su uso a las firmas reales. |
  | search | `Rrf.term(int)` y `TextOperator.elasticsearchOperator()` (duplica `LexicalQueryBuilder.elasticsearch()`). | Quitarlos o mover su uso a las firmas reales. |
  | search | `RankedHit.rrf`: siempre es igual a `score`. | Dejar un solo campo. |
  | search | El parámetro `preference` de `VectorRequestBuilder.build`, que siempre llega `null`. | Quitarlo. |
  | web | El atributo de modelo `changeHref` (`ResultsPage.java`, l. 47): la plantilla usa `@{/}`. | Quitarlo. |

- **Criterio:** `./mvnw verify` en verde y `git grep` de cada nombre quitado sin resultados en `src/main`.

### T-13 — Límites de carga con dos orígenes y valores escritos a mano
- **Dónde:**
  - `elastiflix-web/.../ingest/IngestJobController.java`, l. 55: `@Value("${spring.servlet.multipart.max-file-size:150MB}")`;
  - `application.yml` (`max-request-size: 151MB`);
  - `templates/about.html` («máximo 150 MB», «lotes de 100»).
- **Problema:**
  - `@Value` está prohibido por `docs/convenciones-java.md` §6 y es un segundo origen para el tope que ya
    da `IngestLimits.maxFileSize()`.
  - Si se sube `ELASTIFLIX_INGEST_MAX_FILE_SIZE` por encima de 151 MB, las subidas fallan igual.
  - «Acerca de» repite las cifras a mano, mientras «Cargar datos» ya las toma de `IngestLimits`.
- **Corrección:**
  - usar `limits.maxFileSize()`;
  - derivar `max-request-size` del mismo valor, o documentar en PLAN §7 que hay que subir los dos;
  - pasar `maxFileLabel` y `batchSize` a `AboutController`.
- **Criterio:** MockMvc con `ELASTIFLIX_INGEST_MAX_FILE_SIZE=200MB` y `ELASTIFLIX_INGEST_BATCH_SIZE=50`
  muestra «200 MB» y «50» en «Cargar datos» y en «Acerca de».

---

## 🟡 Menores

### T-14 — Una página de error EFX lee el catálogo dos veces
*(Resto de S-22)*

- `LayoutAdvice.shell()` ya corrió para el controlador. Después, `WebExceptionHandler.shell()` vuelve a
  llamar a `catalog.status()`. Con Elasticsearch caído, son dos rondas de timeouts.
- `ServedPagesTest.shouldReadTheCatalogOnceForAnErrorPage` solo cubre el 405.
- **Corrección:** reutilizar el `shell` de la petición (en un atributo de la petición).
- **Criterio:** una prueba con una `ElastiflixException` verifica `times(1)` en `catalog.status()`.

### T-15 — La API de carga: un error sin código se muestra como «sin conexión», y la subida deja un temporal
- Una `RuntimeException` que no es `ElastiflixException` en `IngestJobController` sale como HTML por
  `/error`. `ingest.js` (`readJson` / `errorText`) muestra «No se pudo conectar con el servidor de la
  demo», que es falso.
- Si `acceptFile` lanza, el archivo temporal de la subida queda en disco (`IngestJobController`).
- **Corrección (decide):**
  - un manejador `RuntimeException` → 500 JSON `EFX-IMP-007` en `IngestJobErrors` (sin atrapar
    `RuntimeException` de forma genérica: Checkstyle `IllegalCatch`; usar el manejador de Spring);
  - o que el JS distinga un `!response.ok` sin JSON de un fallo de red.
  - Además, `try/catch` alrededor de `acceptFile` que borre el temporal y relance.
- **Criterio:** MockMvc con un `IllegalStateException` simulado recibe JSON con código. Una prueba verifica
  que el temporal se borra.

### T-16 — Hilos de la carga: estado final, excepciones y nombres
- **Estado final equivocado:** si el aviso del paso 5 lanza después de que `_aliases` respondió bien,
  `ImportRunner` llama a `importStopped` y el trabajo queda `fallido`. El texto de `JOB_FAILED` dice
  entonces que «El catálogo anterior no se publicó», que es falso.
  - Corrección: que `ImportJob.run` devuelva la señal de publicado antes de avisar, o atrapar la
    `ElastiflixException` alrededor del aviso con un `WARN`.
- **Excepción original perdida:** `ImportRunner` (l. 55) y `FileIntake` (l. 88-92) guardan
  `new ImportException(JOB_FAILED)` sin causa. La excepción original sale del hilo virtual y solo la
  imprime el manejador por defecto de la JVM en stderr, fuera de SLF4J.
  - Corrección: agregar `uncaughtExceptionHandler` con `LOG.error("EFX-IMP-007 hilo={} tipo={}", ...)`.
- **Nombres repetidos:** `Thread.ofVirtual().name("ingest-validate-", 0)` crea un builder por llamada, y
  todos los hilos se llaman `ingest-validate-0` / `ingest-import-0` (`IngestService.java`, l. 61 y 82).
  - Corrección: `.name("ingest-validate-" + jobId)`.
- **Criterio:**
  - `ImportJobTest`: el listener que lanza en el paso 5 deja el trabajo `publicado`;
  - una prueba del manejador de hilos verifica el log.

### T-17 — Detalles de la validación de archivos
- **Elasticsearch caído oculta los errores:** `FileIntake` (l. 101) lee los ids existentes **antes** de
  validar. Con el clúster caído, un archivo inválido termina `fallido` `EFX-ES-001` en lugar de mostrar
  sus errores.
  - Corrección: leer `existingIds` solo si la validación pasó.
- **Orden del informe CSV:** `FileIntake.join` pone todos los errores de ancho (`EFX-ING-014`) antes que
  los de campo, y la vista previa de 6 errores puede mostrar la fila 900 antes que la 2.
  `CsvRowNumbersTest` usa `containsExactlyInAnyOrder`.
  - Corrección: orden estable por número de fila, con el encabezado y los errores del archivo primero.
- **Desbordamiento de `document_count`:** un valor de 25 dígitos hace que `getLongValue()` lance, y sale
  `EFX-ING-013` en lugar de `EFX-ING-008` (`JsonDocuments`, l. 111-116).
- **Vencimiento de trabajos `valido` (resto de S-06):** `JobStore.evict()` solo corre en `open()`. Un
  trabajo `valido` que nadie importa retiene sus películas hasta la siguiente subida. Llamar también a
  `evict()` en `view()` y `beginImport()`. Si no, `docs/diseno/3.md` ya lo documenta como perezoso.
- **Criterio:** una prueba por cada punto.

### T-18 — Códigos de error equivocados en búsqueda y mapping
- `HybridSearchStrategy` (l. 95): un `_msearch` con partes de más o de menos llama a
  `rejected("respuesta", 0)`, que da `EFX-ES-001` («No se pudo conectar…»).
  `KnnStrategiesTest.shouldFailWhenTheMultiSearchHasTheWrongNumberOfParts` solo afirma
  `ElastiflixException`.
  - Corrección: elegir un código (por ejemplo `EFX-ES-007`), documentarlo en `docs/diseno/2.md` y
    afirmarlo en la prueba.
- `IndexMapping.field` (l. 81) lanza `IllegalArgumentException`, contra la regla de errores. Se resuelve
  si T-12 lo quita.

### T-19 — Textos de error y Javadoc que no corresponden al comportamiento
*(Resto de S-21)*

- **`ErrorCode`:**
  - `ALIAS_PUBLISH_FAILED` dice «si no se pudo, lo limpia la próxima importación». Tras una migración
    fallida el nombre sigue siendo un índice concreto, `ImportJob.requireAlias` lanza `EFX-IMP-004` y ese
    huérfano nunca se limpia.
  - `REINDEX_COUNT_MISMATCH` afirma sin condición que «el índice nuevo se borra», aunque
    `CatalogBootstrap` solo lo borra si el original sigue.
  - `PUBLISH_UNCERTAIN` habla de «índice temporal», y en la migración es el «índice nuevo».
  - `BULK_REJECTED`, `IMPORT_COUNT_MISMATCH`, `IMPORT_CANCELLED` y el texto de `JobStatus.CANCELLED`
    dicen «Se borró el índice temporal», aunque `deleteTemp` puede fallar con solo un `WARN`.
  - `UNREADABLE_JSON` (`EFX-ING-013`) dice «o el JSON de una celda», pero una celda CSV mala se lee como
    texto y falla con `EFX-ING-012`.
- **Javadoc:**
  - `JobView` `@param step` dice «0 si todavía no hay», y el paso empieza en 1.
  - `ImportRunner` («o deja el temporal borrado») e `ImportJob.run` («Si falla, el alias queda como
    estaba») no cuentan el caso `EFX-IMP-006`.
  - `BatchWriter` dice que avisa «antes de cada escritura»; desde S-24 avisa antes y después.
  - `SearchController` (l. 19 y 44) dice que el error «se muestra aquí, con su código». Desde S-22 solo se
    muestra `summary()`.
  - `CosineSimilarity` está en `src/main`, pero su Javadoc dice «Lo usan las pruebas».
- **Corrección (decide el texto):** textos neutros que digan «se borra si…; si quedó, bórralo a mano», y
  Javadoc fiel. Copiar los textos nuevos a las tablas de `docs/diseno/1.md` y `docs/diseno/3.md`.
- **Criterio:** `ErrorCodeTest` afirma los textos nuevos. `grep -rn "Se borró el índice temporal"
  elastiflix-*/src/main` no devuelve nada.

### T-20 — Textos visibles sin punto final o sin fila en PLAN §1
*(Resto de S-03, S-16 y S-22)*

- **Sin punto final:**
  - `templates/error.html` muestra `ErrorPages.filled` → `summary()` sin punto («No se pudo conectar con
    Elasticsearch»);
  - `ingest.html` (l. 18) pinta la lista de rechazos `código · campo · esperado` sin punto.
- **Sin fila en PLAN §1:**
  - «Se guardó <título>.» (`ingest.html`, l. 16);
  - la lista de rechazos del registro individual (l. 17-19);
  - el formato de las advertencias reales «`campo: mensaje (N)`» (`JobTexts`). El mockup usa frases
    completas («62 películas tienen un idioma vacío…»).
- **Corrección (decide):** agregar el punto, y por cada texto agregar su fila en §1 o cambiar el texto al
  del mockup.
- **Criterio:**
  - `ErrorPageTest` afirma el punto;
  - cada texto de `ingest.html` y `JobTexts` está en el mockup o en §1.

### T-21 — «Ver todos» aparece aunque no oculte nada
- `results.html` (l. 75 y 95) usa `genres.size() > 6`. Desde S-11, 6 valores de faceta más un marcado que
  falta dan 7 opciones, ninguna `extra`, y el botón no hace nada.
- **Corrección:** un booleano `hasExtra` en el modelo (`anyMatch(FacetChoice::extra)`).
- **Criterio:** una prueba MockMvc con esas 7 opciones no muestra «Ver todos».

### T-22 — CSS duplicado y muerto
- **Reglas repetidas** en `static/css/elastiflix.css`: `.fh`, `.filters legend`, `.poster.missing`,
  `.filters .extra` / `.show-extra .extra`, `.results h1`, `.filters fieldset`, `.results-layout`,
  `.rrf-value`, `.filters .date-field`, `.hit-list`, `.mode-cards`, `.filters-head h2`, y `.legend`, que
  tapa a `.results .legend`.
- **Reglas sin uso:**
  - `.score-panel .eyebrow`;
  - `.sizes`;
  - `.page-size select` (la sobrescribe `.results .page-size .inp`);
  - `#job-ok[hidden]` y `.file-chip[hidden]` (ya los cubre `[hidden]`).
- **Corrección:** unir cada pareja en una regla y quitar las muertas, sin cambiar lo que se ve.
- **Criterio:** capturas de Resultados, Cargar y Acerca idénticas antes y después, comparadas píxel a
  píxel o a ojo y anotadas en `progreso/revision-3.md`.

### T-23 — Tres maneras de agrupar miles, y cifras sin agrupar
- `CatalogTexts.grouped` (web) e `ingest.mapping.Thousands` hacen lo mismo.
- Los votos salen sin separador («21911 votos», `ResultCards`, l. 135-140), igual que el título del conteo
  de Resultados (`ResultsPage`, l. 98-103).
- **Corrección:** un solo helper, usado en todos esos sitios.
- **Criterio:** MockMvc muestra «21 911 votos».

### T-24 — Duplicados y costo por sondeo en la carga
- **Duplicados:**
  - `causeType` está en `ImportJob` y en `JobStore`;
  - `ensureRunning` está en `ImportJob` y en `BatchWriter`;
  - `IMPORT_STEP` y `PUBLISH_STEP` están declarados en tres clases;
  - el JSON/CSV por nombre de archivo está en `FileGate.unreadable` y en `JobFigures.kindOf`;
  - los nombres de los vectores están en cinco sitios (usar `VectorFields.NAMES`).
- **`MappingValidator` creado a mano:** `FileIntake` (l. 38) hace `new MappingValidator()` aunque es un
  `@Component`. `docs/diseno/3.md` dice que se inyecta.
- **Costo por sondeo:** `ImportWork.view` reconstruye en cada sondeo de 1 s la lista de errores (dos
  copias), el `HashSet` de `invalidRecords` y las advertencias agrupadas, bajo el candado del trabajo.
  - Calcularlos una vez en `ready` e `invalid`.
  - Considerar un tope de issues guardados por trabajo.
- **BOM invisible:** `csv/CsvMovieReader.java` (l. 33) tiene el carácter U+FEFF literal. Escribirlo como
  `'﻿'`.

### T-25 — Configuración y beans armados para las pruebas
- **Respaldos solo para pruebas:** `IngestLimits` construye `new ElastiflixProperties(null, …)` si falta
  el bean, y `PosterUrls` usa `Poster.fallback()`. Es código de producción hecho para las pruebas.
  Inyectar `ElastiflixProperties` y que las pruebas lo importen.
- **`@Validated` sin uso:** está puesto, con `spring-boot-starter-validation` en el pom de core, pero no
  hay ninguna restricción en `src/main`. Agregar restricciones (`@NotBlank` en `url`, `alias` e
  `inference-id`) o quitar ambos.
- **Grupos sin uso en Java:** `elasticsearch` y `bootstrap` de `ElastiflixProperties` no se leen desde
  Java. Uno lleva la API key en un bean, aunque enmascarada. Documentarlo en `docs/diseno/1.md` o
  quitarlos.

### T-26 — Accesibilidad
- Las pestañas `role="tab"` de «Cargar datos» no usan `tabindex` itinerante ni flechas.
- `#archivo` oculto puede recibir el foco sin que se vea, porque `.drop` no tiene `:focus-within`.
- `results.html` (l. 15) deja `aria-describedby="q-error"` aunque `#q-error` no exista.
- El `h1` de Resultados no tiene `aria-live="polite"` y la `section` no tiene `aria-labelledby`, como el
  mockup.
- Falta el separador `border-top` antes de APLICAR que tiene el mockup.

### T-27 — Pruebas que no pueden fallar, débiles o con higiene pendiente
*(Resto de S-01, S-05 y S-14)*

| Prueba | Problema | Corrección |
|---|---|---|
| `CatalogIndexIT`, l. 91 | `listOwnedIndexes(alias).isEmpty()` siempre es verdadero: `-a` y `-b` no llevan fecha. `IngestIT` ya cubre un índice con fecha. | Afirmar un índice con fecha, o quitar la aserción. |
| `ImportJobTest.shouldCancelTheCopyBeforeDeletingTheTemp` | El orden lo garantiza el propio doble. No cubre «si la copia falla». | `reindex` lanza `EFX-ES-006`; `InOrder` entre `reindex` y `deleteIndex(temp)`; un solo `deleteIndex`. |
| `ElastiflixPropertiesTest` (S-05) | Pasa aunque se quite cualquiera de los dos `@DefaultValue`, porque los constructores compactos rellenan lo mismo. La tercera prueba repite la primera. | Quitar el relleno de `null` del camino de enlace, o anotar en Decisiones que no se puede distinguir. Quitar la prueba repetida. |
| `MappingValidatorTest.shouldAcceptTheJsonFixture` | Los vectores se quitan en tres capas. | Queda resuelta con T-12. |
| `ScoreJoinerTest`, l. 39 | Prueba la fórmula del helper, no el joiner. | Probar el joiner. |
| `EnvironmentIT` | Su aserción nunca falla: el `EnvironmentPostProcessor` corta antes. | Probar el post-procesador o quitarla. |
| `ElasticsearchClientConfigTest` | Nombra una clase que no existe; la real es `ElasticsearchJsonConfig`. | Renombrar. |
| `JobFiguresTest.shouldGiveTheSummaryWithoutTheCode` | Pasa si `failureMessage` devuelve `""`. | `isEqualTo(ErrorCode.JOB_FAILED.summary())`. |
| `IngestPageTest` | `shouldShowTheAuthorizedCopy` y `shouldShowTheRejectedFileCopy` solo afirman texto fijo de la plantilla. `shouldRedirectAfterSavingOneMovie` no verifica el flash `savedTitle`. | Afirmar datos del modelo y el flash. |
| `ErrorPageTest` | No verifica el pie en la página EFX. | Agregar la aserción. |
| `JobThreadFailureTest` | Deja dos archivos temporales en el tmp del sistema (l. 86-87). | Usar `@TempDir`. |
| Líneas de más de 120 caracteres | `InferenceEmbeddingServiceTest`, l. 56; `KnnStrategiesTest`, l. 105 y 144; `JobThreadFailureTest`, l. 36, 51, 69 y 85; `JobRetentionTest`, l. 34; `NumberRangeTest`, l. 48. | Partirlas. |
