# Pruebas de navegador

Prueban el JavaScript de las páginas (`static/js/*.js`) en Chromium, contra la aplicación ya arrancada.
No forman parte de `./mvnw verify`: necesitan Node.js 20 o más y un navegador. Se agregaron en la
revisión 2 (S-09 y S-10), donde el criterio pide una prueba de JS o de navegador.

## Cómo correrlas

1. Arranca la aplicación. No hace falta Elasticsearch: estas pruebas no buscan ni importan, y simulan
   las respuestas JSON que necesitan.

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
