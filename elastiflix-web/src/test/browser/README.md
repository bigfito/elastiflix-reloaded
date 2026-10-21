# Pruebas de navegador

Prueban el JavaScript de las páginas (`static/js/*.js`) en Chromium, contra la aplicación ya arrancada.
No forman parte de `./mvnw verify`: necesitan Node.js 20 o más y un navegador. Se agregaron en la
revisión 2 (S-09 y S-10), donde el criterio pide una prueba de JS o de navegador.

## Cómo correrlas

1. Arranca la aplicación. No hace falta Elasticsearch: estas pruebas no importan nada.
   `utf8.test.js` y `size.test.js` solo usan la comprobación del navegador en `/cargar`. `ingest.test.js` y `mode.test.js` simulan con
   `page.route()` las respuestas de `/cargar/archivo` y `/cargar/trabajos/*`. `suggestions.test.js`
   intercepta `/buscar` y solo afirma la URL a la que navega la sugerencia.

   ```bash
   ./mvnw -q install -DskipTests
   ELASTICSEARCH_URL=http://localhost:9200 ELASTICSEARCH_API_KEY=test SERVER_PORT=8089 \
     java -jar elastiflix-web/target/elastiflix-web-0.1.0-SNAPSHOT.jar
   ```

2. En otra terminal, desde esta carpeta:

   ```bash
   npm install
   npx playwright install chromium        # en Linux puede pedir: sudo npx playwright install-deps chromium
   BASE_URL=http://localhost:8089 npm test
   ```

## Qué cubren

| Archivo | Comportamiento |
|---|---|
| `utf8.test.js` | S-09: un carácter de 2 bytes cortado en el byte 65 535 no rechaza un archivo válido; un byte inválido sí |
| `ingest.test.js` | Revisión 3 (T-04, T-05, T-11 y T-26): un 409 al importar deja el aviso visible tras el siguiente sondeo; validar un CSV, cambiar el separador y volver a validar el mismo archivo hace un segundo POST; `cancelado` deja el paso 4 detenido; un doble clic en Importar hace un solo POST; una caída de red al importar o al subir muestra el aviso de red. T-26: las pestañas usan `tabindex` itinerante y se recorren con ←, →, Inicio y Fin. Revisión 4: al cambiar de idioma con un trabajo CSV activo sigue la pestaña CSV con su separador, y un `?job=` de un JSON elige la pestaña JSON (U-20); un `?job=` que no es un UUID no se usa y el id va codificado en cada ruta de `fetch` (U-21) |
| `mode.test.js` | Requerimiento 4 (fase 10): append viene marcado, sin aviso de bulk, con el paso 4 «Escritura en el catálogo» y sin paso 5; bulk muestra el aviso y los pasos 4 y 5 de bulk; la subida manda `modo`; cambiar el modo después de validar bloquea «Importar» y vuelve a habilitar «Validar archivo»; `?job=` de un trabajo bulk marca bulk; un append `agregado` deja los pasos 1 a 4 completados |
| `size.test.js` | Requerimiento 3 (fase 9): la página entrega el tope de 25 MB (`data-max-bytes="26214400"`); un archivo de 25 MB exactos pasa la comprobación del navegador y uno de 25 MB + 1 byte se rechaza sin subirse, con «El archivo pesa 25.0 MB y el máximo permitido es 25 MB.» y su texto en inglés |
| `suggestions.test.js` | S-10: en Híbrida, con `k=50`, `plot_1024` y `multi_match`, una sugerencia busca con esos ajustes; sin JavaScript el enlace del servidor sigue llevando a `/buscar` |
