# Pruebas de navegador

Prueban el JavaScript de las páginas (`src/main/resources/static/js/*.js`) en Chromium con Playwright, contra la
aplicación ya arrancada. No forman parte de `./mvnw verify`: necesitan Node.js 20 o más y un navegador.

## Cómo correrlas

1. Arranca la aplicación desde la raíz del repo. No hace falta Elasticsearch: estas pruebas no importan nada.
   `utf8.test.js` y `size.test.js` solo usan la comprobación del navegador en `/cargar`. `ingest.test.js` y
   `mode.test.js` simulan con `page.route()` las respuestas de `/cargar/archivo` y `/cargar/trabajos/*`.
   `suggestions.test.js` intercepta `/buscar` y solo afirma la URL a la que navega la sugerencia.

   ```bash
   ./mvnw -q package -DskipTests
   ELASTICSEARCH_URL=http://localhost:9200 ELASTICSEARCH_API_KEY=test SERVER_PORT=8089 \
     java -jar target/elastiflix-reloaded-0.1.0-SNAPSHOT.jar
   ```

2. En otra terminal, desde esta carpeta (`src/test/browser`):

   ```bash
   npm install
   npx playwright install chromium        # en Linux puede pedir: sudo npx playwright install-deps chromium
   BASE_URL=http://localhost:8089 npm test
   ```

3. Opcional: la última prueba de `mode.test.js` revisa «Cargar datos» cuando el catálogo todavía no existe.
   Necesita una segunda instancia conectada a Elasticsearch con un alias de prueba que no exista, por ejemplo
   `elastiflix-it-sincatalogo`. La página solo lee el estado del alias; la prueba no carga nada. Sin
   `NO_CATALOG_URL` se omite.

   ```bash
   ELASTIFLIX_INDEX_ALIAS=elastiflix-it-sincatalogo SERVER_PORT=8090 \
     java -jar target/elastiflix-reloaded-0.1.0-SNAPSHOT.jar   # lee ELASTICSEARCH_URL y la API key de .env
   BASE_URL=http://localhost:8089 NO_CATALOG_URL=http://localhost:8090 npm test
   ```

## Qué cubren

| Archivo | Comportamiento |
|---|---|
| `utf8.test.js` | Un carácter de 2 bytes cortado en el byte 65 535 no rechaza un archivo válido; un byte inválido sí |
| `about.test.js` | En 1440 px el índice de «Acerca de» está abierto, marca «Tres formas de buscar una película» al cargar, sigue a la vista al bajar y marca la sección elegida o la que se lee; en 375 px empieza plegado, se abre, se cierra al elegir una sección y la página no se desborda a lo ancho |
| `ingest.test.js` | Un 409 al importar deja el aviso visible tras el siguiente sondeo; validar un CSV, cambiar el separador y volver a validar el mismo archivo hace un segundo POST; `cancelado` deja el paso 4 detenido; un doble clic en Importar hace un solo POST; una caída de red al importar o al subir muestra el aviso de red; las pestañas usan `tabindex` itinerante y se recorren con ←, →, Inicio y Fin; al cambiar de idioma con un trabajo CSV activo sigue la pestaña CSV con su separador, y un `?job=` de un JSON elige la pestaña JSON; un `?job=` que no es un UUID no se usa y el id va codificado en cada ruta de `fetch` |
| `mode.test.js` | Append viene marcado, sin aviso de bulk, con el paso 4 «Escritura en el catálogo» y sin paso 5; bulk muestra el aviso y los pasos 4 y 5 de bulk; la subida manda `modo`; cambiar el modo después de validar bloquea «Importar» y vuelve a habilitar «Validar archivo»; `?job=` de un trabajo bulk marca bulk; un append `agregado` deja los pasos 1 a 4 completados. Con `NO_CATALOG_URL`: sin catálogo, el aviso, append deshabilitado, bulk marcado sin aviso de borrado y la cabecera «Conectado · el catálogo todavía no existe» |
| `size.test.js` | La página entrega el tope de 25 MB (`data-max-bytes="26214400"`); un archivo de 25 MB exactos pasa la comprobación del navegador y uno de 25 MB + 1 byte se rechaza sin subirse, con «El archivo pesa 25.0 MB y el máximo permitido es 25 MB.» y su texto en inglés |
| `suggestions.test.js` | En Híbrida, con `k=50`, `plot_1024` y `multi_match`, una sugerencia busca con esos ajustes; sin JavaScript el enlace del servidor sigue llevando a `/buscar` |
