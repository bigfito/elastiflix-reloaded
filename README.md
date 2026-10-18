<div align="center">

# 🎬 Elastiflix-Movies Java

**Busca películas por palabras, por significado o combinando ambos — y compara cómo responde cada modo.**

Demo de búsqueda **full-text (BM25)**, **vectorial (k-NN)** e **híbrida (RRF)** sobre el alias
`elastiflix-reloaded` de **Elasticsearch Serverless**. El índice y el conteo vigentes están en
[`progreso/INDICE.md`](progreso/INDICE.md) › «Catálogo vigente».

![Java](https://img.shields.io/badge/Java-25_LTS-FFCDB2?style=for-the-badge&logo=openjdk&logoColor=2E2A3B&labelColor=FBF7F1)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-BDE8D7?style=for-the-badge&logo=springboot&logoColor=1E7A5C&labelColor=FBF7F1)
![Maven](https://img.shields.io/badge/Maven-3.10.0-FBE7A1?style=for-the-badge&logo=apachemaven&logoColor=A9502A&labelColor=FBF7F1)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-Serverless-BFE0F6?style=for-the-badge&logo=elasticsearch&logoColor=1F6FA8&labelColor=FBF7F1)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3.1.5-CDBDF6?style=for-the-badge&logo=thymeleaf&logoColor=6A50C8&labelColor=FBF7F1)

![Estado](https://img.shields.io/badge/estado-revisi%C3%B3n_2_terminada-BDE8D7?style=flat-square&labelColor=3A3548)
![Fases](https://img.shields.io/badge/fases-7%2F7-FFCDB2?style=flat-square&labelColor=3A3548)
![Agentes](https://img.shields.io/badge/agentes-Claude_Code_%C2%B7_Cursor_%C2%B7_Antigravity_%C2%B7_Junie-BDE8D7?style=flat-square&labelColor=3A3548)

</div>

> [!NOTE]
> ✅ **Estado actual:** las siete fases del plan están terminadas en `main`. La
> **Revisión 2** (27 correcciones de [`docs/revisiones/revision-2.md`](docs/revisiones/revision-2.md)) está
> terminada en la rama `revision-2`, con estado en [`progreso/revision-2.md`](progreso/revision-2.md), y se
> integra en `main` por PR. El índice y el conteo
> del alias `elastiflix-reloaded` están en [`progreso/INDICE.md`](progreso/INDICE.md) › «Catálogo vigente».
> `./mvnw verify` corre unitarias, arquitectura, Checkstyle y PMD.
> `./mvnw verify -Pit` añade integración y el smoke. Esas pruebas leen el conteo real del alias
> con `_count`, afirman los 6 899 documentos y los valores de §6; se omiten con `assumeTrue` si el conteo cambió. La aplicación no modifica el alias al arrancar.
> El avance está en [`progreso/INDICE.md`](progreso/INDICE.md).

---

## 📚 Contenido

- [✨ Qué hace](#-qué-hace)
- [🔎 Los tres modos de búsqueda](#-los-tres-modos-de-búsqueda)
- [🎛️ Resultados, filtros y paginación](#️-resultados-filtros-y-paginación)
- [📥 Carga de datos](#-carga-de-datos)
- [🏗️ Arquitectura](#️-arquitectura)
- [🧰 Stack](#-stack)
- [📁 Estructura del repositorio](#-estructura-del-repositorio)
- [🚀 Puesta en marcha](#-puesta-en-marcha)
- [🔐 Seguridad y secretos](#-seguridad-y-secretos)
- [🤖 Desarrollo con agentes de IA](#-desarrollo-con-agentes-de-ia)
- [🗺️ Plan y estado](#️-plan-y-estado)
- [✅ Calidad de código](#-calidad-de-código)
- [🎨 Diseño](#-diseño)
- [📸 Capturas](#-capturas)
- [📖 Documentación](#-documentación)

---

## ✨ Qué hace

| | Capacidad | Detalle |
|---|---|---|
| 🔤 | **Full-text (léxica)** | Encuentra películas cuyo título contiene las palabras de tu consulta |
| 🧭 | **Vectorial** | Encuentra películas cuya sinopsis se parece en significado a lo que describes |
| 🔀 | **Híbrida** | Ejecuta las dos búsquedas y combina sus resultados en una sola lista |
| 🎛️ | **Filtros dinámicos** | Adultos, géneros, fecha de estreno, idiomas y clasificación, con conteos |
| 📄 | **Paginación** | 25 · 50 · 100 resultados por página |
| 📥 | **Carga de datos** | Registro individual o archivos `.json` / `.csv`, validados contra el mapping |
| ℹ️ | **Acerca de la demo** | Explicación de cada modo dentro de la aplicación |

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
descripciones de trama:

| Campo | 🥇 Correcta en 1.er lugar | Top 5 | Top 10 | MRR@10 |
|---|---|---|---|---|
| `plot_256` | 22 de 32 (69 %) | 29 | 30 | 0.774 |
| `plot_512` | 25 de 32 (78 %) | 31 | 32 | 0.853 |
| `plot_1024` | 26 de 32 (81 %) | 31 | 31 | 0.880 |

> 512 dimensiones queda muy cerca de 1024 con la mitad del tamaño; 256 pierde precisión de forma clara.

### 🔀 Híbrida · RRF

El retriever `rrf` combina el componente full-text (`standard`) y el vectorial (`knn`) según la
**posición** de cada película en cada lista — no según su puntuación, porque las escalas de BM25 y de
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
- **Pósters:** se cargan del CDN de imágenes de TMDB a partir de `poster_path`. La atribución de TMDB
  aparece en el pie de cada página y en la sección «Créditos» de «Acerca de».

## 📥 Carga de datos

```mermaid
flowchart LR
  A["1 · Comprobación<br/>en el navegador"] --> B["2 · Subida<br/>al servidor"]
  B --> C["3 · Validación<br/>contra el mapping"]
  C -->|"todo coincide"| D["4 · Importación<br/>índice temporal · lotes de 100"]
  C -->|"un solo error"| X["⛔ Se detiene<br/>no se importa nada"]
  D --> E["5 · Publicación<br/>swap atómico del alias<br/>y borrado del índice anterior"]
  D -->|"falla"| Y["↩️ Se borra el temporal<br/>catálogo intacto"]
```

| Forma | Formato |
|---|---|
| ✍️ Registro individual | Formulario con los campos del mapping (`id`, `title` y `plot` obligatorios); se escribe **directamente en el catálogo** |
| 🧾 `.json` | Exportación del índice (`documents`, `mapping`, `document_count`), arreglo de películas o NDJSON |
| 📊 `.csv` | UTF-8 con o sin BOM, coma, **encabezado obligatorio** (sin encabezado se rechaza); listas y anidados como JSON en la celda |

- **Antes de subir:** solo `.json` o `.csv`, sin dobles extensiones, máximo **150 MB**, UTF-8.
- **Contra el mapping:** se revisa el archivo completo; si algo no coincide, se muestra cada error
  (registro, `id`, campo, valor recibido, lo que espera el mapping) y se puede descargar el informe.
- **Vectores:** `plot_256`, `plot_512` y `plot_1024` **siempre** se generan en tiempo de ejecución desde
  `plot`, con el modelo de embeddings a través del servicio de inferencia de Elasticsearch, con la misma
  configuración que produjo los vectores del catálogo (calibrada en la tarea 1.9). Los archivos no los
  traen y el formulario no los captura.
- **Publicación:** en una sola operación, el alias `elastiflix-reloaded` pasa al índice nuevo y el
  índice anterior se borra; si algo falla antes, el catálogo queda exactamente como estaba.
- **Un solo usuario:** la demo tiene fines académicos y demostrativos y la usa una persona a la vez.

## 🏗️ Arquitectura

```mermaid
flowchart LR
  user(["👤 Usuario"]) --> web["🌐 elastiflix-web<br/>Thymeleaf + Spring MVC"]
  web --> search["🔎 elastiflix-search<br/>BM25 · k-NN · RRF · facetas"]
  web --> ingest["📥 elastiflix-ingest<br/>validación · importación"]
  search --> core["⚙️ elastiflix-core<br/>cliente ES · catálogo · embeddings"]
  ingest --> core
  core --> es[("🗄️ Elasticsearch Serverless<br/>alias elastiflix-reloaded")]
  core --> inf["🧠 Inferencia<br/>.jina-embeddings-v5-omni-small"]
```

Diagrama completo con los flujos numerados: [`docs/arquitectura.mermaid`](docs/arquitectura.mermaid).

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

## 📁 Estructura del repositorio

```
elastiflix-reloaded/
├── 📄 PLAN.md · AGENTS.md · CLAUDE.md · README.md
├── 📦 pom.xml · mvnw · .mvn/            (Fase 0)
├── ⚙️  elastiflix-core/                  modelo · cliente ES · alias e índices · embeddings
├── 🔎 elastiflix-search/                match / multi_match · knn · rrf · filtros · facetas
├── 📥 elastiflix-ingest/                validación · lectores JSON/CSV · importación · swap de alias
├── 🌐 elastiflix-web/                   único ejecutable · 4 páginas Thymeleaf
├── 🧹 config/                           Checkstyle y PMD (congelados)
├── 📚 docs/                             convenciones · arquitectura · diseños por fase · capturas · revisiones
├── 🎨 design/                           mockups aprobados y tokens de diseño
├── 🗄️  elasticsearch/                    mapping del índice y fixtures reales
├── 📍 progreso/                         estado del desarrollo para retomar sesiones
└── 🖱️  .cursor/rules/                    regla de Cursor para archivos Java
```

## 🚀 Puesta en marcha

> [!IMPORTANT]
> Los comandos de **Comandos** se ejecutaron en este orden en un `git clone` limpio con una copia local
> de `.env` el 2026-10-18, sobre un proyecto que ya tenía el catálogo. La preparación de un proyecto sin
> el índice («Preparar el catálogo») sigue la documentación de Elasticsearch y el código de la migración,
> pero no se repitió en un proyecto vacío.

**Prerrequisitos**

- ☕ JDK 25 · 🐙 Git (en Windows, con **Git Bash**). El Maven Wrapper ya está en el repositorio
  (`./mvnw`); no hace falta instalar Maven para compilar.
- 🔑 Un proyecto **Elasticsearch Serverless** y una API key con los privilegios de
  «Datos y proyecto Serverless».
- 🎬 Para llenar un catálogo nuevo: un archivo `.json` o `.csv` de películas. El archivo de exportación
  del catálogo no está versionado; el respaldo lo guarda quien exportó el índice (PLAN §2, nota 5).

**Configuración**

```bash
cp .env.example .env          # completa ELASTICSEARCH_URL y ELASTICSEARCH_API_KEY
set -a; . ./.env; set +a      # exporta las variables a esta terminal para los curl de abajo
```

La aplicación lee `.env` sola al arrancar; `set -a; . ./.env; set +a` solo hace falta para los `curl`.

> [!WARNING]
> `.env` nunca se sube a git. Ningún secreto va en el código, en YAML ni en los commits.

**Datos y proyecto Serverless**

- El alias, el índice concreto y el conteo vigentes están en
  [`progreso/INDICE.md`](progreso/INDICE.md) › «Catálogo vigente».
- La API key necesita:
  - en los índices `elastiflix-*`: `create_index`, `delete_index`, `read`, `write`,
    `view_index_metadata` y `manage` (este último, para `_aliases`);
  - en el clúster: `monitor` para leer el avance de `_reindex` en `_tasks/<id>`, `cancel_task` para
    cancelar una copia en curso y `monitor_inference` para pedir vectores a `_inference`.

  Serverless reconoce esos nombres (comprobado con `_security/user/_has_privileges`). Las pruebas se
  corrieron con una clave más amplia, así que este mínimo no se probó por separado. Cómo se declaran:
  [claves de API de Elasticsearch](https://www.elastic.co/docs/deploy-manage/api-keys/elasticsearch-api-keys).
- El endpoint de inferencia `.jina-embeddings-v5-omni-small` tiene que existir. El id sale de
  `ELASTIFLIX_INFERENCE_ID` en `.env`, con el mismo valor por defecto en
  [`application.yml`](elastiflix-web/src/main/resources/application.yml). Comprobarlo:

  ```bash
  curl -sS -H "Authorization: ApiKey $ELASTICSEARCH_API_KEY" \
    "$ELASTICSEARCH_URL/_inference/embedding/.jina-embeddings-v5-omni-small"
  ```

  El modelo es de tipo `embedding`. El cliente Java pide los vectores con `TaskType.Embedding`.
  `text_embedding` no es el tipo de este endpoint. La referencia de esa API está en la
  [operación Get inference](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-inference-get).
- Las pruebas de integración leen del alias las películas `597`, `27205`, `10681`, `345887`, `134` y
  `87827`. Las de §6 se omiten si `_count` no es 6 899.

**Preparar el catálogo** (solo si el proyecto no tiene `elastiflix-reloaded`)

La aplicación no crea un catálogo vacío: lee siempre por el alias. En un proyecto nuevo:

1. Crear el índice concreto `elastiflix-reloaded` con el mapping del repositorio:

   ```bash
   curl -sS -X PUT "$ELASTICSEARCH_URL/elastiflix-reloaded" \
     -H "Authorization: ApiKey $ELASTICSEARCH_API_KEY" -H 'Content-Type: application/json' \
     --data-binary @elasticsearch/elastiflix-reloaded-mapping.json
   ```

2. Migrarlo a alias (tarea 1.8). Copia el índice a `elastiflix-reloaded-<AAAAMMDD-HHMMSS>`, borra el
   concreto y crea el alias en una sola llamada; termina solo:

   ```bash
   ./mvnw -q install -DskipTests
   ./mvnw -pl elastiflix-web spring-boot:run "-Dspring-boot.run.arguments=--elastiflix.bootstrap.enabled=true --spring.main.web-application-type=none"
   ```

   Si el nombre ya es un alias, el comando no crea ni borra índices: el log dice a cuál apunta y el
   proceso termina con código 0.
3. Arrancar la aplicación (`./mvnw -pl elastiflix-web spring-boot:run`), abrir
   http://localhost:8080/cargar e importar el archivo de películas en la pestaña «Importar .json» o
   «Importar .csv». Los vectores de la sinopsis se generan con inferencia.

**Comandos**

```bash
./mvnw -v                                        # Maven 3.10.0 y Java 25
./mvnw -q validate                               # enforcer de Java y Maven
./mvnw -q -DskipTests package                    # jar de elastiflix-web
./mvnw verify                                    # unitarias, ArchUnit, Checkstyle y PMD
./mvnw verify -Pit                               # lo anterior más integración y smoke (requiere .env)
./mvnw -q install -DskipTests
./mvnw -pl elastiflix-web spring-boot:run "-Dspring-boot.run.arguments=--elastiflix.bootstrap.enabled=true --spring.main.web-application-type=none"
./mvnw -pl elastiflix-web spring-boot:run        # http://localhost:8080
```

En la ejecución del 2026-10-18 04:47 UTC la migración encontró el alias `elastiflix-reloaded` ya en
`elastiflix-reloaded-20261018-003535`, con 4 322 documentos del catálogo histórico, y no lo cambió. El
2026-10-18 05:44 UTC, con la aprobación de Adolfo, la página de carga importó `movies.json` (6 899
registros, 0 errores) y a las 05:49 UTC el alias pasó a `elastiflix-reloaded-20261018-054429`; el índice
anterior se borró. `spring-boot:run` abre http://localhost:8080 y solo lee ese alias. `GET /` respondió
200 en la corrida de las 04:49 UTC.

Las capturas de la aplicación en marcha están en [docs/capturas/](docs/capturas/).

**Qué ya funciona**

- Conexión al proyecto Serverless con la URL y la API key de `.env`.
- Estado del catálogo: alias, índice vigente y conteo.
- Vectores de consulta (`search`, prefijo `Query: `) y de sinopsis (`ingest`, sin prefijo).
- Recorte de un vector de 1024 a 512 y a 256, con norma 1.
- Búsqueda de solo lectura sobre el alias: full-text (BM25), vectorial (k-NN) e híbrida (RRF).
  Las pruebas de §6 afirman los totales medidos sobre las 6 899 películas y se omiten si `_count`
  ya no es ese número.
- Filtros, facetas, paginación y los dos estados vacíos, en `SearchService` y en la página de
  resultados.
- Carga de un `.json` o un `.csv`: comprobación de nombre, tamaño y UTF-8, validación todo o nada
  contra el mapping e informe de errores. Los vectores de la sinopsis se generan con inferencia;
  si el archivo los trae, se ignoran.
- Importación al catálogo por un alias propio de prueba: índice temporal, lotes de 100, publicación
  en una sola llamada (el índice anterior se borra) y vuelta atrás si falla o se cancela.
- Registro individual directo al alias, visible en la lectura siguiente.
- Cuatro páginas: Inicio con los tres modos y sugerencias, Resultados, Cargar datos y Acerca de
  (tabla de precisión y créditos de TMDB). `GET /actuator/health` incluye el indicador del catálogo.
- Una excepción `EFX-*` no capturada muestra su código, y una ruta desconocida muestra un 404 con
  navegación.
- Smoke de punta a punta: `GET /actuator/health` en `UP`, con el alias de la configuración y las
  6 899 películas de §6; `/`, `/acerca` y `/cargar` responden 200; una sugerencia de cada modo devuelve
  resultados; subir `elasticsearch/fixtures/movies-sample.csv` lo deja validado, con 0 errores y la
  advertencia `EFX-ING-020`, sin importar. Alias, índice y conteo quedan iguales antes y después.

`elastiflix.embedding.document-input-type` y `elastiflix.embedding.document-prefix` no están en el
YAML: sus valores por defecto viven en `@DefaultValue` (`ingest` y cadena vacía). El alias y el id
de inferencia salen de `application.yml` (valores por defecto) o de `.env`
(`ELASTIFLIX_INDEX_ALIAS`, `ELASTIFLIX_INFERENCE_ID`). `.env.example` deja vacías la URL y la API
key y trae los valores por defecto del resto (alias, inferencia, carga, pósters y puerto).

En Windows, ejecuta estos comandos en **Git Bash** (ver `AGENTS.md` › Comandos).

## 🔐 Seguridad y secretos

La demo no tiene autenticación (es intencional y está fuera de alcance). Los POST no tienen protección CSRF: no hay sesión que proteger y la usa una sola persona. Los secretos van solo en `.env` (ver `.env.example`); no se escriben en el código, el YAML, las pruebas ni los logs.

## 🤖 Desarrollo con agentes de IA

El plan está preparado para que **cualquiera de estos agentes** lo ejecute o lo retome donde otro lo dejó:

| Herramienta | Qué lee | Archivo en este repo |
|---|---|---|
| 🟣 **Cursor** *(programó las fases 0 a 6 y la revisión 1)* | `AGENTS.md` en la raíz + reglas `.mdc` | `AGENTS.md`, `.cursor/rules/elastiflix-java.mdc` |
| 🟠 **Claude Code** | `CLAUDE.md`, que importa `AGENTS.md` | `CLAUDE.md` → `@AGENTS.md` |
| 🔵 **Google Antigravity** | `AGENTS.md` en la raíz (máx. 12 000 caracteres) | `AGENTS.md` |
| 🟢 **JetBrains Junie** | `AGENTS.md` en la raíz | `AGENTS.md` |

### ▶️ Para empezar

El plan ya está ejecutado: las siete fases están en `main`. Esta frase era la orden de arranque.

Abre la carpeta del repositorio en tu herramienta y pide:

> Apruebo el plan. Lee `PLAN.md`, `AGENTS.md` y `docs/convenciones-java.md`, aplica el protocolo de
> reanudación y ejecuta las fases en orden desde la Fase 0.

### 🛠️ Para trabajar en las correcciones de la Revisión 2

Las 27 correcciones (`S-01` … `S-27`) las puede tomar **Claude Code**, **Google Antigravity** o
**JetBrains Junie**. Abre la carpeta del repositorio en tu herramienta y pide:

> Lee `PLAN.md`, `AGENTS.md`, `docs/convenciones-java.md` y `docs/revisiones/revision-2.md`. Aplica el
> protocolo de reanudación y atiende la Revisión 2 en orden desde el próximo paso de
> `progreso/revision-2.md`: un commit y un push por corrección en la rama `revision-2`. Detente en S-18
> para pedirme la aprobación de las capturas, y al final abre un PR hacia `main`.

Si otra herramienta ya empezó, el mismo pedido continúa donde se quedó: el avance está en
`progreso/revision-2.md` y en la rama `revision-2`.

Las 27 correcciones están terminadas (2026-10-18); el pedido queda como referencia para una revisión
siguiente.

### 🔁 Si una sesión se interrumpe

Sin tokens, contexto lleno, herramienta cerrada o cambio de agente — no se pierde el avance: al terminar
**cada tarea** el agente actualiza `progreso/`, hace un commit y lo publica con `git push`. Al cerrar
cada fase actualiza también esta documentación. Pide:

> Retoma el trabajo: aplica el protocolo de reanudación de `AGENTS.md` y continúa desde el próximo
> paso registrado en tu archivo de `progreso/`.

### 🔒 Aprobar o pedir cambios a un diseño

Las fases 1, 2 y 3 se detienen en `esperando aprobación de diseño`. Responde con una de estas frases:

> ✅ Apruebo el diseño de la Fase N tal como está. Regístralo en `progreso/N.md` y continúa.

> ✏️ No apruebo el diseño de la Fase N. Cambia lo siguiente: … Actualiza `docs/diseno/N.md` y vuelve a
> pedirme aprobación.

## 🗺️ Plan y estado

| Fase | Nombre | 🔒 Aprobación de diseño | Estado |
|---|---|---|---|
| 0 | 🏗️ Andamiaje y calidad | — | ✅ terminado |
| 1 | ⚙️ Núcleo: conexión, catálogo y embeddings | 🔒 | ✅ terminado |
| 2 | 🔎 Búsqueda | 🔒 | ✅ terminado |
| 3 | 📥 Carga de datos | 🔒 | ✅ terminado |
| 4 | 🌐 Interfaz web | — | ✅ terminado |
| 5 | 🧪 Pruebas integrales y smoke test | — | ✅ terminado |
| 6 | 📚 Documentación y cierre | — | ✅ terminado |
| — | 🔧 Revisión 2 (`S-01` … `S-27`) | — | ✅ terminado (PR hacia `main`) |

El estado vigente siempre es el de [`progreso/INDICE.md`](progreso/INDICE.md); esta tabla se actualiza
al cerrar cada fase.

## ✅ Calidad de código

- 📐 **Convenciones:** [`docs/convenciones-java.md`](docs/convenciones-java.md) (estilo JavaMentor:
  Java 25 con mesura, SOLID, métodos cortos, errores con códigos `EFX-*`, logging SLF4J, Javadoc).
- 📝 **Diseño antes de codificar:** cada fase con código empieza por `docs/diseno/<fase>.md`.
- 🤖 **Verificación automática** en `./mvnw verify`:

| Herramienta | Revisa |
|---|---|
| Checkstyle 14.3.0 | Tamaño y complejidad de métodos, nombres, Javadoc, `catch` vacíos, `System.out`, inyección por campo, secretos literales |
| PMD 7.28.0 | Complejidad cognitiva, clases dios, excepciones genéricas, código sin uso |
| ArchUnit 1.5.1 | Matriz de dependencias entre módulos; la web no usa el cliente de Elasticsearch |

## 🎨 Diseño

Pantallas aprobadas en [`design/mockups/`](design/mockups/) con su paleta pastel multitono y
tipografías **Bricolage Grotesque**, **IBM Plex Sans** e **IBM Plex Mono**. Tokens y guía en
[`design/README.md`](design/README.md).

| 🏠 Inicio | 📋 Resultados | 📥 Cargar datos | ℹ️ Acerca de |
|---|---|---|---|
| Búsqueda, modos y sugerencias | Puntuaciones, filtros y paginación | Formulario, archivos y validación | Documentación de la demo |

## 📸 Capturas

Capturas de página completa tomadas el 2026-10-18 con la aplicación conectada al alias
`elastiflix-reloaded` real. El pie muestra el conteo real. El índice y el número vigentes están en
[`progreso/INDICE.md`](progreso/INDICE.md) › «Catálogo vigente». La comparación con los mockups está en
[`progreso/4.md`](progreso/4.md).

| Página | Captura |
|---|---|
| Inicio `GET /` | ![Inicio](docs/capturas/inicio.png) |
| Resultados · Full-text | ![Resultados full-text](docs/capturas/resultados-fulltext.png) |
| Resultados · Vectorial | ![Resultados vectorial](docs/capturas/resultados-vectorial.png) |
| Resultados · Híbrida | ![Resultados híbrida](docs/capturas/resultados-hibrida.png) |
| Cargar datos · Registro individual | ![Registro individual](docs/capturas/cargar-registro.png) |
| Cargar datos · CSV válido, listo para importar | ![CSV válido](docs/capturas/cargar-csv-valido.png) |
| Cargar datos · CSV que no coincide con el mapping | ![CSV inválido](docs/capturas/cargar-csv-invalido.png) |
| Acerca de `GET /acerca` | ![Acerca de](docs/capturas/acerca.png) |
| Ruta desconocida | ![No encontrada](docs/capturas/no-encontrada.png) |

## 📖 Documentación

| Documento | Para qué |
|---|---|
| 🗺️ [`PLAN.md`](PLAN.md) | Plan de implementación por fases (fuente única de verdad) |
| 🤖 [`AGENTS.md`](AGENTS.md) · [`CLAUDE.md`](CLAUDE.md) | Reglas para los agentes de IA |
| 📍 [`progreso/INDICE.md`](progreso/INDICE.md) | Estado actual y próximo paso |
| 🔧 [`docs/revisiones/revision-2.md`](docs/revisiones/revision-2.md) | Correcciones abiertas de la Revisión 2 (estado en [`progreso/revision-2.md`](progreso/revision-2.md)) |
| 📐 [`docs/convenciones-java.md`](docs/convenciones-java.md) | Cómo se escribe el código |
| 🏗️ [`docs/arquitectura.mermaid`](docs/arquitectura.mermaid) | Arquitectura y flujos |
| ⚙️ [`docs/diseno/1.md`](docs/diseno/1.md) | Diseño aprobado del núcleo y tabla de calibración |
| 🔎 [`docs/diseno/2.md`](docs/diseno/2.md) | Diseño aprobado de la búsqueda |
| 📥 [`docs/diseno/3.md`](docs/diseno/3.md) | Diseño aprobado de la carga |
| 🌐 [`docs/diseno/4.md`](docs/diseno/4.md) | Diseño de la interfaz web |
| 🧪 [`docs/diseno/5.md`](docs/diseno/5.md) | Diseño del smoke test |
| 📸 [`docs/capturas/`](docs/capturas/) | Capturas de las páginas |
| 🗄️ [`elasticsearch/elastiflix-reloaded-mapping.json`](elasticsearch/elastiflix-reloaded-mapping.json) | Mapping del índice |
| 🎨 [`design/README.md`](design/README.md) | Mockups y tokens de diseño |

**Referencias de Elasticsearch:**
[match](https://www.elastic.co/docs/reference/query-languages/query-dsl/query-dsl-match-query) ·
[multi_match](https://www.elastic.co/docs/reference/query-languages/query-dsl/query-dsl-multi-match-query) ·
[kNN retriever](https://www.elastic.co/docs/reference/elasticsearch/rest-apis/retrievers/knn-retriever) ·
[RRF retriever](https://www.elastic.co/docs/reference/elasticsearch/rest-apis/retrievers/rrf-retriever) ·
[dense_vector](https://www.elastic.co/docs/reference/elasticsearch/mapping-reference/dense-vector) ·
[alias](https://www.elastic.co/docs/manage-data/data-store/aliases)

---

<div align="center">

🔒 Repositorio privado · Hecho con ☕ y 🍿

</div>
