# 🗺️ PLAN DE DESARROLLO — Elastiflix-Movies Java

> **Propósito.** Fuente única de verdad para construir **Elastiflix-Movies Java**, una aplicación web
> que demuestra la búsqueda **full-text (BM25)**, **vectorial (k-NN)** e **híbrida (RRF)** de
> películas sobre un índice de **Elasticsearch Serverless**, y la carga segura de datos.
> Escrito para ser **ejecutado por un agente de IA**: Claude Code, Cursor, Google Antigravity o
> JetBrains Junie.
>
> **Cómo usar este plan (para el agente).**
> 1. **Al iniciar cualquier sesión, aplica el protocolo de reanudación** (§9): el trabajo pudo
>    interrumpirse a mitad de una tarea.
> 2. Lee `AGENTS.md` (Claude Code lo carga desde `CLAUDE.md`) y `docs/convenciones-java.md` completos
>    antes de tocar código.
> 3. **Diseña antes de codificar:** cada fase con código empieza por su tarea `.0`
>    (`docs/diseno/<fase>.md`). En las fases con 🔒, **detente** hasta que el usuario apruebe el diseño.
> 4. Ejecuta las **fases en orden** (Fase 0 → Fase 6). No adelantes fases ni tareas.
> 5. Cada tarea tiene rutas, comandos y **criterio de aceptación**. No la marques hecha si el criterio
>    no pasa.
> 6. **Un commit por tarea terminada**, con el código y `progreso/<fase>.md` actualizado, con el id como
>    prefijo (`2.3: estrategia full-text`). Nunca empieces una tarea con cambios sin commit.
> 7. Al terminar una fase, corre su **"Verificación de fase"**; no continúes si falla.
> 8. Nunca escribas secretos en el código ni en git. Todo secreto va por `.env`.
> 9. **No agregues funciones que este plan no pide.** Si algo no está definido, pregúntalo al usuario.

## Índice

- [1. Contexto y objetivo](#1-contexto-y-objetivo)
- [2. Arquitectura](#2-arquitectura)
- [3. Stack y versiones](#3-stack-y-versiones)
- [4. Estructura del repositorio](#4-estructura-del-repositorio)
- [5. Contrato de la aplicación web](#5-contrato-de-la-aplicación-web)
- [6. Modelo de datos](#6-modelo-de-datos)
- [7. Convenciones del proyecto](#7-convenciones-del-proyecto)
- [8. Calidad de código](#8-calidad-de-código)
- [9. Estado del trabajo y reanudación](#9-estado-del-trabajo-y-reanudación)
- [Fases](#fases)
  - [Fase 0 — Andamiaje y calidad](#fase-0--andamiaje-y-calidad)
  - [Fase 1 — Núcleo: conexión, catálogo y embeddings 🔒](#fase-1--núcleo-conexión-catálogo-y-embeddings-)
  - [Fase 2 — Búsqueda 🔒](#fase-2--búsqueda-)
  - [Fase 3 — Carga de datos 🔒](#fase-3--carga-de-datos-)
  - [Fase 4 — Interfaz web](#fase-4--interfaz-web)
  - [Fase 5 — Pruebas integrales y smoke test](#fase-5--pruebas-integrales-y-smoke-test)
  - [Fase 6 — Documentación y cierre](#fase-6--documentación-y-cierre)
- [Definición de Hecho (global)](#definición-de-hecho-global)
- [Variables de entorno](#variables-de-entorno-envexample)
- [Referencias](#referencias)

---

## 1. Contexto y objetivo

**Qué es.** Una demo que permite escribir una consulta y comparar cómo responden tres modos de
búsqueda sobre el mismo catálogo de **4 322 películas**, acotar los resultados con filtros dinámicos y
cargar nuevas películas sin dejar nunca el catálogo a medias.

**Qué debe lograr.**

| Capacidad | Resumen |
|---|---|
| 🔤 Full-text (léxica) | `match` sobre `title` **o** `original_title`, o `multi_match` `best_fields` sobre ambos; operador OR/AND; puntuación BM25 |
| 🧭 Vectorial | `knn` sobre `plot_256`, `plot_512` o `plot_1024`; `k` (100) y `num_candidates` (200) ajustables; similitud `(1 + coseno) / 2` |
| 🔀 Híbrida | Retriever `rrf` con el componente full-text y el vectorial; `rank_constant = 60`, `rank_window_size = k`; muestra BM25, k-NN, sus posiciones y la puntuación RRF |
| 🎛️ Filtros | `adult`, `genres`, `release_date` (desde–hasta), `spoken_languages`, `rating`, con conteos calculados sobre los resultados |
| 📄 Paginación | 25 · 50 · 100 por página |
| 📥 Carga de datos | Registro individual, `.json` o `.csv` (≤ 150 MB, UTF-8); validación completa contra el mapping; importación en índice temporal y swap atómico de alias; vuelta atrás si algo falla |
| ℹ️ Acerca de | Documentación de la demo dentro de la aplicación |

**Fuera de alcance** (no implementar sin que el usuario lo pida): autenticación, comparación lado a
lado, subselectores de fusión, antivirus o análisis de malware de archivos (lo hace el proveedor del
servidor de aplicaciones), despliegue en la nube, CI/CD.

**Diseño visual aprobado:** los mockups de `design/mockups/` (ver `design/README.md`). La interfaz
real **reproduce fielmente** esos mockups: paleta, tipografías, textos, componentes y comportamiento.
No los rediseñes.

## 2. Arquitectura

```mermaid
flowchart LR
  user(["Usuario"]) --> web["elastiflix-web<br/>Thymeleaf + Spring MVC"]
  web --> search["elastiflix-search<br/>BM25 · k-NN · RRF · facetas"]
  web --> ingest["elastiflix-ingest<br/>validación · importación"]
  search --> core["elastiflix-core<br/>cliente ES · catálogo · embeddings"]
  ingest --> core
  core --> es[("Elasticsearch Serverless<br/>alias elastiflix-reloaded")]
  core --> inf["Inferencia<br/>.jina-embeddings-v5-omni-small"]
```

El diagrama completo, con el flujo numerado de búsqueda (1–7) y de carga (A–F), está en
[`docs/arquitectura.mermaid`](docs/arquitectura.mermaid).

**Notas de implementación**

- **Un solo ejecutable:** `elastiflix-web` (Spring Boot). `core`, `search` e `ingest` son librerías.
- **Alias como punto de lectura:** todas las búsquedas van al alias `elastiflix-reloaded`. El índice
  real se llama `elastiflix-reloaded-<AAAAMMDD-HHMMSS>` (UTC). Una carga escribe en un índice nuevo y
  publica con un swap atómico de alias.
- **Estado actual del clúster:** hoy `elastiflix-reloaded` es un **índice concreto**, no un alias. La
  tarea **1.8** lo migra a alias; el borrado del índice original requiere la **aprobación explícita del
  usuario** en la conversación.
- **Serverless:** no hay `_cluster/health`; el indicador de salud consulta el alias y su conteo
  (`_count`). No se configuran shards ni réplicas.
- **Vectores fuera de las respuestas:** las búsquedas excluyen `plot_256`, `plot_512` y `plot_1024`
  de `_source`.
- **Facetas:** en Full-text salen de agregaciones `terms`; en Vectorial e Híbrida se calculan en la
  aplicación sobre los resultados devueltos (hasta `k`), porque en RRF las agregaciones incluirían
  películas que no están en la lista combinada.
- **Híbrida con puntuaciones por componente:** el orden final lo da el retriever `rrf`; para mostrar
  BM25, similitud k-NN y la posición de cada película en su lista, la aplicación ejecuta también los dos
  componentes por separado (mismos filtros, `size = k`) en el mismo `_msearch`.

## 3. Stack y versiones

Versiones verificadas en la web el 2026-10-17.

| Componente | Tecnología | Versión |
|---|---|---|
| Lenguaje | Java (LTS) | **25** |
| Framework | Spring Boot (padre `spring-boot-starter-parent`) | **4.1.1** (Spring Framework ≥ 7.0.9) |
| Build | Maven mediante Maven Wrapper | **3.10.0** |
| Plantillas | Thymeleaf (`spring-boot-starter-thymeleaf`) | **3.1.5** (gestionada por Boot) |
| Cliente de búsqueda | Elasticsearch Java client `co.elastic.clients:elasticsearch-java` | **9.4.5** (gestionada por Boot; sobrescribir `elasticsearch-client.version` a 9.5.4 solo si hace falta) |
| JSON | Jackson 3 (`tools.jackson`), `Jackson3JsonpMapper` | **3.1.5** (gestionada por Boot) |
| Motor | Elasticsearch Serverless | — |
| Modelo de embeddings | `jina-embeddings-v5-omni-small` (endpoint `.jina-embeddings-v5-omni-small`) | 1024 dims nativas |
| Pruebas | JUnit 5, AssertJ, Mockito (`spring-boot-starter-test`) | gestionadas por Boot |
| Estilo | Checkstyle (`maven-checkstyle-plugin` 3.6.0) | **14.3.0** |
| Análisis estático | PMD (`maven-pmd-plugin` 3.28.0) | **7.28.0** |
| Arquitectura | ArchUnit (`archunit-junit5`) | **1.5.1** |

- No se usa Spring Data Elasticsearch, Lombok ni Jackson 2 en el código propio (Jackson 2 queda como
  dependencia transitiva, sin uso).
- Zona horaria: **UTC**. Paquete base: `io.github.bigfito.elastiflix`. `groupId`: `io.github.bigfito`.

## 4. Estructura del repositorio

```
elastiflix-reloaded/
├── PLAN.md                     ← este plan (fuente única de verdad)
├── AGENTS.md                   ← reglas para agentes (Cursor, Antigravity, Junie, Claude Code)
├── CLAUDE.md                   ← entrada para Claude Code (importa AGENTS.md)
├── README.md
├── .cursor/rules/              ← regla de Cursor para archivos .java
├── .env.example                ← variables de entorno sin valores reales
├── pom.xml                     ← POM padre (packaging pom) · Java 25 · Spring Boot 4.1.1   [Fase 0]
├── mvnw · mvnw.cmd · .mvn/     ← Maven Wrapper 3.10.0                                        [Fase 0]
├── config/
│   ├── checkstyle/checkstyle.xml
│   └── pmd/pmd-ruleset.xml
├── docs/
│   ├── arquitectura.mermaid
│   ├── convenciones-java.md
│   └── diseno/_PLANTILLA.md    ← y un <fase>.md por fase con código
├── design/
│   ├── README.md               ← tokens de diseño y cómo usar los mockups
│   └── mockups/                ← Main · Results · Ingest · About (.dc.html)
├── elasticsearch/
│   ├── elastiflix-reloaded-mapping.json
│   └── fixtures/               ← muestras reales: 4 películas en .json y .csv, vector de consulta
├── progreso/
│   ├── INDICE.md               ← fase actual y próximo paso global
│   └── _PLANTILLA.md           ← estado por fase
├── elastiflix-core/            ← modelo, cliente ES, catálogo (alias, índices), mapping, embeddings  [Fase 1]
├── elastiflix-search/          ← match/multi_match, knn, rrf, filtros, facetas, paginación          [Fase 2]
├── elastiflix-ingest/          ← validación, lectores JSON/CSV, importación por lotes, swap de alias [Fase 3]
└── elastiflix-web/             ← único ejecutable: Spring Boot + Thymeleaf (4 páginas)              [Fases 0 y 4]
```

`progreso/` guarda el **estado del desarrollo**; no tiene relación con ningún estado de la aplicación
(por ejemplo, el estado de un trabajo de importación vive en memoria dentro de `elastiflix-ingest`).

## 5. Contrato de la aplicación web

**Páginas**

| Método y ruta | Página (mockup) | Descripción |
|---|---|---|
| `GET /` | Inicio (`Main.dc.html`) | Caja de búsqueda, selector de modo con sus paneles y sugerencias dinámicas |
| `GET /buscar` | Resultados (`Results.dc.html`) | Resultados paginados, puntuaciones por modo y filtros |
| `GET /cargar` | Cargar datos (`Ingest.dc.html`) | Pestañas Registro individual · Importar .json · Importar .csv |
| `GET /acerca` | Acerca de la demo (`About.dc.html`) | Documentación de la demo |

**Parámetros de `GET /buscar`** (también los usa el formulario de Inicio)

| Parámetro | Valores | Por defecto |
|---|---|---|
| `q` | texto no vacío | — (obligatorio: «Escribe qué película buscas antes de pulsar Buscar.») |
| `modo` | `fulltext` · `vectorial` · `hibrida` | `fulltext` |
| `tipo` | `match` · `multi_match` | `match` |
| `campo` | `title` · `original_title` (solo con `match`) | `title` |
| `operador` | `or` · `and` | `or` |
| `vector` | `plot_256` · `plot_512` · `plot_1024` | `plot_256` |
| `k` | entero ≥ 1 | `100` |
| `candidatos` | entero, `k ≤ candidatos ≤ 10000` | `200` |
| `pagina` | entero ≥ 1 | `1` |
| `tamano` | `25` · `50` · `100` | `25` |
| `adult` | `true` · `false` (repetible) | sin filtro |
| `genero`, `idioma`, `clasificacion` | valores de `genres`, `spoken_languages`, `rating` (repetibles) | sin filtro |
| `desde`, `hasta` | `yyyy-MM-dd`; `desde ≤ hasta` | sin filtro |

**Carga de datos** (respuestas JSON salvo donde se indica)

| Método y ruta | Uso |
|---|---|
| `POST /cargar/registro` | Guarda una película desde el formulario (redirige a `/cargar` con el resultado) |
| `POST /cargar/archivo` (multipart: `archivo`, `separador`, `encabezado`) | Sube el archivo, repite las comprobaciones de tipo, tamaño y UTF-8, y lanza la validación; devuelve `jobId` |
| `GET /cargar/trabajos/{jobId}` | Estado del trabajo: paso del proceso (1–5), conteos, errores (primeros 6), advertencias, comprobaciones, progreso |
| `POST /cargar/trabajos/{jobId}/importar` | Inicia la importación de un archivo ya validado |
| `POST /cargar/trabajos/{jobId}/cancelar` | Cancela la importación y borra el índice temporal |
| `GET /cargar/trabajos/{jobId}/informe.csv` | Informe completo de errores (CSV UTF-8) |

**Actuator:** `GET /actuator/health` con el componente `elastiflixCatalog` (`alias`, `index`,
`documents`). Alimenta el indicador «Conectado · Elasticsearch Serverless» del header y el pie
«Índice: elastiflix-reloaded · 4 322 películas».

**Límites entre módulos** (interfaces que define el diseño de la Fase 1 y consumen las demás):
`CatalogIndex`, `EmbeddingService`, `IndexMapping`, `CatalogStatusService` (en `core`);
`SearchService` (en `search`); `IngestService` (en `ingest`). La web solo conoce estas fachadas y sus
`record` de entrada y salida.

## 6. Modelo de datos

- **Mapping:** [`elasticsearch/elastiflix-reloaded-mapping.json`](elasticsearch/elastiflix-reloaded-mapping.json)
  (33 campos). Es la referencia para crear índices y para validar cargas. `elastiflix-core` lo empaqueta
  como recurso desde esa misma ruta (sin copiarlo).
- **Campos que usa la demo:**

| Campo | Tipo | Uso |
|---|---|---|
| `id` | keyword | Identificador; es el `_id` del documento |
| `title`, `original_title` | text | Full-text, Híbrida |
| `plot` | text | Sinopsis; origen de los vectores; se muestra recortada a 3 líneas |
| `plot_256`, `plot_512`, `plot_1024` | dense_vector · cosine · `hnsw` (m 16, ef_construction 100) | Vectorial, Híbrida |
| `adult` | boolean | Filtro |
| `genres` | keyword | Filtro |
| `release_date` | date `yyyy-MM-dd` | Filtro (rango) |
| `spoken_languages` | keyword | Filtro |
| `rating` | keyword (`G`, `PG`, `PG-13`, `R`, `NC-17`, `NR`) | Filtro |
| `keywords`, `belongs_to_collection` | nested | Se capturan en la carga |

- **Vectores:** 512 y 256 son los primeros N valores del vector de 1024, renormalizados (L2). Prefijos:
  `Query: ` en consultas (`input_type: search`) y `Document: ` en sinopsis.
- **Formatos de carga** (muestras reales en `elasticsearch/fixtures/`):
  - **JSON:** objeto de exportación `{index, exported_at, document_count, csv_file, load_notes, mapping, documents[]}`,
    un arreglo de películas o NDJSON. Si trae `document_count`, debe coincidir con `documents`.
  - **CSV:** UTF-8 con o sin BOM, separador coma (o `;` / tabulador si el usuario lo elige), encabezado,
    fin de línea `\r\n` o `\n`, celdas multilínea entre comillas; listas, objetos anidados y vectores
    como JSON dentro de la celda.
  - En ambos formatos puede venir el metadato `_id`; si viene, debe ser igual a `id`. `id` puede llegar
    como número y se guarda como texto.
- **Datos de verificación** (catálogo intacto de 4 322 películas, `k = 100`, `num_candidates = 200`):

| Modo y ajustes | Consulta | Esperado |
|---|---|---|
| `match` · `title` | `dark knight` | OR 40 · AND 3 |
| `match` · `title` | `love story` | OR 65 · AND 1 |
| `match` · `title` | `Kolejne 365 dni` | 0 |
| `match` · `original_title` | `el secreto` | OR 16 · AND 1 |
| `match` · `original_title` | `le fabuleux destin` | OR 14 · AND 1 |
| `multi_match` | `heaven cielo` | OR 6 · AND 0 |
| `multi_match` | `next kolejne` | OR 11 · AND 0 |
| `multi_match` | `parasite 기생충` | OR 1 · AND 0 |
| Vectorial | `un barco de lujo choca con un iceberg en su viaje inaugural` | Titanic (`597`): 4.º en `plot_256`, 3.º en `plot_512`, 1.º en `plot_1024` |
| Vectorial | `un joven náufrago comparte un bote salvavidas con un tigre de Bengala en medio del océano` | Life of Pi (`87827`): 10.º · 3.º · 2.º |
| Vectorial | `un robot solitario que limpia la Tierra` | WALL·E (`10681`): 1.º en los tres campos |
| Híbrida · `match` `title` AND | `toy story juguetes que cobran vida` | componente léxico vacío; los `k` resultados vienen del vectorial |

## 7. Convenciones del proyecto

- **Idioma:** interfaz, mensajes, logs, Javadoc y documentación en español; identificadores en inglés.
- **Fechas:** UTC; `release_date` en `yyyy-MM-dd`.
- **Configuración:** por variables de entorno o `.env` en la raíz (importado con
  `spring.config.import=optional:file:.env[.properties]` y la ruta relativa al módulo web, la que
  decida el diseño de la Fase 1). Prefijo propio `elastiflix.*`.
- **Secretos:** solo en `.env` (excluido por `.gitignore`). Nunca en código, YAML, pruebas, logs ni
  commits.
- **Integración contra Elasticsearch:** las pruebas de lectura usan `elastiflix-reloaded` sin
  modificarlo; las de escritura usan su propio alias `elastiflix-it-<aleatorio>` y lo borran al terminar.
- **Textos de la interfaz:** los de los mockups, palabra por palabra.

## 8. Calidad de código

**Convenciones.** [`docs/convenciones-java.md`](docs/convenciones-java.md) es obligatorio y manda sobre
las preferencias por defecto de cualquier herramienta. Es la skill `java-developer` (persona
JavaMentor) aplicada a este proyecto, con umbrales y desviaciones declaradas.

**Diseño antes de codificar.** Cada fase con código empieza por su tarea `.0`. **Compuertas de
aprobación del usuario** (🔒) — el agente se detiene con el estado `esperando aprobación de diseño`:

| Fase | Motivo |
|---|---|
| 🔒 **1** — Núcleo | Define los contratos (`CatalogIndex`, `EmbeddingService`, `IndexMapping`, errores) que usan todas las demás y la migración del índice a alias |
| 🔒 **2** — Búsqueda | Concentra los algoritmos centrales: construcción de `match`/`multi_match`, `knn`, `rrf`, filtros y facetas |
| 🔒 **3** — Carga de datos | Orquestación con índice temporal, swap de alias y vuelta atrás; manejo de archivos de 150 MB |

En las fases 4 y 5 el diseño se escribe y se hace commit, pero no requiere aprobación previa.

**Matriz de dependencias** (verificada con ArchUnit y PMD `JM-DEP-02`):

| Módulo / paquete | Puede depender de | No puede depender de |
|---|---|---|
| `...core..` | JDK, `co.elastic.clients..`, `tools.jackson..`, `org.slf4j..`, `org.springframework.context/beans/boot` (sin web) | `search`, `ingest`, `web`, Spring MVC, Thymeleaf |
| `...search..` | `core`, `co.elastic.clients..`, `tools.jackson..`, `org.slf4j..`, Spring (sin web) | `ingest`, `web`, Spring MVC, Thymeleaf |
| `...ingest..` | `core`, `co.elastic.clients..`, `tools.jackson..`, `org.slf4j..`, Spring (sin web) | `search`, `web`, Spring MVC, Thymeleaf |
| `...web..` | `core` (solo errores y estado del catálogo), fachadas y `record` de `search` e `ingest`, Spring MVC, Thymeleaf, Actuator | `co.elastic.clients..`, clases internas de `search` e `ingest` |

Además: sin ciclos entre paquetes; los controladores solo en `...web..` y con nombre `*Controller`;
ninguna clase usa `System.out`/`System.err`.

**Verificación automática.**

| Comando | Qué ejecuta |
|---|---|
| `./mvnw -q test` | Pruebas unitarias |
| `./mvnw verify` | Unitarias + arquitectura (ArchUnit) + Checkstyle + PMD. Sin red. **Lo corren todas las fases** |
| `./mvnw verify -Pit` | Lo anterior + pruebas de integración (`*IT`) contra Elasticsearch Serverless (requiere `.env`) |

En Windows usa `mvnw.cmd` en lugar de `./mvnw`.

**Cierre de fase.** El agente escribe en `progreso/<fase>.md` el **Resumen de diseño** y responde la
**Revisión de convenciones** (lista de `docs/convenciones-java.md` §16). El usuario la confirma al
revisar la fase.

## 9. Estado del trabajo y reanudación

**Archivos.** `progreso/INDICE.md` (fase actual, estado de cada fase y próximo paso global) y
`progreso/<fase>.md` (`0.md` … `6.md`), creado desde `progreso/_PLANTILLA.md` al empezar cada fase.
Se versionan en git; `.gitignore` nunca los excluye.

**Estados de una fase:** `no iniciado` · `en curso` · `esperando aprobación de diseño` · `bloqueado` ·
`en verificación` · `terminado`.

**Disciplina por tarea.**

1. **Al empezar:** marca la tarea `en curso`, escribe tarea actual, subpaso y **próximo paso** como una
   acción concreta que otro agente pueda ejecutar sin contexto, y anota el hash del commit anterior.
2. **Durante:** actualiza subpaso y "Cambios sin commit" tras cada archivo terminado y antes de cada
   comando largo (suite completa, `-Pit`, refactorizaciones amplias).
3. **Al pasar el criterio:** márcala `terminada` con evidencia (comando y resultado), vacía "Cambios sin
   commit", escribe el nuevo próximo paso y haz **un commit con el código y el archivo de estado**:
   `<id>: <resumen>`.
4. **Nunca** empieces una tarea con cambios sin commit de la anterior.
5. **Si te bloqueas:** estado `bloqueado`, descríbelo en "Bloqueos y solicitudes", commit y detente.
6. **Al terminar la fase:** estado `terminado`, última verificación, resumen de diseño, revisión de
   convenciones; actualiza `progreso/INDICE.md` en el mismo commit.

**Protocolo de reanudación** (toda sesión, nueva o reanudada, tras `/clear` o compactación):

1. **Ubícate:** rama (`git branch --show-current`) y carpeta.
2. **Lee el estado:** `progreso/INDICE.md` y `progreso/<fase actual>.md`. Si la fase no tiene archivo,
   no ha empezado: créalo desde `progreso/_PLANTILLA.md`.
3. **Revisa git:** `git status` y `git log --oneline -10`.
4. **Contrasta:**
   - `esperando aprobación de diseño` → no escribas código; pregunta al usuario si aprueba
     `docs/diseno/<fase>.md` y registra la respuesta;
   - cambios sin commit que coinciden con el archivo de estado → continúa sobre ellos;
   - cambios sin commit **no registrados** → no los descartes ni sobrescribas; descríbelos al usuario y
     pregunta;
   - commits posteriores a la última actualización del archivo → reconstruye la tabla de tareas desde
     los mensajes de commit (`<id>: …`) y actualiza el archivo.
5. **Confirma el punto real:** `./mvnw verify` (y las pruebas de la tarea actual).
6. **Registra la reanudación** en "Decisiones": `Sesión reanudada <fecha UTC> con <herramienta>: <punto encontrado>`.
7. **Continúa desde el próximo paso**, sin rehacer tareas terminadas con commit.

---

## FASES

### Fase 0 — Andamiaje y calidad

**Objetivo:** un mono-repo Maven que compila con Java 25 y cuyo build falla ante cualquier violación de
estilo, análisis estático o arquitectura.

- [ ] **0.1** Crea `progreso/0.md` desde `progreso/_PLANTILLA.md`; marca la Fase 0 `en curso` en
  `progreso/INDICE.md`. Comprueba que git no ignora el estado ni versiona secretos.
  - Criterio: `git check-ignore -q progreso/INDICE.md` devuelve **1** (no ignorado) y
    `git check-ignore -q .env` devuelve **0** (ignorado).
- [ ] **0.2** Genera el Maven Wrapper 3.10.0 en la raíz: `mvn -N wrapper:wrapper -Dmaven=3.10.0`
  (requiere un Maven local ≥ 3.6.3 solo para este paso). Versiona `mvnw`, `mvnw.cmd` y `.mvn/wrapper/`.
  - Criterio: `./mvnw -v` muestra `Apache Maven 3.10.0` y `Java version: 25`.
- [ ] **0.3** Crea el POM padre `pom.xml`: padre `org.springframework.boot:spring-boot-starter-parent:4.1.1`;
  `io.github.bigfito:elastiflix-reloaded:0.1.0-SNAPSHOT`, `packaging pom`; módulos `elastiflix-core`,
  `elastiflix-search`, `elastiflix-ingest`, `elastiflix-web`; `java.version` 25; propiedades de versión
  de §3 (Checkstyle, PMD, ArchUnit y sus plugins); `maven-enforcer-plugin` con
  `requireJavaVersion [25,)` y `requireMavenVersion [3.6.3,)`.
  - Criterio: `./mvnw -q validate` termina en 0.
- [ ] **0.4** Crea los cuatro módulos con su `pom.xml` (sin versiones propias) y las dependencias de la
  matriz de §8: `core` (cliente `elasticsearch-java`, Jackson 3, `spring-boot-starter`), `search` y
  `ingest` (`core`), `web` (`core`, `search`, `ingest`, starter de Spring MVC de Boot 4, Thymeleaf,
  Actuator, Validation, `spring-boot-maven-plugin`). Agrega un `package-info.java` por paquete base
  (`io.github.bigfito.elastiflix.{core,search,ingest,web}`) y la clase `ElastiflixApplication`
  (`@SpringBootApplication`) en `web`.
  - Criterio: `./mvnw -q -DskipTests package` termina en 0 y genera `elastiflix-web/target/elastiflix-web-0.1.0-SNAPSHOT.jar`.
- [ ] **0.5** Configura Checkstyle (con `config/checkstyle/checkstyle.xml`) y PMD (con
  `config/pmd/pmd-ruleset.xml`, `targetJdk` 25, `printFailingErrors`) en el POM padre, ligados a la fase
  `verify`, con `failOnViolation` y solo sobre `src/main/java`. Sobrescribe la versión de Checkstyle
  (14.3.0) y de PMD (7.28.0) dentro de cada plugin. **No modifiques los archivos de `config/`.**
  - Criterio: `./mvnw verify` termina en 0 con 0 violaciones.
- [ ] **0.6** Agrega ArchUnit (`archunit-junit5`, scope test) y crea
  `elastiflix-web/src/test/java/io/github/bigfito/elastiflix/architecture/ArchitectureTest.java` con las
  reglas de la matriz de §8 (incluida "sin ciclos" y "web no depende de `co.elastic.clients..`").
  Importa las clases de producción de los cuatro módulos (`ImportOption.DoNotIncludeTests`).
  - Criterio: `./mvnw verify` ejecuta `ArchitectureTest` y queda en verde.
- [ ] **0.7** **Prueba negativa** de las compuertas: en una rama temporal, agrega a `web` una clase
  que importe `co.elastic.clients.elasticsearch.ElasticsearchClient` y use `System.out.println`; corre
  `./mvnw verify`; confirma que falla por `JM-LOG-01` (Checkstyle), `JM-DEP-02` (PMD) y ArchUnit;
  **revierte** y borra la rama. Registra la salida resumida como evidencia.
  - Criterio: el build falla con los tres mensajes; tras revertir, `./mvnw verify` vuelve a 0 y
    `git status` queda limpio.
- [ ] **0.8** Confirma que `.env.example` contiene todas las variables de la sección
  [Variables de entorno](#variables-de-entorno-envexample) y ningún valor real.
  - Criterio: `grep -E '=.+' .env.example` solo muestra valores por defecto no secretos (alias, endpoint
    de inferencia, tamaños, puerto).

**Verificación de fase 0:**
```bash
./mvnw -v
./mvnw verify
git check-ignore -q progreso/INDICE.md; echo $?   # 1
git check-ignore -q .env; echo $?                 # 0
```
Criterio: Maven 3.10.0 y Java 25; `verify` en verde con 0 violaciones y `ArchitectureTest` en verde;
prueba negativa (0.7) documentada en `progreso/0.md`.

---

### Fase 1 — Núcleo: conexión, catálogo y embeddings 🔒

**Objetivo:** `elastiflix-core` se conecta a Elasticsearch Serverless, conoce el mapping, administra el
alias y sus índices, y genera vectores de consulta y de documento idénticos a los del catálogo.

- [ ] **1.0** 🔒 Diseño: `docs/diseno/1.md` desde `docs/diseno/_PLANTILLA.md` (clases, contratos de
  `CatalogIndex`, `EmbeddingService`, `IndexMapping`, `CatalogStatusService`, catálogo de errores,
  configuración, secuencia de la migración a alias). Commit y **detente** hasta la aprobación del usuario.
  Incluye en "Dudas" si la autoconfiguración de Spring Boot 4.1.1 para el cliente cubre URL + API key +
  `Jackson3JsonpMapper` o si se declara el bean a mano.
- [ ] **1.1** Catálogo de errores en `core/.../core/error/`: `ErrorCode` y la jerarquía de
  `ElastiflixException` (`docs/convenciones-java.md` §7).
  - Criterio: pruebas unitarias verifican el formato `<código> · <qué pasó>. Causa probable: … Acción
    sugerida: …` y que los códigos son únicos.
- [ ] **1.2** Configuración tipada (`@ConfigurationProperties` + `@Validated`) para
  `ELASTICSEARCH_URL`, `ELASTICSEARCH_API_KEY` y `elastiflix.*` (alias, id del endpoint de inferencia,
  tamaño máximo de archivo, tamaño de lote). Importación de `.env` desde `application.yml` de `web`.
  - Criterio: sin `ELASTICSEARCH_URL` la aplicación no arranca y muestra `EFX-CFG-001` con la acción
    sugerida (prueba unitaria del binder).
- [ ] **1.3** Bean `ElasticsearchClient` con autenticación por API key y `Jackson3JsonpMapper`
  (autoconfiguración de Boot o bean propio, según el diseño aprobado). Desactiva el indicador de salud
  de Elasticsearch de Boot (usa `_cluster/health`, que Serverless no ofrece).
  - Criterio: prueba unitaria confirma que el mapper es `Jackson3JsonpMapper`; `ElasticsearchClientIT`
    (perfil `it`) ejecuta `info()` sin error.
- [ ] **1.4** Modelo de dominio en `core/.../core/model/`: `Movie` (los 33 campos del mapping),
  `MovieCollection`, `MovieKeyword`; serialización con Jackson 3 (`snake_case`), `release_date` como
  `LocalDate`, vectores como `float[]`.
  - Criterio: `MovieJsonTest` lee las 4 películas de `elasticsearch/fixtures/movies-sample.json`, las
    vuelve a escribir y obtiene los mismos valores (incluida `belongs_to_collection` de `345887`).
- [ ] **1.5** `IndexMapping`: carga `elasticsearch/elastiflix-reloaded-mapping.json` como recurso del
  jar (empaquetado desde la ruta de la raíz, sin copiarlo) y expone el tipo de cada campo, los
  subcampos de los `nested` y las dimensiones de los vectores.
  - Criterio: prueba unitaria verifica 33 campos, `release_date` con formato `yyyy-MM-dd` y
    `plot_512` con 512 dimensiones.
- [ ] **1.6** `CatalogIndex` (gateway): resolver el índice detrás del alias, crear un índice con el
  mapping, `_reindex` de un índice a otro, `refresh`, `_count`, swap atómico de alias (`remove` + `add`
  en una sola llamada a `_aliases`), borrar índice, comprobar si un nombre es alias o índice.
  - Criterio: `CatalogIndexIT` crea `elastiflix-it-<aleatorio>-a`, lo reindexa a `-b`, cambia un alias
    de prueba de `-a` a `-b` atómicamente y borra todo al terminar, aun si la prueba falla.
- [ ] **1.7** `CatalogStatusService`: devuelve alias, índice vigente, conteo y estado (conectado / sin
  conexión) con el código de error correspondiente.
  - Criterio: `CatalogStatusServiceIT` devuelve `documents = 4322` para `elastiflix-reloaded`.
- [ ] **1.8** Migración del índice concreto a alias (`CatalogBootstrap`), idempotente, que solo corre
  con `elastiflix.bootstrap.enabled=true`:
  1. Si `elastiflix-reloaded` ya es alias → no hace nada y lo registra.
  2. Si es índice concreto → crea `elastiflix-reloaded-<AAAAMMDD-HHMMSS>` con el mapping, reindexa y
     verifica que el conteo coincida.
  3. Solo con `elastiflix.bootstrap.confirm-delete-original=true`: en una sola llamada a `_aliases`
     agrega el alias al índice nuevo y elimina el índice original (`remove_index`). Sin esa bandera se
     detiene después del paso 2 y explica cómo continuar.
  - **Antes de ejecutar el paso 3 contra el clúster real, pide la aprobación explícita del usuario en
    la conversación** (borra el índice original) y regístrala en "Decisiones".
  - Criterio: `CatalogBootstrapTest` (con dobles) cubre los tres caminos; tras la ejecución real,
    `GET _alias/elastiflix-reloaded` apunta a `elastiflix-reloaded-…` con 4 322 documentos y una segunda
    ejecución no cambia nada.
- [ ] **1.9** `EmbeddingService` (`InferenceEmbeddingService`) sobre `_inference` con el endpoint de
  `ELASTIFLIX_INFERENCE_ID`: consulta con `input_type: search` y prefijo `Query: `; documentos con el
  prefijo `Document: ` en lotes; `VectorProjector` recorta a 512 y 256 y renormaliza.
  - Criterio: `VectorProjectorTest` (norma 1 ± 1e-6, primeros N valores proporcionales);
    `EmbeddingServiceIT`: el vector de `elasticsearch/fixtures/query-vector-titanic.json` y el generado
    tienen coseno ≥ 0.999; el vector de documento regenerado para la película `597` tiene coseno ≥ 0.99
    con su `plot_1024` almacenado. **Si este último no se cumple**, marca la tarea `bloqueado` y pregunta
    al usuario cómo se generaron los vectores del catálogo (`input_type` y prefijo).

**Verificación de fase 1:**
```bash
./mvnw verify
./mvnw verify -Pit -pl elastiflix-core -am
```
Criterio: todas las pruebas en verde; alias `elastiflix-reloaded` operativo con 4 322 documentos;
0 violaciones; arquitectura en verde; resumen de diseño y revisión de convenciones en `progreso/1.md`.

---

### Fase 2 — Búsqueda 🔒

**Objetivo:** `elastiflix-search` ejecuta los tres modos con filtros, facetas, paginación y
puntuaciones, y reproduce los datos de verificación de §6.

- [ ] **2.0** 🔒 Diseño: `docs/diseno/2.md` (estrategias por modo, construcción de peticiones,
  `_msearch` de la híbrida, cálculo de facetas, paginación, estados vacíos). Commit y **detente** hasta
  la aprobación del usuario.
- [ ] **2.1** Modelo de entrada en `search/.../search/query/`: `SearchQuery` con texto, modo y ajustes
  (`FullTextSettings`, `KnnSettings`), filtros, página y tamaño; validación con códigos `EFX-SRCH-*`:
  texto vacío, `k < 1`, `num_candidates < k` o `> 10000`, `campo` con `multi_match`, `tamano` fuera de
  {25, 50, 100}, `desde > hasta`, full-text con `from + size > 10000`.
  - Criterio: una prueba unitaria por regla, con el mensaje exacto de los mockups cuando existe
    («num_candidates debe ser mayor o igual que k.», etc.).
- [ ] **2.2** `SearchFilters` → cláusulas de filtro: entre filtros distintos AND; dentro de un filtro OR
  (`terms`); `release_date` como `range` con `gte`/`lte`; `adult` booleano.
  - Criterio: pruebas unitarias comparan el JSON generado con el esperado para cada filtro y su
    combinación.
- [ ] **2.3** Estrategia Full-text: `match` (campo elegido, operador) o `multi_match` `best_fields`
  sobre `title` y `original_title` (operador); filtros en contexto `filter`; `from`/`size`;
  `track_total_hits: true`; agregaciones `terms` de `genres`, `spoken_languages`, `rating`, `adult` y
  `min`/`max` de `release_date`; `_source` sin vectores.
  - Criterio: prueba unitaria del JSON de la petición; `FullTextSearchIT` reproduce las 8 filas
    full-text de §6 (totales exactos).
- [ ] **2.4** Estrategia Vectorial: vector de la consulta (`EmbeddingService`) recortado a las
  dimensiones del campo; retriever `knn` con `field`, `k`, `num_candidates` y `filter`; `size = k`;
  facetas y paginación sobre los `k` resultados.
  - Criterio: `VectorSearchIT` reproduce las 3 filas vectoriales de §6; sin filtros devuelve exactamente
    `k` resultados; toda similitud está en (0, 1].
- [ ] **2.5** Estrategia Híbrida: `_msearch` con (a) retriever `rrf` (`standard` con la consulta
  full-text + `knn`), `rank_constant: 60`, `rank_window_size: k`, `filter` del `rrf`, `size = k`;
  (b) componente full-text solo; (c) componente k-NN solo, ambos con los mismos filtros y `size = k`.
  Cada resultado lleva BM25 y similitud (si aparece en esa lista), su posición en cada lista y la
  puntuación RRF.
  - Criterio: `HybridSearchIT`: para cada resultado, `RRF = Σ 1 / (60 + posición)` con tolerancia 1e-6
    respecto a la puntuación de Elasticsearch; la fila híbrida de §6 se cumple.
- [ ] **2.6** `FacetCalculator`: conteos por valor (orden: conteo desc, luego etiqueta), valores
  seleccionados siempre visibles aunque tengan 0, rango de fechas de los resultados; desde agregaciones
  (Full-text) o desde los resultados (Vectorial e Híbrida).
  - Criterio: pruebas unitarias con listas de resultados construidas a mano.
- [ ] **2.7** Paginación: página, total de páginas, rango («1–25 de 100»), límites por modo (Full-text
  hasta 10 000; Vectorial e Híbrida hasta `k`).
  - Criterio: pruebas unitarias de bordes (última página parcial, `k` no múltiplo del tamaño).
- [ ] **2.8** `SearchService` (fachada): elige la estrategia, mide el tiempo, registra en `INFO` (modo,
  total, ms), devuelve `SearchResult` (resultados de la página, total, facetas, paginación, descripción
  de ajustes) y distingue los estados vacíos «sin resultados» y «sin resultados con estos filtros»
  (este último confirma con un conteo sin filtros).
  - Criterio: pruebas unitarias con estrategias simuladas cubren los tres modos y los dos estados vacíos.

**Verificación de fase 2:**
```bash
./mvnw verify
./mvnw verify -Pit -pl elastiflix-search -am
```
Criterio: todos los datos de verificación de §6 se cumplen; 0 violaciones; arquitectura en verde;
resumen de diseño y revisión de convenciones en `progreso/2.md`.

---

### Fase 3 — Carga de datos 🔒

**Objetivo:** `elastiflix-ingest` valida archivos completos contra el mapping y los importa sin dejar
nunca el catálogo a medias.

- [ ] **3.0** 🔒 Diseño: `docs/diseno/3.md` (lectura en streaming de 150 MB, dos pasadas validación →
  importación, estado del trabajo, cancelación, swap y vuelta atrás). Commit y **detente** hasta la
  aprobación del usuario. Lleva a "Dudas", para que el usuario decida:
  - qué hacer con el índice anterior después de publicar (borrarlo o conservarlo como respaldo);
  - si el registro individual se escribe directamente en el índice vigente o pasa por el índice temporal;
  - qué hacer si el CSV no trae encabezado (la casilla «La primera fila contiene los nombres de
    columna» desmarcada);
  - qué hacer si un registro trae solo algunos de los tres vectores.
- [ ] **3.1** Comprobaciones del archivo (las mismas que hace el navegador, repetidas en el servidor):
  solo `.json`/`.csv`, sin dobles extensiones, máximo 150 MB, contenido UTF-8 válido (con o sin BOM).
  Límite de multipart de Spring alineado a 150 MB.
  - Criterio: pruebas unitarias aceptan `elastiflix-reloaded.json`, `peliculas_2026.csv` y rechazan
    `peliculas.txt`, `peliculas`, `peliculas.csv.exe`, `peliculas.exe.csv`, `peliculas.json.csv`, un
    archivo de 150 MB + 1 byte y uno con bytes no UTF-8, cada uno con su código `EFX-ING-*`.
- [ ] **3.2** `JsonMovieReader`: objeto de exportación, arreglo o NDJSON, en streaming con Jackson 3;
  verifica `document_count` si viene.
  - Criterio: lee las 4 películas de `elasticsearch/fixtures/movies-sample.json` y las mismas
    convertidas a arreglo y a NDJSON en las pruebas.
- [ ] **3.3** `CsvMovieReader`: BOM opcional, separador elegido (coma por defecto, `;`, tabulador),
  encabezado, celdas multilínea entre comillas, JSON dentro de celdas; tabla de correspondencia
  columna → campo (columnas desconocidas = error).
  - Criterio: lee `elasticsearch/fixtures/movies-sample.csv` y produce los mismos valores que el
    JSON para las 4 películas.
- [ ] **3.4** `MappingValidator`: cada campo existe y tiene su tipo (`keyword` texto o lista de textos,
  `text`, `boolean`, `integer`/`long`, `float`, `date` `yyyy-MM-dd` estricto, `nested` con sus
  subcampos, `dense_vector` con N números finitos); `id`, `title` y `plot` obligatorios; `id` no se
  repite; `_id` igual a `id`. Advertencias que no detienen: valores vacíos dentro de listas (se omiten),
  textos vacíos (se guardan sin valor). Cuenta cuántos `id` ya existen en el catálogo (reemplazarán).
  Informe completo exportable a CSV (registro, id, campo, recibido, esperado).
  - Criterio: pruebas con variantes inválidas de los fixtures (creadas en `src/test/resources`) producen
    exactamente los errores esperados; los fixtures válidos producen 0 errores.
- [ ] **3.5** Vectores: si un registro trae `plot_256`, `plot_512` y `plot_1024` válidos se usan tal
  cual; si no los trae, se generan desde `plot` con `EmbeddingService` (prefijo `Document: `), según la
  decisión de 3.0 para los casos parciales.
  - Criterio: prueba unitaria con `EmbeddingService` simulado verifica cuándo se llama y cuándo no.
- [ ] **3.6** `ImportJob`: crea `elastiflix-reloaded-<AAAAMMDD-HHMMSS>` con el mapping, copia el
  índice vigente (`_reindex`), escribe en lotes de 100 (`_id` = `id`, reemplaza si existe), revisa
  cada respuesta `bulk`, `refresh`, verifica el conteo esperado (anterior + nuevos), y hace el swap
  atómico del alias. Ante un lote rechazado, un conteo que no cuadra o una cancelación: borra el índice
  temporal y deja el alias intacto. Una sola importación a la vez. Publica el progreso por el listener
  (paso 1–5 del mockup, lote actual, registros escritos).
  - Criterio: pruebas unitarias con `CatalogIndex` simulado cubren éxito, lote rechazado, conteo que no
    cuadra, cancelación y segunda importación simultánea rechazada.
- [ ] **3.7** Registro individual: valida los campos del formulario (`id`, `title` y `plot`
  obligatorios) con el mismo `MappingValidator`, genera los vectores y lo guarda según la decisión de 3.0.
  - Criterio: prueba unitaria del flujo completo con dobles.
- [ ] **3.8** `IngestService` (fachada) y pruebas de integración: `IngestIT` crea un alias propio
  `elastiflix-it-<aleatorio>` con un índice copiado de un subconjunto pequeño, importa
  `movies-sample.json` (una película modificada para probar el reemplazo) y verifica conteo, reemplazo,
  swap y limpieza; otra prueba cancela a mitad y verifica que el alias no cambió y el índice temporal no
  existe.
  - Criterio: `./mvnw verify -Pit -pl elastiflix-ingest -am` en verde; **ninguna** prueba toca
    `elastiflix-reloaded`.

**Verificación de fase 3:**
```bash
./mvnw verify
./mvnw verify -Pit -pl elastiflix-ingest -am
```
Criterio: validación todo-o-nada demostrada; vuelta atrás demostrada; 0 violaciones; arquitectura en
verde; resumen de diseño y revisión de convenciones en `progreso/3.md`.

---

### Fase 4 — Interfaz web

**Objetivo:** las cuatro páginas de los mockups, servidas por Thymeleaf, cableadas a las fachadas.

- [ ] **4.0** Diseño: `docs/diseno/4.md` (plantillas y fragmentos, controladores, modelo de cada vista,
  JavaScript mínimo, CSS con los tokens de `design/README.md`). Commit antes del código. Lleva a
  "Dudas", para que el usuario decida, si las imágenes de `poster_path` se muestran desde un CDN externo
  o se deja el marcador del mockup.
- [ ] **4.1** Layout común: `templates/fragments/` (franja pastel, header con logo, navegación Buscar ·
  Cargar datos · Acerca de la demo y estado de conexión; pie con alias y conteo), CSS con los tokens y
  las fuentes Bricolage Grotesque, IBM Plex Sans e IBM Plex Mono. `CatalogHealthIndicator`
  (`elastiflixCatalog`) en Actuator.
  - Criterio: `GET /actuator/health` → `UP` con `alias`, `index` y `documents`; el header muestra
    «Conectado · Elasticsearch Serverless» y el pie «Índice: elastiflix-reloaded · 4 322 películas».
- [ ] **4.2** Inicio (`GET /`): caja de búsqueda con su validación, selector de modo y paneles
  (Full-text: tipo de consulta, campo solo con `match`, operador y textos de ayuda; Vectorial: campo de
  vector con sus dimensiones, `k` y `num_candidates` con sus validaciones; Híbrida: ambos y la
  explicación RRF), y las **sugerencias dinámicas por modo y ajustes** exactamente como en
  `Main.dc.html` (textos, emojis y etiquetas).
  - Criterio: pruebas MockMvc verifican los textos clave y que cada sugerencia lleva a `/buscar` con
    los parámetros de su modo.
- [ ] **4.3** Resultados (`GET /buscar`): caja de consulta, chip del modo y ajustes con «Cambiar
  ajustes», tarjetas de resultado (posición, póster, título, original_title · fecha · duración · géneros,
  sinopsis en 3 líneas, calificación y votos, panel de puntuación por modo), selector 25 · 50 · 100,
  paginación Anterior · números · Siguiente, filtros con conteos («Ver todos», «Tienes cambios sin
  aplicar», APLICAR, Limpiar, rango de fechas de los resultados), estados vacíos y leyenda por modo.
  Cambiar el tamaño o aplicar filtros vuelve a la página 1.
  - Criterio: pruebas MockMvc para los tres modos, los dos estados vacíos y un error de validación
    (`candidatos < k`) mostrado con su código.
- [ ] **4.4** Cargar datos (`GET /cargar` y endpoints de §5): pestañas, formulario completo del mockup,
  zona de arrastre con las comprobaciones del navegador, opciones de CSV, proceso en 5 pasos, tarjetas
  de conteo, tabla de errores (6 primeros + descarga del informe), comprobaciones superadas,
  advertencias, botón de importar, progreso y cancelación (consulta periódica del estado del trabajo).
  - Criterio: pruebas MockMvc del flujo validar → importar → estado → cancelar con `IngestService`
    simulado; un archivo `.txt` se rechaza con su código.
- [ ] **4.5** Acerca de (`GET /acerca`): contenido de `About.dc.html` (tres modos, BM25, k-NN, tabla de
  precisión por dimensión, RRF, resultados y filtros, carga de datos, catálogo) con alias y conteo
  reales y los enlaces a la documentación oficial.
  - Criterio: prueba MockMvc verifica las secciones y la tabla de precisión.
- [ ] **4.6** Errores: `@ControllerAdvice` muestra una página con el código `EFX-*` y el mensaje del
  catálogo; 404 propio con navegación.
  - Criterio: pruebas MockMvc para `ElastiflixException` y ruta inexistente.

**Verificación de fase 4:**
```bash
./mvnw verify
./mvnw -q install -DskipTests && ./mvnw -pl elastiflix-web spring-boot:run   # abrir http://localhost:8080
```
Criterio: las cuatro páginas reproducen los mockups (compara con `design/mockups/` y con el canvas de
diseño); el usuario confirma la fidelidad visual con capturas de las cuatro páginas; 0 violaciones;
arquitectura en verde; resumen de diseño y revisión de convenciones en `progreso/4.md`.

---

### Fase 5 — Pruebas integrales y smoke test

**Objetivo:** demostrar de punta a punta, contra el clúster real, que la aplicación cumple el plan.

- [ ] **5.0** Diseño: `docs/diseno/5.md` (alcance del smoke test, datos y limpieza). Commit antes del
  código.
- [ ] **5.1** `SmokeIT` en `web` (`@SpringBootTest(webEnvironment = RANDOM_PORT)`, perfil `it`):
  `GET /actuator/health` `UP` con 4 322 documentos; `GET /`, `/acerca`, `/cargar` responden 200; una
  búsqueda por modo con una sugerencia del mockup devuelve resultados; subir
  `elasticsearch/fixtures/movies-sample.csv` llega a «listo para importar» sin errores (no importa).
  - Criterio: `./mvnw verify -Pit` en verde.
- [ ] **5.2** Revisión de cobertura funcional: tabla en `progreso/5.md` que enlaza cada capacidad de §1
  y cada fila de datos de verificación de §6 con la prueba que la cubre.
  - Criterio: ninguna capacidad sin prueba.

**Verificación de fase 5:**
```bash
./mvnw verify -Pit
```
Criterio: todas las pruebas (unitarias, arquitectura, integración y smoke) en verde; 0 violaciones;
resumen de diseño y revisión de convenciones en `progreso/5.md`.

---

### Fase 6 — Documentación y cierre

**Objetivo:** dejar el proyecto listo para que cualquier persona lo ejecute y lo entienda.

- [ ] **6.1** Actualiza `README.md` con los comandos reales verificados (compilar, configurar `.env`,
  migrar a alias, arrancar, pruebas) y una captura de cada página en `docs/capturas/`.
  - Criterio: siguiendo solo el README en un clon limpio se compila y arranca la aplicación.
- [ ] **6.2** Recorre la [Definición de Hecho](#definición-de-hecho-global) y marca cada punto con su
  evidencia en `progreso/6.md`.
  - Criterio: todos los puntos marcados.
- [ ] **6.3** Cierra `progreso/INDICE.md` (todas las fases `terminado`) y pide al usuario la revisión
  final.
  - Criterio: `git status` limpio y todo en `main`.

**Verificación de fase 6:**
```bash
./mvnw verify -Pit
git status
```
Criterio: Definición de Hecho completa.

---

## Definición de Hecho (global)

- [ ] Los tres modos de búsqueda, los filtros, la paginación y la carga de datos funcionan como describe
  §1 y los mockups.
- [ ] Todos los datos de verificación de §6 pasan en las pruebas de integración.
- [ ] `elastiflix-reloaded` es un alias; una carga publica con swap atómico y una falla deja el catálogo
  intacto (demostrado en pruebas).
- [ ] `./mvnw verify -Pit` en verde: unitarias, arquitectura, integración y smoke.
- [ ] 0 violaciones de Checkstyle y PMD; ArchUnit en verde; supresiones justificadas y registradas.
- [ ] Cada fase con código tiene su diseño (aprobado en las fases 🔒), su resumen de diseño y su revisión
  de convenciones.
- [ ] Ningún secreto en el repositorio (`git grep -i -E "api[_-]?key\s*[:=]\s*[A-Za-z0-9]{12,}"` vacío).
- [ ] `README.md` actualizado con los comandos reales.
- [ ] Todos los archivos de `progreso/` en `terminado`, sin cambios sin commit, e `INDICE.md` con todas
  las fases cerradas.

## Variables de entorno (`.env.example`)

```dotenv
# Elasticsearch Serverless (obligatorias; el usuario las provee)
ELASTICSEARCH_URL=
ELASTICSEARCH_API_KEY=

# Catálogo y modelo
ELASTIFLIX_INDEX_ALIAS=elastiflix-reloaded
ELASTIFLIX_INFERENCE_ID=.jina-embeddings-v5-omni-small

# Carga de datos
ELASTIFLIX_INGEST_MAX_FILE_SIZE=150MB
ELASTIFLIX_INGEST_BATCH_SIZE=100

# Servidor web
SERVER_PORT=8080
```

## Referencias

- Mockups aprobados: `design/mockups/` y el canvas de diseño (enlace en `design/README.md`).
- Elasticsearch: [match](https://www.elastic.co/docs/reference/query-languages/query-dsl/query-dsl-match-query) ·
  [multi_match](https://www.elastic.co/docs/reference/query-languages/query-dsl/query-dsl-multi-match-query) ·
  [kNN retriever](https://www.elastic.co/docs/reference/elasticsearch/rest-apis/retrievers/knn-retriever) ·
  [RRF retriever](https://www.elastic.co/docs/reference/elasticsearch/rest-apis/retrievers/rrf-retriever) ·
  [filtered kNN](https://www.elastic.co/docs/solutions/search/vector/knn/filtered-knn-search) ·
  [dense_vector](https://www.elastic.co/docs/reference/elasticsearch/mapping-reference/dense-vector) ·
  [alias](https://www.elastic.co/docs/manage-data/data-store/aliases) ·
  [paginación](https://www.elastic.co/docs/reference/elasticsearch/rest-apis/paginate-search-results)
- Modelo: [jina-embeddings-v5-omni-small](https://huggingface.co/jinaai/jina-embeddings-v5-omni-small)
