<div align="center">

# 🎬 Elastiflix-Movies Java

**Busca películas por palabras, por significado o combinando ambos, y compara cómo responde cada modo.**

Demo web de búsqueda **full-text (BM25)**, **vectorial (k-NN)** e **híbrida (RRF)** sobre un catálogo de películas en
**Elasticsearch Serverless**, con interfaz en español e inglés.

![Java](https://img.shields.io/badge/Java-25_LTS-FFCDB2?style=for-the-badge&logo=openjdk&logoColor=2E2A3B&labelColor=FBF7F1)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-BDE8D7?style=for-the-badge&logo=springboot&logoColor=1E7A5C&labelColor=FBF7F1)
![Maven](https://img.shields.io/badge/Maven-3.10.0-FBE7A1?style=for-the-badge&logo=apachemaven&logoColor=A9502A&labelColor=FBF7F1)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-Serverless-BFE0F6?style=for-the-badge&logo=elasticsearch&logoColor=1F6FA8&labelColor=FBF7F1)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3.1.5-CDBDF6?style=for-the-badge&logo=thymeleaf&logoColor=6A50C8&labelColor=FBF7F1)

**Español** · [English](README.en.md)

</div>

---

## 🎯 Objetivo

Elastiflix-Movies Java es una demo académica que pone lado a lado las tres formas de buscar en Elasticsearch:

- **Full-text:** encuentra las películas cuyo título contiene tus palabras.
- **Vectorial:** encuentra las películas cuya sinopsis *significa* algo parecido a lo que describes, aunque no comparta
  ninguna palabra.
- **Híbrida:** combina las dos listas en una sola.

Cada resultado muestra de dónde sale su puntuación, así que puedes ver por qué una película aparece primero en un
modo y más abajo en otro. La aplicación también deja **cargar y validar** películas (una por una o desde archivos
`.json` y `.csv`) y genera los vectores de cada sinopsis con el servicio de inferencia de Elasticsearch.

## 📚 Contenido

- [✨ Qué puedes hacer](#-qué-puedes-hacer)
- [🚀 Guía de inicio, de principio a fin](#-guía-de-inicio-de-principio-a-fin)
- [🔎 Los tres modos de búsqueda](#-los-tres-modos-de-búsqueda)
- [🎛️ Resultados, filtros y paginación](#️-resultados-filtros-y-paginación)
- [📥 Carga de datos](#-carga-de-datos)
- [⚙️ Configuración](#️-configuración)
- [🧪 Pruebas](#-pruebas)
- [🩺 Solución de problemas](#-solución-de-problemas)
- [🏗️ Arquitectura](#️-arquitectura)
- [📁 Estructura del repositorio](#-estructura-del-repositorio)
- [🧰 Stack](#-stack)
- [✅ Calidad de código](#-calidad-de-código)
- [📸 Capturas](#-capturas)
- [📖 Documentación](#-documentación)
- [🙏 Créditos](#-créditos)

---

## ✨ Qué puedes hacer

| | Página | Qué ofrece |
|---|---|---|
| 🏠 | **Inicio** `GET /` | Escribir una búsqueda, elegir el modo y sus ajustes, o probar una sugerencia |
| 📋 | **Resultados** `GET /buscar` | Puntuaciones de cada modo, filtros con conteos, paginación de 25 · 50 · 100 |
| 🎬 | **Detalle** `GET /pelicula/{id}` | Ficha técnica completa, cifras en USD y regreso a los resultados con la búsqueda intacta |
| 📥 | **Cargar datos** `GET /cargar` | Registro individual o archivos `.json` / `.csv`, validados contra el mapping; modos append y bulk |
| ℹ️ | **Acerca de** `GET /acerca` | Cómo funciona cada modo, la precisión medida, todos los campos del catálogo y los créditos |

Toda la interfaz está en **español e inglés**: selector «ES · EN» en el encabezado, parámetro `?lang=en` y cookie
`elastiflix-lang`. El pie muestra el índice vigente y cuántas películas tiene.

---

## 🚀 Guía de inicio, de principio a fin

Al terminar estos pasos tendrás la aplicación en http://localhost:8080 con un catálogo propio en tu proyecto de
Elasticsearch, y habrás hecho una búsqueda en cada modo.

### 1. Requisitos

| Necesitas | Para qué |
|---|---|
| ☕ **JDK 25** | Compilar y ejecutar. Maven no hace falta: el repositorio trae el Maven Wrapper (`./mvnw`) |
| 🐙 **Git** | Clonar el repositorio. En **Windows**, usa **Git Bash** para todos los comandos de esta guía |
| ☁️ **Una cuenta de [Elastic Cloud](https://cloud.elastic.co/)** | Crear un proyecto **Elasticsearch Serverless** |
| 🌐 Un navegador | Usar la aplicación |
| 🟢 Node.js 20 o más *(opcional)* | Solo para las pruebas de navegador |

### 2. Crea el proyecto Serverless y una API key

1. En Elastic Cloud, crea un proyecto **Elasticsearch Serverless** (cualquier región).
2. Copia el **endpoint de Elasticsearch** del proyecto. Tiene la forma
   `https://<proyecto>.es.<región>.<proveedor>.elastic.cloud`.
3. Crea una **API key** en la sección de API keys del proyecto. Sin restricciones funciona. Si prefieres limitarla,
   estos son los privilegios que usa la aplicación:

   ```json
   {
     "elastiflix": {
       "cluster": ["monitor", "monitor_inference", "cancel_task"],
       "indices": [
         {
           "names": ["elastiflix-*"],
           "privileges": ["create_index", "delete_index", "read", "write", "view_index_metadata", "manage"]
         }
       ]
     }
   }
   ```

   `manage` sirve para mover el alias del catálogo; `monitor_inference`, para pedir vectores; `monitor` y
   `cancel_task`, solo para el comando de migración del paso opcional. Guía oficial:
   [claves de API de Elasticsearch](https://www.elastic.co/docs/deploy-manage/api-keys/elasticsearch-api-keys).

### 3. Clona el repositorio y configura `.env`

```bash
git clone <url-del-repositorio> elastiflix-reloaded
cd elastiflix-reloaded
cp .env.example .env
```

Abre `.env` y completa las dos variables obligatorias:

```dotenv
ELASTICSEARCH_URL=https://<proyecto>.es.<región>.<proveedor>.elastic.cloud
ELASTICSEARCH_API_KEY=<tu-api-key>
```

El resto ya trae valores por defecto (ver [Configuración](#️-configuración)). `.env` está en `.gitignore`: nunca
se sube al repositorio.

> [!IMPORTANT]
> Ejecuta todos los comandos **desde la raíz del repositorio**: la aplicación lee `.env` de la carpeta donde se
> ejecuta.

### 4. Comprueba la conexión y el modelo de embeddings

La búsqueda vectorial usa el endpoint de inferencia `.jina-embeddings-v5-omni-small` del servicio de inferencia de
Elastic. Comprueba que tu proyecto responde y que el endpoint existe:

```bash
set -a; . ./.env; set +a      # exporta las variables de .env a esta terminal (solo para los curl)

curl -sS -H "Authorization: ApiKey $ELASTICSEARCH_API_KEY" "$ELASTICSEARCH_URL"

curl -sS -H "Authorization: ApiKey $ELASTICSEARCH_API_KEY" \
  "$ELASTICSEARCH_URL/_inference/embedding/.jina-embeddings-v5-omni-small"
```

La primera llamada devuelve la información del proyecto. La segunda describe un endpoint de tipo `embedding`; si
responde `resource_not_found_exception`, tu proyecto no tiene ese modelo y la búsqueda vectorial y las cargas no
funcionarán. Referencia: [Get inference](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-inference-get).

### 5. Compila y verifica

```bash
./mvnw verify
```

Compila, corre las pruebas unitarias y las de arquitectura, y revisa el estilo con Checkstyle y PMD. No necesita
red ni Elasticsearch. Debe terminar con `BUILD SUCCESS`.

### 6. Arranca la aplicación

```bash
./mvnw spring-boot:run
```

Abre http://localhost:8080. Con un proyecto nuevo, el encabezado dice **«Conectado · el catálogo todavía no
existe»**: la conexión funciona y falta cargar películas.

Otras formas de arrancarla:

```bash
./mvnw -q package -DskipTests
java -jar target/elastiflix-reloaded-0.1.0-SNAPSHOT.jar      # el mismo resultado, desde el jar
```

- **IntelliJ IDEA:** abre la carpeta raíz como proyecto Maven y ejecuta
  `com.bigfito.elastiflix.ElastiflixApplication` con la raíz del repositorio como directorio de trabajo.
- **Otro puerto:** `SERVER_PORT=8090 ./mvnw spring-boot:run`.
- **Detenerla:** `Ctrl+C` en la terminal.

### 7. Crea el catálogo con la primera carga

El catálogo es el alias `elastiflix-reloaded` (configurable), que apunta a un índice
`elastiflix-reloaded-<AAAAMMDD-HHMMSS>`. **La primera carga lo crea**, con el mapping del repositorio.

1. Ve a **Cargar datos** (http://localhost:8080/cargar) y abre la pestaña **Importar .json**.
2. La tarjeta «Modo de carga» ya trae **bulk** marcado; append está deshabilitado mientras no haya catálogo.
3. Elige `elasticsearch/fixtures/movies-sample.json`: cuatro películas reales (Titanic, Inception, WALL·E y The
   Equalizer 2).
4. Pulsa **Validar archivo**. Cuando la validación termine, pulsa **Reemplazar el catálogo con 4 registros**.
5. Cuando el paso 5, «Publicación», quede completado, recarga la página: el pie muestra el índice nuevo y
   **4 películas**.

Para tener el **catálogo completo de 6 899 películas**, usa los archivos de [`data/`](data/README.md): carga
`elastiflix-reloaded_01.json` en **bulk** y después `_02` a `_07` en **append** (o los mismos en `.csv`). Cada parte
pesa menos de 5 MB y también sirve sola, en bulk, como un catálogo más chico de unas 985 películas.

También puedes crear el catálogo guardando una película en la pestaña **Registro individual** (`id`, `title` y
`plot` son obligatorios). Para cargar tu propio catálogo, prepara un `.json` o un `.csv` como se explica en
[Carga de datos](#-carga-de-datos); «Acerca de» › «El catálogo» lista cada campo y si es obligatorio.

### 8. Busca

En **Inicio**, prueba:

| Modo | Escribe | Qué esperar |
|---|---|---|
| Full-text | `titanic` | Coincidencia por palabra en el título |
| Vectorial | `un barco que se hunde en el océano` | Titanic primero, aunque ninguna palabra coincida con el título |
| Híbrida | `sueños dentro de sueños` | Inception primero; cada tarjeta muestra BM25, k-NN, RRF y su posición en cada lista |

Pulsa el título de una película para ver su detalle, y cambia a **EN** en el encabezado para ver la interfaz en
inglés. Con solo cuatro películas, cualquier búsqueda vectorial devuelve las cuatro en algún orden: el interés está en
el orden y en las puntuaciones.

### 9. Opcional: convierte un índice concreto en alias

Si tu proyecto ya tiene un **índice concreto** llamado `elastiflix-reloaded` (no un alias), la aplicación no lo
usa para cargar. Este comando lo copia a `elastiflix-reloaded-<AAAAMMDD-HHMMSS>` y, en una sola llamada, borra el
índice concreto y crea el alias. Termina solo:

```bash
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--elastiflix.bootstrap.enabled=true --spring.main.web-application-type=none"
```

Si el nombre ya es un alias, no crea ni borra nada: el log dice a qué índice apunta y el proceso termina con código 0.
Si el nombre no existe, se detiene sin crear nada: en ese caso, el catálogo lo crea la primera carga (paso 7).

---

## 🔎 Los tres modos de búsqueda

### 🔤 Full-text (léxica) · BM25

Busca coincidencias de términos en `title` y `original_title` (ambos `text`).

| Ajuste | Opciones |
|---|---|
| Tipo de consulta | `match` (un campo: `title` **o** `original_title`) · `multi-match` (`best_fields` sobre los dos) |
| Operador | `OR` (basta una palabra; por defecto) · `AND` (todas; con `multi-match`, en un mismo campo) |

> [!TIP]
> **BM25** es el algoritmo de puntuación por defecto de Elasticsearch: favorece los términos poco comunes
> en el catálogo; `k1 = 1.2` limita cuánto suma repetir un término y `b = 0.75` ajusta por la longitud
> del campo.

### 🧭 Vectorial · k-NN

Tu consulta se convierte en un vector con **jina-embeddings-v5-omni-small** y Elasticsearch devuelve
las películas cuya sinopsis (`plot`) tiene el vector más cercano, con un grafo **HNSW**.

| Campo | Dimensiones | Origen |
|---|---|---|
| `plot_256` | 256 | Primeros valores del vector de 1024, renormalizados (Matryoshka) |
| `plot_512` | 512 | Primeros valores del vector de 1024, renormalizados (Matryoshka) |
| `plot_1024` | 1024 | Vector completo del modelo |

| Ajuste | Por defecto | Regla |
|---|---|---|
| `k` | 100 | Resultados que devuelve la búsqueda vectorial |
| `num_candidates` | 200 | `k ≤ num_candidates ≤ 10 000` |

Los tres campos usan la misma configuración (`hnsw`, `m = 16`, `ef_construction = 100`, `cosine`, float
sin cuantización): la única diferencia es el número de dimensiones. **Precisión medida** con 32
descripciones de trama sobre un catálogo de 6 899 películas:

| Campo | 🥇 Correcta en 1.er lugar | Top 5 | Top 10 | MRR@10 |
|---|---|---|---|---|
| `plot_256` | 22 de 32 (69 %) | 29 | 30 | 0.774 |
| `plot_512` | 25 de 32 (78 %) | 31 | 32 | 0.853 |
| `plot_1024` | 26 de 32 (81 %) | 31 | 31 | 0.880 |

> 512 dimensiones queda muy cerca de 1024 con la mitad del tamaño; 256 pierde precisión de forma clara.

Las consultas se vectorizan con `input_type: search` y el prefijo `Query: `; las sinopsis, con `input_type: ingest`
y sin prefijo.

### 🔀 Híbrida · RRF

El retriever `rrf` combina el componente full-text (`standard`) y el vectorial (`knn`) según la
**posición** de cada película en cada lista, no según su puntuación, porque las escalas de BM25 y de
la similitud no son comparables.

| Parámetro | Valor |
|---|---|
| `rank_constant` | 60 (por defecto de Elasticsearch) |
| `rank_window_size` | igual a `k` (lo fija la aplicación) |

Cada resultado muestra su puntuación BM25, su similitud k-NN, su posición en cada lista y la puntuación
RRF: `Σ 1 / (60 + posición)`.

## 🎛️ Resultados, filtros y paginación

| | Full-text | Vectorial | Híbrida |
|---|---|---|---|
| **Puntuación** | BM25 (sin escala fija) | `(1 + coseno) / 2`, entre 0 y 1 | BM25 + k-NN + RRF |
| **Máximo de resultados** | 10 000 (`index.max_result_window`) | `k` | `rank_window_size` (= `k`) |
| **Filtros en Elasticsearch** | contexto `filter` | `filter` del retriever `knn` | `filter` del retriever `rrf` |
| **Conteos de filtros** | agregaciones `terms` | calculados en la aplicación | calculados en la aplicación |

- **Filtros:** `adult`, `genres`, `release_date` (desde–hasta), `spoken_languages`, `rating`. Entre
  filtros distintos se cumplen todos; dentro de uno basta con un valor. Se aplican con **APLICAR**.
- **Valores dinámicos:** cada filtro muestra solo los valores presentes en los resultados, con su conteo.
- **Sin resultados:** la página sugiere ajustes según el modo, o quitar filtros si estos vaciaron la
  lista.
- **Pósters:** se cargan del CDN de imágenes de TMDB a partir de `poster_path`.
- **Detalle:** se abre desde un resultado y conserva la búsqueda en la URL para volver a ella. Un enlace directo
  sin búsqueda responde 400 (`EFX-SRCH-012`) y un `id` que no existe, 404.

## 📥 Carga de datos

```mermaid
flowchart LR
  A["1 · Comprobación<br/>en el navegador"] --> B["2 · Subida<br/>al servidor"]
  B --> C["3 · Validación<br/>contra el mapping"]
  C -->|"un solo error"| X["⛔ Se detiene<br/>no se importa nada"]
  C -->|"append (por defecto)"| W["4 · Escritura en el catálogo<br/>por el alias · lotes de 100"]
  W -->|"falla o se cancela"| V["⏸️ Lo escrito se queda<br/>la página dice cuántas"]
  C -->|"bulk"| D["4 · Importación<br/>índice temporal solo con el archivo"]
  D --> E["5 · Publicación<br/>swap atómico del alias<br/>y borrado del índice anterior"]
  D -->|"falla"| Y["↩️ Se borra el temporal<br/>catálogo intacto"]
  E -->|"no se confirma y el alias<br/>no se puede leer"| Z["⚠️ EFX-IMP-006<br/>el temporal se deja"]
```

Un archivo se carga en uno de dos modos, que se elige en la tarjeta «Modo de carga»:

- **Append** (por defecto, no borra nada): las películas se agregan al índice vigente por el alias, en lotes de 100 y
  sin índice temporal. Un `id` que ya existe se reemplaza; antes de empezar, la página dice cuántas se reemplazarán.
  Si un lote se rechaza (`EFX-IMP-008`) o la carga se cancela (`EFX-IMP-009`), lo ya escrito se queda y la página
  dice cuántas se agregaron.
- **Bulk** (reemplaza el catálogo): las películas se escriben en un índice temporal nuevo que tiene **solo** las del
  archivo; al publicar, el alias pasa a ese índice en una sola operación y el índice anterior se borra. La página
  avisa cuántas películas tiene el catálogo que se borra. Si algo falla antes de publicar, el catálogo queda como
  estaba.
- **Sin catálogo:** bulk y el registro individual crean el índice con el mapping y el alias; append queda bloqueado
  (`EFX-IMP-010`) hasta que el catálogo exista.

Un catálogo de más de 25 MB se carga con un bulk (el primer archivo) y varios append. Así se carga el catálogo
completo que trae [`data/`](data/README.md), repartido en 7 partes.

| Forma | Formato |
|---|---|
| ✍️ Registro individual | Formulario con los campos del mapping (`id`, `title` y `plot` obligatorios); se escribe **directamente en el catálogo** |
| 🧾 `.json` | Exportación del índice (objeto con `documents` y, opcionalmente, `mapping` y `document_count`), arreglo de películas o NDJSON |
| 📊 `.csv` | UTF-8 con o sin BOM, separador coma, punto y coma o tabulador, **encabezado obligatorio**; listas y anidados como JSON en la celda |

- **Campos obligatorios:** en los dos formatos y en los dos modos, `id`, `title` y `plot` con valor (en `.csv`, como
  columnas del encabezado); el resto es opcional. «Acerca de» › «El catálogo» lo dice campo por campo, y el mapping
  completo está en [`elasticsearch/elastiflix-reloaded-mapping.json`](elasticsearch/elastiflix-reloaded-mapping.json).
- **Antes de subir:** solo `.json` o `.csv`, sin dobles extensiones, máximo **25 MB** (tope fijo), UTF-8.
- **Contra el mapping:** se revisa el archivo completo, todo o nada. Si algo no coincide, se muestra cada error
  (registro, `id`, campo, valor recibido, lo que espera el mapping) y se puede descargar el informe en CSV.
- **Vectores:** `plot_256`, `plot_512` y `plot_1024` **siempre** se generan desde `plot` con el servicio de
  inferencia. Si el archivo los trae, se ignoran con una advertencia; el formulario no los pide.
- **Un solo usuario:** la demo está pensada para una persona a la vez; no tiene controles de concurrencia.

## ⚙️ Configuración

Todo se configura en `.env` (o con variables de entorno, que tienen prioridad). `.env.example` trae la lista
completa:

| Variable | Por defecto | Qué controla |
|---|---|---|
| `ELASTICSEARCH_URL` | — (obligatoria) | Endpoint de Elasticsearch del proyecto Serverless. Sin ella, la aplicación no arranca (`EFX-CFG-001`) |
| `ELASTICSEARCH_API_KEY` | — (obligatoria) | API key del proyecto. Sin ella, la aplicación no arranca (`EFX-CFG-002`) |
| `ELASTIFLIX_INDEX_ALIAS` | `elastiflix-reloaded` | Alias del catálogo. Los índices se llaman `<alias>-<AAAAMMDD-HHMMSS>` |
| `ELASTIFLIX_INFERENCE_ID` | `.jina-embeddings-v5-omni-small` | Endpoint de inferencia que genera los vectores |
| `ELASTIFLIX_INGEST_BATCH_SIZE` | `100` | Películas por lote al cargar un archivo |
| `ELASTIFLIX_POSTER_BASE_URL` | `https://image.tmdb.org/t/p/w342` | Prefijo de las imágenes de los pósters |
| `SERVER_PORT` | `8080` | Puerto de la aplicación |

El estado del catálogo (alias, índice vigente y conteo) también está en `GET /actuator/health`.

## 🧪 Pruebas

| Comando | Qué corre | Necesita |
|---|---|---|
| `./mvnw verify` | Pruebas unitarias, de arquitectura (ArchUnit), Checkstyle y PMD | Nada más |
| `./mvnw verify -Pit` | Lo anterior más las pruebas de integración y el smoke test | `.env` y el proyecto Serverless |
| `npm test` en `src/test/browser` | Pruebas del JavaScript de las páginas con Playwright | La aplicación en marcha y Node.js; ver [su README](src/test/browser/README.md) |

- Las pruebas de integración que **escriben** usan alias propios `elastiflix-it-<aleatorio>` y los borran al
  terminar; nunca tocan el catálogo.
- Las que **leen** el catálogo están pensadas para el catálogo de referencia de 6 899 películas con el que se midió la
  precisión. Con otro catálogo, las que comparan conteos y posiciones se omiten y algunas pueden fallar, porque
  buscan películas concretas (por ejemplo, la `597`, Titanic).

## 🩺 Solución de problemas

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| No arranca con `EFX-CFG-001` o `EFX-CFG-002` | Falta `ELASTICSEARCH_URL` o `ELASTICSEARCH_API_KEY` | Revisa `.env` y arranca desde la raíz del repositorio |
| Encabezado «Sin conexión · EFX-ES-001» | URL o API key incorrectas, o el proyecto no responde | Repite los `curl` del paso 4 |
| Encabezado «Conectado · el catálogo todavía no existe» | El alias no existe | Haz la primera carga (paso 7) |
| Append rechazado con `EFX-IMP-010` | Todavía no hay catálogo | Carga primero en modo bulk o guarda un registro individual |
| La búsqueda vectorial o la carga fallan al pedir vectores | El endpoint de inferencia no existe en el proyecto | Comprueba el endpoint (paso 4) o ajusta `ELASTIFLIX_INFERENCE_ID` |
| Carga rechazada con `EFX-IMP-004` | El nombre del catálogo es un índice concreto, no un alias | Conviértelo en alias (paso 9) |
| `Port 8080 was already in use` | Otro proceso usa el puerto | Usa `SERVER_PORT=8090` |
| IntelliJ: `ClassNotFoundException` al arrancar | Compilación incremental incompleta tras un `./mvnw clean` | **Build › Rebuild Project** |
| Comandos que fallan en Windows | `cmd` o PowerShell no entienden `./mvnw`, `set -a` ni `cp` | Usa **Git Bash** |

Cada error de la aplicación tiene un código `EFX-<ÁREA>-<NNN>` y explica qué pasó, la causa probable y qué hacer.

## 🏗️ Arquitectura

```mermaid
flowchart LR
  user(["👤 Usuario"]) --> web["🌐 web<br/>Thymeleaf + Spring MVC"]
  web --> search["🔎 search<br/>BM25 · k-NN · RRF · facetas"]
  web --> ingest["📥 ingest<br/>validación · importación"]
  search --> core["⚙️ core<br/>cliente ES · catálogo · embeddings"]
  ingest --> core
  core --> es[("🗄️ Elasticsearch Serverless<br/>alias elastiflix-reloaded")]
  core --> inf["🧠 Inferencia<br/>.jina-embeddings-v5-omni-small"]
```

Es una sola aplicación Spring Boot en un solo módulo de Maven. `core`, `search`, `ingest` y `web` son paquetes de
`com.bigfito.elastiflix`; `ArchitectureTest` (ArchUnit) comprueba que cada uno use solo lo que le toca, como indican
las flechas. El diagrama completo, con los flujos numerados de búsqueda, detalle y carga, está en
[`docs/arquitectura.mermaid`](docs/arquitectura.mermaid).

- **Lectura siempre por el alias.** Una carga bulk publica el índice nuevo con una sola llamada atómica a
  `_aliases` (`add` al nuevo y `remove_index` del anterior), así que nunca hay un momento sin catálogo.
- **Sin `_cluster/health`** en Serverless: el indicador de salud consulta el alias y su conteo.
- **Vectores fuera de las respuestas:** las búsquedas excluyen `plot_256`, `plot_512` y `plot_1024` de `_source`.
- **Facetas:** en Full-text salen de agregaciones `terms`; en Vectorial e Híbrida se calculan en la aplicación sobre
  los resultados devueltos.

## 📁 Estructura del repositorio

```
elastiflix-reloaded/
├── 📄 README.md · README.en.md
├── 📦 pom.xml · mvnw · mvnw.cmd · .mvn/     build de Maven (un solo módulo)
├── 🔑 .env.example                          variables de entorno (cópialo como .env)
├── ☕ src/main/java/com/bigfito/elastiflix/
│   ├── ElastiflixApplication.java          punto de entrada
│   ├── ⚙️  core/                            modelo · cliente ES · alias e índices · embeddings · errores
│   ├── 🔎 search/                          match / multi_match · knn · rrf · filtros · facetas
│   ├── 📥 ingest/                          validación · lectores JSON/CSV · importación
│   └── 🌐 web/                             controladores y 5 páginas Thymeleaf en español e inglés
├── 🖼️  src/main/resources/                  application.yml · plantillas · estáticos · mensajes ES/EN
├── 🧪 src/test/                            java/ (mismos paquetes) · resources/ · browser/ (Playwright)
├── 🎞️  data/                               catálogo completo de 6 899 películas en 7 partes (.json y .csv)
├── 🗄️  elasticsearch/                       mapping del índice y películas de ejemplo (.json y .csv)
├── 🧹 config/                              reglas de Checkstyle y PMD
├── 📚 docs/                                convenciones de código · arquitectura · capturas
└── 🎨 design/                              mockups y tokens de diseño
```

## 🧰 Stack

| | Componente | Versión |
|---|---|---|
| ☕ | Java (LTS) | 25 |
| 🍃 | Spring Boot | 4.1.1 |
| 📦 | Maven (Maven Wrapper) | 3.10.0 |
| 🖼️ | Thymeleaf | 3.1.5 |
| 🔍 | Cliente Java de Elasticsearch | 9.4.5 (gestionado por Spring Boot) |
| 🧩 | Jackson 3 (`Jackson3JsonpMapper`) | 3.1.5 |
| 🧠 | jina-embeddings-v5-omni-small | endpoint `.jina-embeddings-v5-omni-small` |
| 🧹 | Checkstyle · PMD · ArchUnit (`archunit-junit6`) | 14.3.0 · 7.28.0 · 1.5.1 |

Sin Spring Data Elasticsearch ni Lombok: la aplicación usa directamente el cliente Java de Elasticsearch para
controlar los retrievers (`knn`, `rrf`), los alias y la inferencia.

## ✅ Calidad de código

`./mvnw verify` falla ante la primera violación:

| Herramienta | Revisa |
|---|---|
| Checkstyle 14.3.0 | Tamaño y complejidad de métodos, nombres, Javadoc, `catch` vacíos, `System.out`, inyección por campo, secretos literales |
| PMD 7.28.0 | Complejidad cognitiva, clases dios, excepciones genéricas, código sin uso |
| ArchUnit 1.5.1 | Dependencias entre los paquetes `core`, `search`, `ingest` y `web`; Spring MVC y Servlet solo en `web`; la web no usa el cliente de Elasticsearch |

Las reglas de estilo y diseño (Java 25 con mesura, SOLID, métodos cortos, errores con códigos `EFX-*`, logging con
SLF4J, Javadoc) están en [`docs/convenciones-java.md`](docs/convenciones-java.md).

**Seguridad.** La demo no tiene autenticación ni protección CSRF: es intencional, porque no hay sesión que proteger y
la usa una persona a la vez. Los secretos solo van en `.env`; nunca en el código, el YAML, las pruebas ni los logs.

## 📸 Capturas

Páginas completas con la aplicación conectada a un catálogo de 6 899 películas, en español e inglés (ventana de
1440 px).

| Página | Español | English |
|---|---|---|
| Inicio `GET /` | ![Inicio](docs/capturas/inicio.png) | ![Inicio (EN)](docs/capturas/inicio-en.png) |
| Resultados · Full-text | ![Resultados · Full-text](docs/capturas/resultados-fulltext.png) | ![Resultados · Full-text (EN)](docs/capturas/resultados-fulltext-en.png) |
| Resultados · Vectorial | ![Resultados · Vectorial](docs/capturas/resultados-vectorial.png) | ![Resultados · Vectorial (EN)](docs/capturas/resultados-vectorial-en.png) |
| Resultados · Híbrida | ![Resultados · Híbrida](docs/capturas/resultados-hibrida.png) | ![Resultados · Híbrida (EN)](docs/capturas/resultados-hibrida-en.png) |
| Detalle de película `GET /pelicula/{id}` | ![Detalle de película](docs/capturas/detalle.png) | ![Detalle de película (EN)](docs/capturas/detalle-en.png) |
| Detalle sin búsqueda (`EFX-SRCH-012`) | ![Detalle sin búsqueda](docs/capturas/detalle-sin-busqueda.png) | ![Detalle sin búsqueda (EN)](docs/capturas/detalle-sin-busqueda-en.png) |
| Cargar datos · Registro individual | ![Cargar datos · Registro individual](docs/capturas/cargar-registro.png) | ![Cargar datos · Registro individual (EN)](docs/capturas/cargar-registro-en.png) |
| Cargar datos · CSV válido, listo para importar | ![Cargar datos · CSV válido](docs/capturas/cargar-csv-valido.png) | ![Cargar datos · CSV válido (EN)](docs/capturas/cargar-csv-valido-en.png) |
| Cargar datos · CSV que no coincide con el mapping | ![Cargar datos · CSV inválido](docs/capturas/cargar-csv-invalido.png) | ![Cargar datos · CSV inválido (EN)](docs/capturas/cargar-csv-invalido-en.png) |
| Acerca de `GET /acerca` | ![Acerca de](docs/capturas/acerca.png) | ![Acerca de (EN)](docs/capturas/acerca-en.png) |
| Ruta desconocida | ![Ruta desconocida](docs/capturas/no-encontrada.png) | ![Ruta desconocida (EN)](docs/capturas/no-encontrada-en.png) |

## 📖 Documentación

| Documento | Para qué |
|---|---|
| 🏗️ [`docs/arquitectura.mermaid`](docs/arquitectura.mermaid) | Arquitectura y flujos numerados de búsqueda, detalle y carga |
| 📐 [`docs/convenciones-java.md`](docs/convenciones-java.md) | Cómo está escrito el código y qué verifica el build |
| 🗄️ [`elasticsearch/elastiflix-reloaded-mapping.json`](elasticsearch/elastiflix-reloaded-mapping.json) | Mapping del índice (33 campos) |
| 🎞️ [`data/README.md`](data/README.md) | Catálogo completo en 7 partes: cómo cargarlo y su formato |
| 🎬 [`elasticsearch/fixtures/`](elasticsearch/fixtures/) | Cuatro películas de ejemplo en `.json` y `.csv`, y un vector de consulta |
| 🎨 [`design/README.md`](design/README.md) | Mockups de las páginas y tokens de diseño |
| 🧪 [`src/test/browser/README.md`](src/test/browser/README.md) | Pruebas de navegador |
| 📸 [`docs/capturas/`](docs/capturas/) | Capturas de las páginas |

**Referencias de Elasticsearch:**
[match](https://www.elastic.co/docs/reference/query-languages/query-dsl/query-dsl-match-query) ·
[multi_match](https://www.elastic.co/docs/reference/query-languages/query-dsl/query-dsl-multi-match-query) ·
[kNN retriever](https://www.elastic.co/docs/reference/elasticsearch/rest-apis/retrievers/knn-retriever) ·
[RRF retriever](https://www.elastic.co/docs/reference/elasticsearch/rest-apis/retrievers/rrf-retriever) ·
[dense_vector](https://www.elastic.co/docs/reference/elasticsearch/mapping-reference/dense-vector) ·
[alias](https://www.elastic.co/docs/manage-data/data-store/aliases)

## 🙏 Créditos

Los datos y los pósters de las películas vienen de [TMDB](https://www.themoviedb.org/).
*This product uses the TMDB API but is not endorsed or certified by TMDB.* La atribución aparece en el pie de cada
página y en «Acerca de» › «Créditos».

---

<div align="center">

Hecho con ☕ y 🍿

</div>
