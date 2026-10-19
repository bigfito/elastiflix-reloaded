# Correcciones de la revisión 2 — Elastiflix-Movies Java

> **Terminada y fusionada en `main`** (PR #13, merge `7f08810`; estado posterior en el PR #14, `27fcdef`).
> No la reabras: es la especificación histórica. La evidencia está en `progreso/revision-2.md` y lo que
> quedó pendiente continúa en `docs/revisiones/revision-3.md`.

> Segunda revisión exhaustiva del código y la documentación de `main` en el commit `41c1188`
> (2026-10-18), después de la revisión 1 (R-01…R-30) y de los bugs B-01…B-05.
> 27 correcciones: 3 críticas, 16 importantes y 8 menores. Cada una se verificó leyendo el código actual.
> Checkstyle y PMD siguen en 0 violaciones. La revisión no pudo compilar ni ejecutar pruebas.
>
> **Estado del trabajo:** `progreso/revision-2.md` y `progreso/INDICE.md`. Esta lista es la especificación;
> no marques nada aquí, solo en `progreso/`.
>
> **Correcciones de la revisión 1 que quedaron bien:** R-01, R-05 (código), R-06 (casi todo), R-07,
> R-08, R-10 (código), R-13, R-20, R-22, R-23, R-25 (código), R-26, R-27 (casi todo), R-29 y R-30
> (casi todo).
>
> **Correcciones marcadas como terminadas que no cumplen su criterio:**
>
> | Corrección | Ver |
> |---|---|
> | R-04 | S-02, S-03 |
> | R-09 | S-05 |
> | R-11 | S-04, S-07 |
> | R-12 | S-06 |
> | R-15 | S-01 |
> | R-16 | S-10 |
> | R-18 | S-14 |
> | R-19 | S-19 |
> | R-21 | S-18 |
>
> B-04 reabrió R-15.

---

## Cómo trabajar esta lista

Sirve igual para **Claude Code**, **Google Antigravity** o **JetBrains Junie**: todos leen
`AGENTS.md` en la raíz (Claude Code lo importa desde `CLAUDE.md`).

1. Aplica el protocolo de reanudación de `AGENTS.md` y lee `PLAN.md`, `AGENTS.md` y
   `docs/convenciones-java.md`. Las reglas no cambian: mismas convenciones, mismos umbrales y `config/`
   congelado.
2. El estado vive en `progreso/revision-2.md` (unidad «Revisión 2», tareas `S-01` … `S-27`) y en
   `progreso/INDICE.md`.
   - Si la unidad dice `no iniciado`, márcala `en curso` al empezar S-01 y anota la herramienta.
   - Si ya hay avance, continúa desde su «Próximo paso». Puede ser otro agente el que lo dejó.
3. Trabaja en la rama `revision-2`, creada desde `main`. Si ya existe en `origin`, continúa en ella. No
   hagas push directo a `main`: al terminar, abre un PR hacia `main` y deja que el usuario lo fusione.
4. Atiende las correcciones **en el orden de la lista**.
5. **No marques una corrección como terminada si no cumples su criterio completo.**
   - Si algo del criterio no se puede hacer, deja la corrección `bloqueada`, explica qué falta y pregunta.
   - En la revisión 1, varias correcciones quedaron marcadas como terminadas sin estarlo. La evidencia
     debe nombrar la prueba que falla si se rompe el comportamiento, y comprobarlo rompiendo el código a
     mano y revirtiendo.
6. **Al terminar cada corrección:**
   - cumple su criterio;
   - actualiza `progreso/revision-2.md` y `progreso/INDICE.md` con la evidencia y el **hash real** del
     commit anterior;
   - haz un commit `S-NN: <resumen>`;
   - haz `git push` de la rama.
7. Si cambias algo de un diseño aprobado, anótalo en «Cambios respecto al diseño aprobado» de
   `docs/diseno/<fase>.md`. Si cambias un texto de los mockups, agrégalo a la tabla de ajustes
   autorizados de PLAN §1.
8. **Cuándo detenerte a preguntar al usuario:**
   - en S-18, para aprobar las capturas nuevas;
   - cuando un criterio no se pueda cumplir.

   Las decisiones abiertas de S-03, S-05 y S-06 las tomas tú: elige la opción más simple que cumpla el
   criterio y regístrala en «Decisiones» de `progreso/revision-2.md`.
9. **Al final:**
   - `./mvnw verify` y `./mvnw verify -Pit` en verde;
   - capturas nuevas (S-18);
   - `README.md`, PLAN y `progreso/` al día;
   - deja el estado `terminado` en `progreso/revision-2.md` y en `progreso/INDICE.md` (cuando el PR se
     fusione, el próximo paso queda en «ninguno»);
   - abre el PR, detente y entrega un resumen con la evidencia de cada `S-NN`.

---

## 🔴 Críticas

### S-01 — La copia del índice se corta a los 30 s, y el reindex sigue escribiendo después de borrar el temporal
*(Regresión de B-04; resto de R-15)*

- **Dónde:**
  - `elastiflix-core/src/main/java/com/bigfito/elastiflix/core/catalog/ReindexTasks.java` (`await`,
    `requireFinished`);
  - `ElasticsearchCatalogIndex`;
  - `ImportJob.rollback`;
  - `CatalogBootstrap`.
- **Problema:**
  - `await` hace **una sola** lectura `_tasks/<id>?wait_for_completion=true&timeout=30m` por el cliente
    compartido. No hay `spring.elasticsearch.socket-timeout` ni `Rest5ClientBuilderCustomizer`, así que
    vale el valor por defecto de Spring Boot: **30 s** (confírmalo en `ElasticsearchProperties`).
  - Con `wait_for_completion=true`, Elasticsearch no responde hasta que la tarea termina. Si la copia
    tarda más de 30 s (el catálogo tiene 6 899 películas con tres HNSW):
    1. se lanza `IOException`, que sale como `EFX-ES-001`;
    2. se borra el temporal **mientras la tarea sigue en el servidor**;
    3. el siguiente lote del reindex vuelve a crear `<alias>-<fecha>` con mapping dinámico, y queda un
       índice huérfano.
  - En la migración (1.8) ese huérfano no se limpia nunca.
  - Tampoco se cumple lo que pedía R-15:
    - nadie revisa `failures` ni `timed_out` del cuerpo de la tarea (`task.response()`);
    - no se cancela la tarea antes de borrar el temporal;
    - si `taskId` viene vacío, la copia se da por terminada sin haberla esperado.
  - `ReindexTasksTest` solo cubre `error` y `completed=false`. Este último caso no ocurre: al agotar el
    `timeout`, Elasticsearch responde con un error.
- **Corrección:**
  - Esperar en un **bucle** de lecturas `_tasks/<id>?wait_for_completion=true&timeout=20s`, cada una por
    debajo del socket-timeout, con un plazo total de 30 min medido con `Clock`. Sin `Thread.sleep`: la
    espera del servidor hace de pausa.
  - Tratar como «sigue corriendo» tanto `completed=false` como el error `timeout_exception`.
  - Al terminar, revisar `failures` y `timed_out`. Con cualquiera de los dos → `EFX-ES-006`.
  - Si vence el plazo, llega un `IOException` o la persona cancela: `POST _tasks/<id>/_cancel` (best
    effort, con `WARN` si falla) **antes** de borrar el temporal. Para eso, la API del catálogo tiene que
    exponer el id de la tarea, o `ReindexTasks` cancela dentro de su propio manejo.
  - Si `taskId` viene vacío → `EFX-ES-006`.
  - Documentar en `docs/diseno/1.md` y PLAN §7 cómo queda el tope. Si se cambia
    `spring.elasticsearch.socket-timeout`, agregarlo a la tabla de §7.
- **Criterio:** `ReindexTasksTest` con el cliente simulado cubre seis casos:
  1. una respuesta `timeout_exception` y después `completed=true` → termina;
  2. `failures` no vacío → `EFX-ES-006`;
  3. `timed_out=true` → `EFX-ES-006`;
  4. plazo vencido → se llama a `cancel` y sale `EFX-ES-006`;
  5. `IOException` → se llama a `cancel`;
  6. sin `taskId` → `EFX-ES-006`.

  `ImportJobTest` verifica que, si la copia falla, la cancelación ocurre antes de `deleteIndex(temp)`.

### S-02 — «Cargar datos» sigue sin reproducir `Ingest.dc.html`: contenedor, botones y estilos
*(Resto de R-04)*

- **Dónde:**
  - `elastiflix-web/src/main/resources/static/css/elastiflix.css` (`.ingest`, aprox. l. 324-368;
    `.pill.is-on`, l. 139);
  - `templates/ingest.html`.
- **Problema:** la captura `docs/capturas/cargar.png` muestra:
  - el contenido pegado al borde izquierdo, sin contenedor;
  - «Limpiar» y «Guardar película» como botones nativos del navegador.

  Además:
  - `.ingest` solo tiene `display:flex; flex-direction:column; gap:24px`. Le faltan `flex:1`,
    `max-width:1040px`, `margin:0 auto` y `padding:32px 20px 72px`, como el mockup.
  - No hay reglas para los botones de `.ingest` («Limpiar», «Guardar película», «Validar archivo»,
    «Importar N registros», «Cancelar importación») ni para su estado deshabilitado (`#EFE7DC` /
    `#6B6578`). Les faltan los iconos.
  - La pestaña activa usa `.pill.is-on`, con fondo oscuro. El mockup pide el pastel de cada pestaña
    (`#FFCDB2` / `#FBE7A1` / `#BDE8D7`), `border-radius:10px` e icono SVG.
  - `.ingest .inp` y `.drop` usan fondo `#FFF`; el mockup usa `#FBF7F1`.
  - La etiqueta superior no tiene icono, y su margen se suma al `gap`.
  - En los pasos 1–5, `<strong>`, `<em>` y el detalle quedan en la misma línea. El estado debe ir con la
    tinta de su color y no en cursiva.
  - No tienen estilo:
    - la alerta `#job-stopped` (fondo `#FDEEF5`, `h3` Bricolage 21 px);
    - las tablas de errores y de columnas (padding, `th` mono en mayúsculas);
    - la lista de comprobaciones (icono verde);
    - la caja de advertencias (`#FFF9E3` con icono);
    - el `h2` de `.block`.
  - «Importación» está en la misma tarjeta que la validación; el mockup usa dos tarjetas.
- **Corrección:** completar el CSS y la estructura con las medidas del mockup, sin cambiar textos salvo
  los ajustes de PLAN §1.
- **Criterio:** capturas nuevas de «Cargar datos» en tres escenarios:
  - registro individual;
  - CSV válido validado (paso 4 «listo para empezar»);
  - CSV inválido.

  Las tres coinciden con el mockup en contenedor, pestañas, botones, campos, pasos, tarjetas, tablas,
  alertas y listas. Ver S-18.

### S-03 — «Cargar datos»: faltan datos y textos del trabajo de carga
*(Resto de R-04)*

- **Dónde:**
  - `elastiflix-ingest/.../job/JobView.java`, `JobFigures.java`, `ImportWork.java`;
  - `elastiflix-web/.../static/js/ingest.js`;
  - `templates/ingest.html`.
- **Problema:**
  - **«Válidos» siempre sale en 0 si hay un error.** `JobFigures.validCount` devuelve `fileCount` solo
    si `errors == 0`. Además, con `invalido`, `fileCount` queda en 0 porque solo se fija en `ready()`.
    El mockup muestra 512 leídos, 495 válidos y 23 errores, y «0 de 512 importadas».
  - **`JobFigures.checks()` agrega una comprobación que PLAN §1 elimina:** «Las columnas de vector, si
    vienen, se ignoran…». Solo debe ir la advertencia.
  - **Lotes con tamaño fijo:** `JobFigures.BATCH = 100` no sigue a `elastiflix.ingest.batch-size`.
  - **«Reemplazarán existentes» se mueve durante la importación:** `progress()` pisa `replaceCount` con
    el conteo parcial, así que la cifra baja y vuelve a subir.
  - **La tabla «Correspondencia de columnas» nunca se llena:**
    - es una fila fija con una nota inventada;
    - `JobView` no trae columnas;
    - faltan los estados «Asignada», «Se ignora · se generará desde plot», «Sin datos» y «No existe en
      el mapping»;
    - debe mostrarse solo después de validar un CSV.
  - **Textos de la importación incompletos:**
    - el progreso no dice «· índice temporal <alias>-<AAAAMMDD-HHMMSS>», porque `JobView` no expone el
      temporal;
    - falta el texto completo «Se importarán N películas en L lotes de 100 dentro de un índice
      temporal…»;
    - falta «La importación está bloqueada: …» en el escenario inválido;
    - en la alerta de error faltan «en M registros», las negritas y «Corrige el archivo y vuelve a
      cargarlo; la carga solo continúa cuando todo el archivo coincide con el mapping.»;
    - la columna de ubicación debe decir «Registro» en JSON y «Fila» en CSV.
  - **Código duplicado:** en un trabajo fallido sale «EFX-IMP-007 · EFX-IMP-007 · …», porque
    `failureMessage` usa `userMessage()` y el JS antepone el código. Usar `summary()`.
  - **Estados del proceso:**
    - `cancelado` deja el paso 4 «en curso» para siempre;
    - Importar y Cancelar no se deshabilitan al hacer clic, y abren varias cadenas de consulta;
    - `.then(poll)` ignora 404/409;
    - un fallo de red corta la consulta sin aviso;
    - «Cancelar importación» se habilita con `valido`, y en el mockup está deshabilitado;
    - las cifras salen sin separador de miles («6899»; el mockup usa «4 322»);
    - `aria-valuemax="0"` no es válido.
  - **Bloque «No se puede cargar este archivo» incompleto:**
    - falta «El archivo se rechazó en el navegador, antes de subirlo, así que no salió de tu equipo ni se
      importó nada.» y la lista de motivos;
    - el tamaño debe decir «El archivo pesa X MB y el máximo permitido es 150 MB.»;
    - el paso 1 no queda «detenido»;
    - falta la ficha del archivo con «Cambiar archivo»;
    - el bloque debe ir después de «Proceso de carga»;
    - no debe reutilizarse para errores del servidor o de red, con textos que entonces son falsos;
    - un error nuevo debe ocultar el trabajo anterior, y cambiar de pestaña debe limpiar.
  - **Textos que no están en el mockup ni en §1:**
    - el botón «Validar archivo»;
    - la nota CSV «Si se desmarca, el archivo se rechaza…»;
    - «Elige un archivo .json o .csv.»;
    - «· detectado» y «· detectada» sin que haya detección;
    - el aviso de guardado individual en color de error.

    Decidir cada uno: quitarlo o agregarlo a §1.
  - **`encabezado=false`:** se rechaza dentro de la validación con el título «los datos no coinciden con
    el mapping». PLAN §5 dice que la subida comprueba el encabezado. Rechazarlo en la subida (400 JSON
    `EFX-ING-005`) o en el navegador.
  - **El límite de 150 MB** está fijo en el JS y en las etiquetas, aunque
    `ELASTIFLIX_INGEST_MAX_FILE_SIZE` es configurable. Pasar el valor configurado a la plantilla.
- **Corrección:**
  - Agregar a `JobView`:
    - `fileCount` también en `invalido`;
    - `validCount` = `fileCount` menos los registros distintos con al menos un error;
    - `invalidRecords`;
    - `tempIndex`;
    - `batchSize` configurado;
    - la lista de columnas (columna, campo, contenido, estado);
    - `replaceCount` aparte del progreso.
  - Pintar todo en `ingest.js` con los textos del mockup.
- **Criterio:**
  - Pruebas de `JobFigures`/`ImportWork`: 512 leídos y 23 errores en 17 registros dan 495 válidos.
  - MockMvc y JS (o una prueba de la plantilla) verifican los textos exactos de los tres escenarios del
    mockup.
  - `encabezado=false` tiene prueba.

---

## 🟠 Importantes

### S-04 — Publicación: proteger el temporal ya publicado y no borrar ante un fallo que no es 4xx
*(Resto de R-02, R-11 y B-01)*

- **Dónde:**
  - `elastiflix-ingest/.../job/ImportJob.java` (`importInto`, `publish`, `swap`, `rollback`,
    `resolvePublish`);
  - `elastiflix-core/.../catalog/CatalogBootstrap.java` (`confirmOrReject`);
  - `ElasticsearchCatalogIndex.publish` e `indexOne`.
- **Problema:**
  - **No existe la bandera `published` que pedía R-11.** Dentro del mismo `try`, después de `swap`,
    corren `listener.onProgress(...)` y el log. Si algo de eso lanza (por ejemplo, un trabajo expulsado
    cuando se implemente S-06), `rollback` borra el temporal, que **ya es el índice vigente**, y el
    anterior ya se borró: el catálogo queda vacío.
  - **Sin `finally`:** una excepción que no es `ElastiflixException` (por ejemplo `JsonpMappingException`
    o NPE) deja el temporal huérfano.
  - **Cualquier fallo de `publish` permite borrar.** El índice nuevo se borra siempre que `describe`
    diga «anterior» o «índice concreto», aunque el fallo haya sido un timeout del cliente. R-02 pedía que
    solo un 4xx contara como rechazo seguro. Con un master lento, `_aliases` puede aplicarse después del
    `describe` y antes del `deleteIndex`, y entonces se borra el catálogo vivo.
  - **`_aliases` va sin `master_timeout` ni `timeout`.**
  - **`indexOne` (registro individual) no usa `require_alias=true`:** si falta el alias, crea un índice
    concreto con ese nombre.
- **Corrección:**
  - `boolean published = true` en cuanto `swap` vuelve. Sacar el aviso y el log del `try`. En un
    `finally`, borrar el temporal solo si `!published` y el error no es `PUBLISH_UNCERTAIN`.
  - En `swap`, tratar también una `RuntimeException` ajena con `resolvePublish`.
  - En `publish`, pasar `masterTimeout(10s)` y `timeout(10s)`, ambos menores que el socket-timeout.
  - Borrar el nuevo **solo** si la causa es un `ElasticsearchException` 4xx **y** `describe` confirma el
    estado anterior. Un corte de red o un 5xx → `EFX-IMP-006` sin borrar. Aplicarlo igual en
    `CatalogBootstrap`.
  - `require_alias=true` en `indexOne`.
- **Criterio:** pruebas en `ImportJobTest` y `CatalogBootstrapTest`:
  - un listener que lanza en el paso 5 no llama a `deleteIndex(temp)`;
  - `publish` con `IOException` y `describe` en el estado anterior → sin borrado y `EFX-IMP-006`;
  - `publish` con 4xx y `describe` en el estado anterior → se borra el nuevo.

### S-05 — El `input_type` y el prefijo de documento siguen con dos fuentes
*(R-09 sin resolver)*

- **Dónde:** `elastiflix-core/.../config/ElastiflixProperties.java` (el parámetro raíz
  `Embedding embedding` sin `@DefaultValue`; `Embedding.fallback()`; constantes
  `DEFAULT_DOCUMENT_INPUT_TYPE` y `DEFAULT_DOCUMENT_PREFIX`).
- **Problema:**
  - Sin propiedades `elastiflix.embedding.*`, que es el caso real, Spring deja `embedding = null`. Se
    usan entonces `fallback()` y las constantes, y los `@DefaultValue` nunca se aplican.
  - El commit `c4f0d23` volvió configurables la consulta (`search`, `Query: `), algo que el plan no pide.
    El Javadoc de `EmbeddingService.embedQuery` sigue diciendo que son fijos.
- **Corrección:**
  - Anotar `@DefaultValue Embedding embedding` en el record raíz.
  - Que el literal calibrado exista en **un solo** sitio: `@DefaultValue(Embedding.DEFAULT_…)` y el
    constructor con esa misma constante, o quitar las constantes y `fallback()`.
  - Decidir sobre la parte de consulta: volver a constantes, o documentarla en PLAN §7 y en el Javadoc.
- **Criterio:** una prueba con binding real (`ApplicationContextRunner`) fija solo
  `elastiflix.embedding.document-prefix=` y comprueba que `documentInputType` sale del `@DefaultValue`.
  Otra prueba, sin ninguna propiedad, comprueba los dos valores calibrados.

### S-06 — Memoria de la ingesta: los vectores se quitan tarde y los trabajos nunca se expulsan
*(Resto de R-12; D2)*

- **Dónde:**
  - `elastiflix-ingest/.../json/JsonDocuments.java` (`readElements`, `readNdjson`);
  - `mapping/MappingValidator.java` (`dropVectors`);
  - `job/JobStore.java` (un `ConcurrentHashMap` sin expulsión);
  - `job/FileIntake.java`.
- **Problema:**
  - `dropVectors` corre en el validador, cuando el archivo entero ya está en memoria con 1 792 números
    por película: el pico de memoria no baja. En el pico conviven los nodos crudos, `bodies` y las
    `Movie`.
  - **No se implementó «conservar solo los últimos N trabajos».** Un trabajo `valido` que no se importa
    no es terminal: retiene la lista de películas y el archivo de hasta 150 MB en `/tmp` para siempre.
- **Corrección:**
  - Al leer, saltar `plot_256`, `plot_512` y `plot_1024` con el parser (`skipChildren()`), o quitarlos
    de cada nodo apenas se lee, y registrar ahí la advertencia `EFX-ING-020`.
  - Liberar los nodos crudos a medida que se validan.
  - Borrar el archivo local al terminar la validación: ya no se relee.
  - Expulsar trabajos terminados con un tope (por ejemplo 20) y vencer los `valido` sin actividad.
    **Nunca expulsar uno en `importando`** (ver S-04).
- **Criterio:** pruebas que verifican cuatro cosas:
  - los nodos leídos no traen vectores;
  - después de un estado final, el trabajo no retiene películas;
  - con más de N trabajos terminados, se expulsan los más viejos;
  - un trabajo `importando` no se expulsa.

  La documentación de D2 se corrige en S-15.

### S-07 — Números fuera de rango y conversiones que tumban el trabajo o la página
*(Resto de R-11)*

- **Dónde:**
  - `elastiflix-ingest/.../mapping/IdTexts.java`;
  - `ScalarValues.fitsInt`;
  - `NestedValues.writeId`;
  - `csv/CsvValues.java`;
  - `elastiflix-web/.../ingest/MovieForms.java` (`convertValue`).
- **Problema:**
  - En Jackson 3, `JsonNode.longValue()` lanza `JsonNodeException` si el valor no cabe en un `long`. Un
    `id` o un `vote_count` de 25 dígitos en JSON termina en `EFX-IMP-007` («el trabajo se interrumpió»),
    sin fila ni campo.
  - En CSV:
    - los enteros aceptan `1e3` y `12.0`, porque `intValueExact` sobre `BigDecimal` los deja pasar;
    - un `float` como `1e39` pasa la validación y falla después en el `bulk`;
    - `trim()` recorta `title`, `plot` e `id`, mientras que `IdTexts` considera el espacio parte del id.
  - En el formulario individual, `vote_count=1e3` o un `runtime` de 11 dígitos dan una página 500 sin
    código. R-11 pedía `EFX-ING-012` y una prueba.
- **Corrección:**
  - Usar `canConvertToLong()`/`bigIntegerValue()` y parsear los enteros con `BigInteger`. Fuera de rango
    → `EFX-ING-012`.
  - Rechazar `float` fuera del rango finito.
  - Recortar solo para detectar celdas en blanco.
  - En `MovieForms`, capturar la excepción concreta de conversión de Jackson y devolver el rechazo con
    `EFX-ING-012` y el campo.
- **Criterio:** pruebas con:
  - una celda CSV de 25 dígitos;
  - un número JSON de 25 dígitos;
  - un entero CSV `1e3`;
  - un `float` `1e39`;
  - `vote_count=1e3` en el formulario (MockMvc).

  En todas, error de fila o rechazo con código, y el trabajo nunca colgado ni en 500.

### S-08 — CSV: el número de fila de los errores no corresponde a la fila del archivo
*(Nuevo; viene del camino de R-03 y R-13)*

- **Dónde:** `mapping/MappingValidator.java` (numera documentos desde 1); `csv/CsvMovieReader.java`
  (numera filas desde 2); `service/MovieReaderFactory.csvDocuments`; `job/FileIntake.finish`.
- **Problema:**
  - En `csv-bad-date.csv`, la fecha mala está en la línea 2 y el informe dice «Fila 1», que es el
    encabezado. Las filas en blanco desplazan aún más los números.
  - Además, si hay un error de ancho (`EFX-ING-014`), los documentos se descartan y `MappingValidator`
    no corre: el informe deja de ser completo.
- **Corrección:**
  - `CsvMovieReader` entrega la etiqueta de fila real de cada documento y `MappingValidator` la usa.
  - Validar también las filas de ancho correcto y unir los dos informes.
- **Criterio:** pruebas en dos casos:
  - la fecha mala de la línea 2 sale como «Fila 2»;
  - una fila corta más 2 fechas malas dan 3 errores en el informe.

### S-09 — La comprobación UTF-8 del navegador rechaza archivos válidos
*(Nuevo; viene de R-04)*

- **Dónde:** `static/js/ingest.js` (`readUtf8`).
- **Problema:** `file.slice(0, 65536)` puede cortar un carácter de varios bytes («é», «ñ», «기»), y
  `TextDecoder(..., {fatal:true}).decode(buffer)` sin `stream` lanza un error. El archivo válido se
  rechaza con «El contenido no es texto UTF-8.».
- **Corrección:** `decode(buffer, { stream: true })`.
- **Criterio:** una prueba (o un caso documentado con un archivo de muestra) con un carácter de 2 bytes
  en la posición 65 535 se acepta.

### S-10 — Las sugerencias de Inicio siguen perdiendo los ajustes
*(Resto de R-16)*

- **Dónde:** `static/js/home.js` (`keepChosenSettings`, `checkKnn`); `SearchHref.java`.
- **Problema:**
  - `keepChosenSettings` solo corre en `refresh()`. Al escribir `k=50` en el campo, el enlace sigue con
    `k=100`.
  - Las sugerencias híbridas fijan `tipo=match&campo=title`, y el JS no los reemplaza.
- **Corrección:** como pedía R-16, al hacer clic en `.chip`, cancelar la navegación, poner el texto en
  `q` y hacer `form.requestSubmit()` con los ajustes actuales. El `href` del servidor queda como respaldo
  sin JS.
- **Criterio:** una prueba de JS, o de navegador con Playwright si está disponible, demuestra que con
  `k=50`, `vector=plot_1024` y `multi_match` en Híbrida, la URL resultante conserva esos tres valores.

### S-11 — Resultados: un filtro marcado desaparece si no está en las facetas
*(Nuevo)*

- **Dónde:** `elastiflix-web/.../FacetChoices.java` (`of`); `templates/results.html`.
- **Problema:**
  - Solo se pintan los valores que trae la faceta. Un valor marcado que ya no aparece se pierde del
    formulario: no se puede desmarcar y el siguiente APLICAR lo quita sin avisar.
  - Es seguro que pasa en «Ningún resultado cumple los filtros»: las facetas llegan vacías y desaparecen
    todos los marcados, aunque el texto dice «Desmarca algún valor…».
  - Además, un valor marcado en la posición 7 o más recibe la clase `extra` y queda oculto.
- **Corrección:** agregar los valores de `selected` que falten con conteo 0, como `mk(...)` del mockup, y
  no ocultar los seleccionados.
- **Criterio:** una prueba MockMvc con facetas vacías y `genero=Drama` muestra «Drama 0» marcado y
  visible.

### S-12 — «Acerca de»: las tarjetas de modo salen apiladas
*(Nuevo)*

- **Dónde:** `static/css/elastiflix.css` (`.about section`, `.mode-cards`, `.about p`).
- **Problema:**
  - `.about section { flex-direction: column }` le gana a `.mode-cards`, porque `.mode-cards` es una
    `<section>`. Las tres tarjetas salen en columna y de 280 px de alto cada una (se ve en
    `acerca.png`); el mockup las pone en fila.
  - Faltan los cuadros de icono de 44 px de las tarjetas y de los encabezados, y el panel celeste
    «Cómo se calcula la relevancia: BM25».
  - `.about p` pinta de gris la etiqueta superior, que debe ser `#A9502A`.
- **Corrección:**
  - `.about .mode-cards { flex-direction: row; }`;
  - agregar los iconos y el panel;
  - `.about .eyebrow { color: var(--accent-ink); }`.
- **Criterio:** una captura de página completa de «Acerca de» coincide con el mockup (ver S-18).

### S-13 — Resultados: estilos del mockup pendientes
*(Resto de R-06 y R-17)*

- **Dónde:** `templates/results.html`; `static/css/elastiflix.css`; `static/js/results.js`.
- **Problema:**
  - **Leyendas de los filtros:** salen en mono 13 px, color tinta y sin mayúsculas. El mockup (`.fh`)
    pide 12 px, peso 500, `letter-spacing .1em`, mayúsculas y `#A8336B`, con el `code` en `#6B6578`.
  - **Paginación:** son enlaces sin estilo. Faltan `.pg` (44×44, borde, mono), la página actual en
    `#FFCDB2` y los iconos de Anterior y Siguiente.
  - **Póster faltante:**
    - el marcador no tiene el SVG del mockup;
    - el `onerror` en línea no pone `aria-label`;
    - no hay prueba con `posterPath` vacío.
  - **Tarjeta:**
    - el número de posición debe ir sobre el póster;
    - `h3` en Bricolage 21 px;
    - meta en mono 12 px;
    - estrella en los votos;
    - iconos de lupa y flecha en «Buscar», de embudo en «Filtros» y en la nota de facetas;
    - estilo de `p.limit`.
  - **Caja de consulta con error:** `#q` sin `aria-invalid`/`aria-describedby` ni borde rojo, y sin `h1`
    en la página.
- **Corrección:** aplicar el mockup.
- **Criterio:**
  - Captura nueva de Resultados en los tres modos (ver S-18).
  - MockMvc con `posterPath` vacío muestra el marcador con `aria-label`.

### S-14 — Las pruebas que pedía R-18 no se hicieron
*(Resto de R-18; incluye core e ingest)*

- **Problema y corrección:**

  | Prueba | Qué falta |
  |---|---|
  | `ResultsPageTest` | Un mock con varias páginas. Verificar que Anterior, Siguiente y los números conservan `genero` y `desde`, y que el selector a 50 lleva `pagina=1` con los filtros. Quitar aserciones de texto que siempre está en el HTML («Tienes cambios sin aplicar.»). |
  | `HomePageTest` | Comparar el `href` **completo** de una sugerencia vectorial y de una híbrida. |
  | `IngestPageTest` | Cubrir `encabezado=false`. El mock de `importar` debe devolver `importando` y después un estado final distinto. |
  | `LayoutPageTest` | El pie con la atribución de TMDB en Resultados, Cargar, Acerca, la página de error y el 404. |
  | `ErrorPageController` | Probar el `/error` real (400, 405 y sin código `EFX`); hoy deja un `h1` vacío. |
  | `IndividualRecordTest` | Usar `eq(alias)` y un `ArgumentCaptor<Movie>` que verifique vectores de 1024, 512 y 256 valores. |
  | `ImportJobTest` | En el caso de éxito, `verify(catalog).createWithMapping(temp)` y `verify(catalog).writeAll(eq(temp), anyList())`. |
  | `InferenceEmbeddingServiceTest` | Probar el servicio con un `ElasticsearchClient` simulado que devuelva menos vectores, no solo el helper estático. |
  | `VectorSearchStrategy` / `HybridSearchStrategy` | `verify(embeddings, times(1)).embedQuery(...)` en el estado vacío con filtros (R-25). Ítem fallido del `_msearch` y número de partes distinto de 3. |
  | `CatalogIndexIT` | `listOwnedIndexes(alias).isEmpty()` siempre es verdadero. Crear un índice con fecha y afirmar que aparece. |
  | `MappingValidatorTest.shouldAcceptTheJsonFixture` | Pasa aunque se borre `dropVectors`. Afirmarlo sobre los nodos validados. |
  | `PlotVectorizerTest` | La prueba «si la inferencia falla» es una tautología. Probar el comportamiento real. |
  | `JsonDocumentsTest` y `CsvMovieReaderTest` | Comparar a través de `MappingValidator`, el camino real. |

- **Criterio:** cada prueba de la tabla falla si se rompe el comportamiento que describe. Compruébalo
  rompiendo el código a mano y revirtiendo, y deja la evidencia en `progreso/revision-2.md`.

### S-15 — El registro de entrega no refleja las decisiones de la revisión 1
*(Nuevo; también resto de R-12)*

- **Dónde:** `progreso/INDICE.md` («Aprobaciones»), `progreso/revision-1.md`, PLAN 3.0 y 3.2,
  `docs/diseno/3.md`.
- **Problema:**
  - **Ya corregido en el commit que agregó esta lista:** el «Punto de reanudación» de INDICE, el cierre de
    `progreso/bugs.md` con el hash de B-05 (`8cbfa49`), las filas de aprobación de las fusiones de los PR
    #10, #11 y #12, y la mención en `CLAUDE.md`. No hay que rehacerlo.
  - **«Aprobaciones» no tiene filas para:**
    - las decisiones D1, D2 y D3 de la revisión 1;
    - la edición del mockup en B-05.
  - **D2 (ingesta en memoria) sigue escrita como streaming:**
    - PLAN 3.0 y 3.2 siguen diciendo «dos pasadas» y «en streaming»;
    - en `docs/diseno/3.md`, el supuesto 9 y «Alternativas descartadas» contradicen lo implementado;
    - «Cambios» no cita D2 ni sus mitigaciones;
    - todavía dice que un 4xx sale como `EFX-ES-001` (desde R-24 es `EFX-ES-007`).
  - **`revision-1.md` marca como terminadas** R-04, R-09, R-11, R-12, R-15, R-16, R-18, R-19 y R-21, que
    no lo están (ver la tabla del encabezado).
- **Corrección:**
  - Agregar las filas de aprobación de D1, D2, D3 y del mockup de B-05.
  - En `revision-1.md`, anotar en cada una de esas R «incompleta: continúa en S-NN».
  - Reescribir PLAN 3.0 y 3.2 y el diseño 3 según D2.
- **Criterio:**
  - «Aprobaciones» tiene una fila por cada una de esas cuatro decisiones.
  - PLAN 3.0 y 3.2 y `docs/diseno/3.md` solo mencionan el streaming como alternativa descartada.
  - Cada R incompleta de `revision-1.md` remite a su `S-NN`.

### S-16 — Tabla de ajustes autorizados de PLAN §1: faltan R-10, R-30 y el cambio del mockup de B-05
*(Resto de R-10, R-30 y B-05)*

- **Dónde:** `PLAN.md` §1 (tabla «Únicos ajustes de texto autorizados»).
- **Problema:**
  - R-10 y R-30 pedían agregar sus filas, y no se agregaron.
  - B-05 editó el mockup aprobado `design/mockups/About.dc.html`, cambiando «`Document: ` en las
    sinopsis» por «`input_type` `ingest` y sin prefijo», sin registrarlo.
  - `about.html` difiere del mockup en «Medido en el catálogo original de 4 322 películas» (tabla de
    precisión, `adult` y `release_date`).
- **Corrección:**
  - Agregar filas con fecha 2026-10-18 para el prefijo (R-10 y B-05) y para los rótulos de «catálogo
    original» (R-30, D3).
  - Cambiar el encabezado a «decisiones del usuario del 2026-10-17 y 2026-10-18».
- **Criterio:** cada diferencia de texto entre las plantillas y los mockups tiene su fila en §1.
  Compruébalo comparando los textos de las cuatro páginas.

### S-17 — Seis documentos dicen que las pruebas solo exigen «conteo mayor que cero»
*(Resto de R-05)*

- **Dónde:**
  - `README.md`, nota «Estado actual» del inicio;
  - `PLAN.md`, l. 64-65;
  - `docs/diseno/1.md`, l. 23 y l. 457;
  - `docs/diseno/5.md`, l. 16-17, l. 38 y l. 134, más su sección «Cambios»;
  - `progreso/5.md`, l. 112;
  - `AGENTS.md`, línea del catálogo.
- **Problema:** con D1, las pruebas afirman los 6 899 documentos y los valores exactos de §6, y se omiten
  con `assumeTrue` si `_count` cambió. Los documentos dicen lo contrario.
- **Corrección:** sustituir cada frase por «afirman los 6 899 documentos y los valores de §6; se omiten
  con `assumeTrue` si el conteo cambió», y agregar R-05/D1 a «Cambios» de `diseno/5.md`.
- **Criterio:** `grep -rn "mayor que cero" README.md PLAN.md AGENTS.md docs progreso` no devuelve nada.

### S-18 — Las capturas y la comparación de R-21 no reflejan la interfaz real
*(Resto de R-21)*

- **Dónde:** `docs/capturas/`; `progreso/4.md`, tabla de comparación.
- **Problema:**
  - **«Cargar datos»:** `progreso/4.md` dice que `cargar.png` muestra «Archivo JSON», la tabla de
    errores, «Importación bloqueada» y el paso 4. La captura solo muestra el registro individual.
  - **Inicio:** dice que la navegación tiene «Resultados», y no lo tiene.
  - **Resultados:** cita chips «Campo: title», que no existen.
  - **Recortes:** `resultados.png` y `acerca.png` se cortan antes de las partes que se describen.
  - **Fecha:** dice 17:45 UTC, pero cuatro capturas se rehicieron a las 18:28.
  - **Sin diferencias reales:** la tabla nunca lista las diferencias de S-02, S-12 y S-13, así que la
    aprobación visual quedó apoyada en una descripción inexacta.
- **Corrección:** después de S-02, S-03, S-12 y S-13:
  - rehacer capturas de **página completa**: Inicio; Resultados en los tres modos; Cargar datos en los
    tres escenarios de S-02; Acerca de; 404;
  - escribir una comparación página por página con las diferencias que queden;
  - pedir de nuevo la confirmación del usuario y registrarla en «Aprobaciones».
- **Criterio:**
  - Cada afirmación de `progreso/4.md` se ve en una captura.
  - La confirmación del usuario es posterior a las capturas nuevas.

### S-19 — Con solo el README todavía no se puede preparar el catálogo
*(Resto de R-19)*

- **Dónde:** `README.md` («Datos y proyecto Serverless», «Configuración»).
- **Problema:**
  - Dice que la tarea 1.8 necesita el índice concreto `elastiflix-reloaded` «ya creado», pero no dice
    cómo crearlo. La aplicación no crea un catálogo vacío.
  - La lista de privilegios omite la lectura de `_tasks`, que desde B-04 necesita el privilegio de
    clúster `monitor` (o el equivalente en Serverless; verifícalo).
  - Los `curl` usan `$ELASTICSEARCH_URL` y `$ELASTICSEARCH_API_KEY` antes de `cp .env.example .env`, y
    `.env` nunca se exporta.
  - Detalles:
    - «`.env.example` solo lista los nombres» es falso;
    - el índice de contenido no incluye «Seguridad y secretos»;
    - el aviso «Esta lista es la que se ejecutó en un clone limpio» ya no es exacto.
- **Corrección:**
  - Documentar la creación del índice:
    `curl -X PUT "$ELASTICSEARCH_URL/elastiflix-reloaded" -H "Authorization: ApiKey $ELASTICSEARCH_API_KEY" -H 'Content-Type: application/json' --data-binary @elasticsearch/elastiflix-reloaded-mapping.json`,
    y después la migración y la importación por `/cargar`.
  - Agregar el privilegio de `_tasks`.
  - Mover el bloque después de «Configuración» y anteponer `set -a; . ./.env; set +a`.
  - Corregir los tres detalles.
- **Criterio:** en un `git clone` limpio y un proyecto Serverless sin el índice, siguiendo solo el README
  se llega a la aplicación funcionando con datos. Registrar la corrida en `progreso/revision-2.md`.

---

## 🟡 Menores

### S-20 — Una API key inválida aparece como «corrige la petición»
*(Regresión de R-24)*
- `ElasticsearchDiagnostics.codeFor` convierte un 401 o un 403 en `EFX-ES-007`. El catálogo de errores
  asigna ese caso a `EFX-ES-001` («La URL o la API key no son válidas»).
- Con la misma clave mala, el pie muestra 001 (los HEAD no traen cuerpo) y Resultados muestra 007.
- `HybridSearchStrategy.rejected` siempre da 007, aunque el ítem del `_msearch` sea un 5xx.
- **Corrección:**
  - 401 y 403 → `EFX-ES-001`, o un código nuevo de credenciales;
  - en la híbrida, usar `codeFor(failure.status())`;
  - agregar 401 a `ElasticsearchDiagnosticsTest`;
  - actualizar `docs/diseno/2.md`, l. 388 y 431.

### S-21 — Códigos y textos de error que no corresponden al contexto
- `REINDEX_COUNT_MISMATCH` dice «Vuelve a ejecutar la migración», pero también sale en una importación.
- `REINDEX_FAILED` dice «el temporal se borra», pero en la migración depende del caso.
- `PUBLISH_UNCERTAIN` da una causa que no aplica cuando `describe` sí respondió.
- `ALIAS_PUBLISH_FAILED` afirma «Se borró el índice temporal», aunque `deleteTemp` haya fallado con solo
  un `WARN`.
- El Javadoc de `ElasticsearchAccessException` dice `EFX-ES-*`, pero se lanza con `EFX-IMP-006`.
- `PlotVectorizer.attach` usa `UNEXPECTED_VECTOR` (`EFX-EMB-002`) para un lote incompleto; desde B-02 es
  `EFX-EMB-004`.
- `FileGate.open` y `MovieDocuments.fromNode` usan `EFX-ING-013` («El JSON no se puede leer») también en
  CSV.
- **Corrección:** textos neutros (importación o migración) o códigos separados, y el código según
  `FileKind`.

### S-22 — Mensajes y página de error genérica
- `ErrorCode.summary()` va sin punto final. Resultados muestra «Escribe qué película buscas antes de
  pulsar Buscar» sin punto, y el mockup y PLAN §5 lo llevan. Lo mismo pasa con k y `num_candidates`.
- `ErrorPageController` pone `code=""` y deja un `h1` vacío en un 400 o un 405. Además, `catalog.status()`
  se llama dos veces por página (`LayoutAdvice` y el propio controlador).
- Sin conexión, el pie dice «· sin películas». Debe decir que el conteo no está disponible.
- PLAN 4.3 todavía dice «mostrado con su código». Desde R-06 solo se muestra `summary()`. Corregir el
  criterio y el Javadoc de `ResultsPageTest`.

### S-23 — Errores JSON de la carga con alcance incompleto
*(Resto de R-07)*
- `IngestJobErrors` solo atiende `ImportException` y `MaxUploadSizeExceededException`. Otra
  `ElastiflixException` o un `IOException` en los endpoints de trabajo sigue devolviendo HTML.
- Al no tener alcance, su manejador de `ImportException` compite con `WebExceptionHandler` en las páginas.
- **Corrección:** `@RestControllerAdvice(assignableTypes = IngestJobController.class)` para
  `ElastiflixException` e `IOException`, y dejar sin alcance solo el de
  `MaxUploadSizeExceededException`.
- **Criterio:** una prueba con un `IOException` simulado recibe JSON.

### S-24 — Detalles de la ingesta
- **Aviso de escritos antes de escribir:** `BatchWriter.writeOne` avisa «escritos» antes de `writeAll`.
  Avisar los vectorizados antes y los escritos después (R-28).
- **Retroceso de estado:** `ImportWork.validating()` puede pasar un trabajo `cancelado` a `validando`.
  No cambiarlo si ya se canceló.
- **`IngestIT.stopJobs`:** si un trabajo no termina en 4 minutos, lanza y `deleteOwned()` no corre. Usar
  `try/finally` para limpiar siempre.
- **Configuración duplicada:** `TestApplication.java` de ingest es un segundo `@SpringBootConfiguration`
  sin uso. Quitarlo si sobra.

### S-25 — Código muerto
- `VectorRequestBuilder.build(String, SearchQuery, float[])`, `Paginator.describe(long, int, int)` y
  `MovieDocuments.read(JsonParser)` solo los usan las pruebas, o nadie (resto de R-27).
- Quitarlos o mover su uso a las firmas reales.

### S-26 — Documentación técnica desactualizada
- `docs/diseno/2.md`, l. 138: el `;` en `Note over St: full-text cuenta; …` rompe el diagrama Mermaid.
  Cambiarlo por «,» y verificar con `mmdc`.
- La regla de la ventana full-text sigue escrita como `from + size > 10000` en PLAN 2.1 y en
  `docs/convenciones-java.md`, l. 195 y 272. El código usa `from >= 10000` (R-01).
- `docs/diseno/3.md`, l. 564, dice que un 4xx sale como `EFX-ES-001` (ver S-20).
- `docs/convenciones-java.md`:
  - l. 204, `record ValidationError` → el tipo real es `ValidationIssue`;
  - l. 198, área «`EFX-WEB`», que no existe.
- PLAN, l. 60-65: repite el índice y el conteo de `progreso/INDICE.md` › «Catálogo vigente». Dejar solo
  la referencia.

### S-27 — Evidencia histórica que quedó sin nota
- `progreso/revision-1.md`:
  - cita `AGENTS.md` con 10 400 y 10 428 caracteres (hoy tiene 10 831);
  - R-23 dice `EFX-EMB-002` (sustituido en B-02);
  - R-15 dice «dos lecturas de `_tasks`» (cambiado en B-04).
  - Agregar «(sustituido por B-0x)» donde corresponda.
- `progreso/6.md`, l. 62 y 87: «No hay `@SuppressWarnings` en el código». Hoy existe uno en
  `ReindexTasksTest.java`, registrado en `diseno/1.md`. Corregir la frase.
