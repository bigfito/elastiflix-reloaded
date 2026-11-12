# 📐 Convenciones de código Java — Elastiflix-Movies Java

Reglas de estilo, diseño y calidad del código de Elastiflix-Movies Java. `./mvnw verify` comprueba
automáticamente buena parte de ellas (ver §14); el resto es criterio para mantener el código claro. Las
reglas del dominio (búsqueda, carga, Elasticsearch) salen del mapping
`elasticsearch/elastiflix-reloaded-mapping.json` y de §11.

## Índice

1. [Principios de trabajo](#1-principios-de-trabajo)
2. [Organización del código](#2-organización-del-código)
3. [Java 25 con mesura](#3-java-25-con-mesura)
4. [Diseño: SOLID y patrones permitidos](#4-diseño-solid-y-patrones-permitidos)
5. [Estilo y nombres](#5-estilo-y-nombres)
6. [Spring Boot](#6-spring-boot)
7. [Errores](#7-errores)
8. [Logging](#8-logging)
9. [Documentación en código](#9-documentación-en-código)
10. [Pruebas](#10-pruebas)
11. [Reglas de dominio que afectan el código](#11-reglas-de-dominio-que-afectan-el-código)
12. [Build y dependencias](#12-build-y-dependencias)
13. [Ejemplos de referencia](#13-ejemplos-de-referencia)
14. [Qué se verifica automáticamente](#14-qué-se-verifica-automáticamente)
15. [Decisiones de estilo](#15-decisiones-de-estilo)
16. [Lista de revisión de un cambio](#16-lista-de-revisión-de-un-cambio)

---

## 1. Principios de trabajo

Java 25 LTS, Spring Boot y Maven, con estos principios:

1. **Claridad antes que ingenio.** Un código legible vale más que uno "elegante" pero críptico.
2. **SOLID y patrones al servicio del problema**, nunca al revés.
3. **Errores que hablan:** toda falla explica qué pasó, la causa probable y qué hacer.
4. **Todo se registra:** una aplicación sin logging es una caja negra.
5. **Documentar es parte de programar.**
6. **Si un programador Junior no lo entiende, se puede escribir mejor.**
7. **Lo que no está definido no se inventa:** se decide y se documenta.

## 2. Organización del código

Un solo módulo de Maven. El código vive en paquetes de `com.bigfito.elastiflix`; las pruebas, en los mismos
paquetes de `src/test/java`.

| Paquete | Responsabilidad | Puede usar |
|---|---|---|
| `com.bigfito.elastiflix` | `ElastiflixApplication`, el punto de entrada | todo |
| `core` | Modelo, cliente de Elasticsearch, catálogo (alias e índices), mapping, embeddings, errores | Spring sin web |
| `search` | Full-text, vectorial e híbrida; filtros, facetas y paginación | `core` |
| `ingest` | Comprobación de archivos, lectores JSON/CSV, validación contra el mapping, importación | `core` |
| `web` | Controladores Spring MVC, plantillas Thymeleaf, textos en español e inglés | `core`, `search`, `ingest` |

`ArchitectureTest` (ArchUnit) hace cumplir la columna «Puede usar»: `core` no conoce a los demás, `search` e
`ingest` no se conocen entre sí, Spring MVC y Servlet solo aparecen en `web`, y `web` no usa el cliente de
Elasticsearch.

## 3. Java 25 con mesura

| Característica | Úsala cuando… | Evítala cuando… |
|---|---|---|
| `record` | Datos inmutables: DTO, ajustes de búsqueda, resultados, errores de validación | La clase tiene comportamiento con estado mutable |
| `sealed` + `switch` con patrones | Jerarquías cerradas: modos de búsqueda, resultados de validación, eventos de importación | La jerarquía debe ser extensible por terceros |
| `var` | El tipo es evidente a la derecha (`var page = new PageRequest(…)`) | Oculta el tipo (`var x = service.run()`) |
| Bloques de texto `"""` | Plantillas largas en pruebas (JSON de ejemplo) | Textos cortos |
| Hilos virtuales | Tareas de E/S: importación en segundo plano, llamadas a Elasticsearch | Cálculo intensivo de CPU |
| `Stream` | Transformaciones simples y legibles (≤ 4 operaciones encadenadas) | Lógica con efectos secundarios o ramas complejas: usa un `for` |
| `Optional` | Valor de retorno que puede faltar | Campos, parámetros o colecciones |
| Características en *preview* | **Nunca** | — |

## 4. Diseño: SOLID y patrones permitidos

**SOLID aplicado al proyecto:**

- **S** — Una clase, una razón para cambiar: `CsvMovieReader` solo lee CSV; `MappingValidator` solo
  valida contra el mapping; `MovieReaderFactory` solo elige el lector.
- **O** — Agregar un modo de búsqueda = agregar una estrategia, sin tocar las existentes.
- **L** — Toda implementación de `SearchStrategy` se puede usar donde se espera la interfaz.
- **I** — Interfaces pequeñas: `EmbeddingService` no expone métodos de índice.
- **D** — `search` e `ingest` dependen de interfaces de `core` (`CatalogIndex`, `EmbeddingService`),
  no de detalles del cliente.

**Lista cerrada de patrones** (otro patrón necesita una razón clara):

| Patrón | Dónde | Por qué |
|---|---|---|
| Strategy | paquete `search`: una estrategia por modo (full-text, vectorial, híbrida) | Cada modo construye su petición de forma distinta; agregar uno no toca los demás |
| Facade | `SearchService` e `IngestService` | La capa web ve una sola entrada por caso de uso |
| Factory (método estático o clase pequeña) | Elegir lector por tipo de archivo (`.json` / `.csv`) | Oculta qué lector se usa |
| Gateway / Adapter | `core`: envoltura del cliente de Elasticsearch y del endpoint de inferencia | Aísla el cliente; facilita las pruebas con dobles |
| Observer (listener simple) | Progreso de la importación hacia la página de carga | Desacopla el trabajo en segundo plano de la interfaz |
| Builder | Solo el del propio cliente de Elasticsearch; en clases propias prefiere `record` o métodos de fábrica | Evita boilerplate |

**Prohibido:** Singleton manual, herencia de más de 2 niveles en clases propias, clases `*Utils`
genéricas (usa clases con nombre de responsabilidad), Visitor, reflexión.

## 5. Estilo y nombres

**Idioma:** identificadores en **inglés** (coinciden con los campos del índice: `release_date`,
`spoken_languages`); Javadoc, comentarios y mensajes de log en **español**. Los textos de la interfaz viven en
`messages_es.properties` y `messages_en.properties` (español e inglés, requerimiento 2), no en el código.

**Umbrales** (verificados automáticamente, ver §14):

| Medida | Límite |
|---|---|
| Líneas por método (sin vacías) | 30 |
| Sentencias por método (NCSS) | 25 |
| Parámetros por método | 5 |
| Complejidad ciclomática por método | 10 |
| Complejidad cognitiva por método | 15 |
| Anidamiento de `if` | 2 niveles |
| Anidamiento de `for` / `try` | 1 nivel |
| `return` por método | 3 (2 en `void`) |
| Líneas por archivo | 400 |
| Métodos por clase | 20 |
| Longitud de línea | 120 |
| Componentes por `record` | 35 (solo el documento de película se acerca) |

**Nombres:**

| Elemento | Convención | Ejemplo |
|---|---|---|
| Paquete | `com.bigfito.elastiflix.<parte>.<área>` (`core`, `search`, `ingest` o `web`) | `com.bigfito.elastiflix.search.facet` |
| Clase | Sustantivo en PascalCase | `HybridSearchStrategy`, `MappingValidator` |
| Interfaz | Capacidad, sin prefijo `I` | `EmbeddingService`, `SearchStrategy` |
| Implementación | Calificador + interfaz | `InferenceEmbeddingService` |
| Método | Verbo en camelCase | `buildRequest`, `validateRecord` |
| Booleano | `is`/`has`/`can` | `isAdult`, `hasValidVectors` |
| Constante | MAYÚSCULAS | `MAX_NUM_CANDIDATES` |
| Excepción | Termina en `Exception` | `IngestValidationException` |
| Prueba | Clase + `Test` / `IT` | `KnnSettingsTest`, `FullTextSearchIT` |

Abreviaturas permitidas en nombres: `RRF`, `BM25`, `KNN`, `CSV`, `JSON`, `NDJSON`, `UTF`, `API`,
`URL`, `HTTP`, `IMDB`.

## 6. Spring Boot

- **Inyección solo por constructor.** Nada de `@Autowired` en campos (regla `JM-SPR-01`).
- **Configuración tipada:** `@ConfigurationProperties` sobre `record` con `@Validated`; prohibido
  `@Value` disperso. Prefijo `elastiflix.*` para lo propio y `spring.*` para lo de Boot.
- **Secretos** solo por variables de entorno o `.env` (importado con `spring.config.import`); nunca en
  `application.yml` ni en el código (regla `JM-SEC-01`).
- **Dónde va Spring:** `core`, `search` e `ingest` pueden usar `spring-context` (`@Service`,
  `@Configuration`) pero **no** Spring MVC, Servlet ni Thymeleaf; eso solo vive en el paquete `web`. El proyecto es
  un solo módulo de Maven, así que lo impide `ArchitectureTest`, no el classpath.
- **Controladores delgados:** validan la entrada, llaman a un servicio y eligen la vista. Sin lógica
  de búsqueda ni de ingesta.
- **Manejo global de errores:** un `@ControllerAdvice` traduce `ElastiflixException` a una página o
  respuesta con el código y el mensaje del catálogo.
- **Hilos virtuales:** `spring.threads.virtual.enabled=true`.
- **Actuator:** `health` expuesto, con un indicador propio de Elasticsearch que alimenta el
  indicador «Conectado · Elasticsearch Serverless» del header.
- **Starters de Spring Boot 4:** `spring-boot-starter-webmvc` (no `-web`), `spring-boot-starter-thymeleaf`,
  `spring-boot-starter-actuator`, `spring-boot-starter-validation`, `spring-boot-starter-elasticsearch`;
  para MockMvc, `spring-boot-starter-webmvc-test`. `HealthIndicator` está en
  `org.springframework.boot.health.contributor`.
- **Clase principal:** vive en el paquete raíz para que Spring escanee las cuatro partes, y no es una clase de
  utilidades (Checkstyle la exime por `@SpringBootApplication`). Pasa la verificación automática tal cual:

```java
package com.bigfito.elastiflix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada de Elastiflix-Movies Java.
 *
 * <p>Está en el paquete raíz para que Spring escanee sus cuatro partes: {@code core}, {@code search},
 * {@code ingest} y {@code web}.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ElastiflixApplication {

    /**
     * Arranca la aplicación.
     *
     * @param args argumentos de la línea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(ElastiflixApplication.class, args);
    }
}
```

## 7. Errores

- Jerarquía: `ElastiflixException` (base, `RuntimeException`, siempre con `ErrorCode`) →
  `ConfigurationException`, `ElasticsearchAccessException`, `EmbeddingException`,
  `SearchValidationException`, `IngestValidationException`, `ImportException`.
- **Códigos** con forma `EFX-<ÁREA>-<NNN>`:

| Área | Prefijo | Ejemplos |
|---|---|---|
| Configuración | `EFX-CFG` | falta `ELASTICSEARCH_URL` |
| Conexión e índice | `EFX-ES` | sin conexión, alias inexistente |
| Embeddings | `EFX-EMB` | endpoint de inferencia no disponible, dimensión inesperada |
| Búsqueda | `EFX-SRCH` | `num_candidates < k`, full-text con `from ≥ 10 000`, consulta vacía |
| Validación de archivo | `EFX-ING` | extensión no permitida, más de 25 MB, no es UTF-8, no coincide con el mapping, sinopsis marcador o corta (`EFX-ING-025`); un cero desconocido, un repetido en una lista, restos de un modelo o una sinopsis repetida en el archivo son advertencias (`EFX-ING-023`, `024`, `026`, `027`) |
| Importación | `EFX-IMP` | lote rechazado, conteo no cuadra, alias no cambiado |

- **Formato del mensaje:** `<código> · <qué pasó>. Causa probable: <…>. Acción sugerida: <…>.`
- **Nunca** silencies excepciones ni dejes un `catch` vacío; al envolver, conserva la causa.
- No captures `Exception`, `RuntimeException` ni `Throwable` (regla `JM-ERR-01`); el
  `@ControllerAdvice` usa `@ExceptionHandler`, no `catch`.
- Los errores de validación de la ingesta **no son excepciones por registro**: se acumulan como
  `record ValidationIssue(String code, String recordLabel, String id, String field, String received,
  String expected, boolean warning)`
  y se lanzan juntos al final.

## 8. Logging

SLF4J con un `private static final Logger LOG` por clase.

| Nivel | Cuándo | Ejemplo |
|---|---|---|
| `ERROR` | Falla que el usuario ve, con su código | `EFX-IMP-002 · Elasticsearch rechazó el lote 7 de 44; se borró el índice temporal` |
| `WARN` | Situación anómala recuperable | `Ingesta con 62 advertencias (valores vacíos omitidos en spoken_languages)` |
| `INFO` | Eventos de negocio | `Búsqueda híbrida: 100 resultados en 182 ms`; `Alias elastiflix-reloaded → elastiflix-reloaded-20261020-153000` |
| `DEBUG` | Detalle para diagnosticar | Texto de la consulta y ajustes elegidos |
| `TRACE` | Peticiones a Elasticsearch | JSON de la petición **sin** vectores |

- Contexto obligatorio: modo de búsqueda; en importaciones, `jobId` en el MDC, archivo, lote y
  conteos.
- **Prohibido registrar:** API key, cabecera `Authorization`, vectores completos, documentos
  completos, contenido de archivos subidos.
- Mensajes parametrizados (`LOG.info("… {}", valor)`), nunca concatenados.
- En `TRACE` y `DEBUG`, si un argumento es una llamada a método o una expresión, envuelve el log en
  `if (LOG.isDebugEnabled())` / `isTraceEnabled()` (regla PMD `GuardLogStatement`). En `INFO`, `WARN` y
  `ERROR` no hace falta.

## 9. Documentación en código

- Javadoc en **toda clase y método público, incluidos los de una línea** (verificado), con primera
  frase terminada en punto,
  `@param`, `@return` y `@throws` cuando aplique.
- Comenta el **por qué**, no el qué. Ejemplo: por qué las facetas de Vectorial e Híbrida se
  calculan en la aplicación y no con agregaciones.
- `package-info.java` en cada paquete con su responsabilidad en una frase.

## 10. Pruebas

- JUnit Jupiter (JUnit 6 en Spring Boot 4), AssertJ y Mockito (de `spring-boot-starter-test`).
- **Nombres:** método en inglés `should<Resultado>When<Condición>` y `@DisplayName` en español.
- Estructura con comentarios `// given`, `// when`, `// then`.
- **Unitarias** (`*Test`, Surefire): sin red ni Elasticsearch; el cliente y la inferencia se sustituyen
  por dobles.
- **Integración** (`*IT`, Failsafe, **perfil de Maven** `it`: `./mvnw verify -Pit`): contra el proyecto
  Elasticsearch Serverless configurado en `.env`. Todas usan `ElastiflixApplication` (`@SpringBootTest`) y el
  `application.yml` de la aplicación, que importa `optional:file:.env[.properties]` desde la raíz del repo, donde
  trabajan Maven, Surefire y Failsafe.
  - Las unitarias no necesitan `.env` ni red: JUnit puro o `ApplicationContextRunner`; las de web con
    contexto (`@WebMvcTest`) pasan URL y API key ficticias en `properties` y simulan las fachadas.
  - Los fixtures se leen desde `elasticsearch/fixtures/`, relativo a la raíz del repo.
  - Las de **lectura** usan el alias `elastiflix-reloaded` y no lo modifican.
  - Las de **escritura** crean su propio alias `elastiflix-it-<aleatorio>` y lo borran al terminar,
    pase o falle la prueba. **Nunca** escriben en `elastiflix-reloaded`.
- **Prohibido:** `Thread.sleep` (usa espera con condición), pruebas que dependan del orden,
  `@Disabled` sin motivo, aserciones vacías, datos reales de credenciales en recursos de prueba.
- Las pruebas de arquitectura (ArchUnit) viven en `src/test/java/com/bigfito/elastiflix/architecture/`
  y corren en `./mvnw verify`.

## 11. Reglas de dominio que afectan el código

- **Tiempo:** `java.time` y UTC (`Instant`, `Clock` inyectable). `release_date` es `LocalDate` con
  formato `yyyy-MM-dd`. Prohibidos `Date`, `Calendar`, `SimpleDateFormat` (regla `JM-TIME-01`).
- **Vectores:** `float[]`; 512 y 256 son los primeros N valores del vector de 1024, **renormalizados
  (L2)**. Consultas: `input_type: search` y prefijo `Query: `. Documentos: el `input_type` y el prefijo
  calibrados contra los vectores del catálogo, que viven **solo** como valores por defecto (`@DefaultValue`) del `record`
  de propiedades `elastiflix.embedding.*`. En las cargas, los tres vectores **siempre** se generan con el
  servicio de inferencia; los archivos no los necesitan (si los traen, se ignoran con advertencia) y el
  formulario no los captura. Para leer vectores guardados, pide `"_source": {"exclude_vectors": false}`
  (Serverless los excluye de `_source` por defecto).
- **Límites:** `k ≥ 1`, `k ≤ num_candidates ≤ 10 000`; full-text `from < 10 000` (la última página se recorta a la ventana)
  (`index.max_result_window`); híbrida `rank_window_size = k` y `rank_constant = 60`.
- **Puntuaciones:** BM25 tal cual; k-NN `(1 + coseno) / 2`; RRF `Σ 1 / (60 + posición)`.
- **Alias e índices:** el nombre `elastiflix-reloaded` es configurable (`elastiflix.index.alias`); el
  código nunca lo escribe como literal fuera de la configuración por defecto. Los índices se nombran
  `<alias>-<AAAAMMDD-HHMMSS>` (UTC) y la limpieza de huérfanos solo toca los de su propio alias.
- **Un solo usuario:** la demo la usa una persona a la vez (fines académicos y demostrativos); no se
  agregan bloqueos ni controles de concurrencia entre búsquedas, registros e importaciones. El estado
  del trabajo de importación es inmutable (`record`) y se publica por el listener de progreso.
- **Publicación:** `add` del alias al índice nuevo y `remove_index` del anterior en una sola llamada a
  `_aliases`; nunca en dos llamadas.
- **Texto de interfaz** en `messages_es.properties` y `messages_en.properties`, igual al de los mockups de
  `design/mockups/` (en inglés, los `-en`). Los logs siguen en español.

## 12. Build y dependencias

- Maven **solo mediante el wrapper** (`./mvnw`; en Windows, desde Git Bash).
- Versiones en `pom.xml`: las gestiona Spring Boot o se declaran en `<properties>`.
- **JSON con Jackson 3** (`tools.jackson.*`). Prohibido importar `com.fasterxml.jackson.databind`
  (regla `JM-DEP-01`); las anotaciones `com.fasterxml.jackson.annotation` sí están permitidas porque
  Jackson 3 las conserva.
- Sin Spring Data Elasticsearch, sin Lombok y sin librerías de CSV/JSON adicionales.

## 13. Ejemplos de referencia

Estos ejemplos pasan Checkstyle 14.3.0 y PMD 7.28.0 con las configuraciones de `config/`
(verificado al redactar este documento).

**Catálogo de códigos de error**

El enum real, con todos los códigos, está en `ErrorCode.java`. Este fragmento solo enseña la forma
y copia cuatro textos vigentes. No es la lista completa.

```java
package com.bigfito.elastiflix.core.error;

/**
 * Catálogo de códigos de error de Elastiflix.
 *
 * <p>Cada código explica qué pasó, la causa probable y la acción sugerida, para que el usuario
 * final sepa cómo resolver o reportar la falla.</p>
 */
public enum ErrorCode {

    ELASTICSEARCH_UNAVAILABLE("EFX-ES-001",
            "No se pudo conectar con Elasticsearch",
            "La URL o la API key no son válidas, o el proyecto Serverless no responde",
            "Revisa ELASTICSEARCH_URL y ELASTICSEARCH_API_KEY en .env y vuelve a intentarlo"),

    K_TOO_SMALL("EFX-SRCH-002",
            "k debe ser un entero mayor o igual que 1",
            "k es menor que 1",
            "Elige un entero a partir de 1"),

    CANDIDATES_BELOW_K("EFX-SRCH-003",
            "num_candidates debe ser mayor o igual que k",
            "Hay menos candidatos que resultados pedidos",
            "Sube num_candidates o baja k"),

    CANDIDATES_TOO_LARGE("EFX-SRCH-004",
            "num_candidates debe ser un entero de máximo 10 000",
            "num_candidates pasa de 10 000",
            "Elige un entero entre k y 10 000");

    private final String code;
    private final String whatHappened;
    private final String probableCause;
    private final String suggestedAction;

    ErrorCode(String code, String whatHappened, String probableCause, String suggestedAction) {
        this.code = code;
        this.whatHappened = whatHappened;
        this.probableCause = probableCause;
        this.suggestedAction = suggestedAction;
    }

    /**
     * Devuelve el código visible para el usuario, por ejemplo {@code EFX-ES-001}.
     *
     * @return el código del error
     */
    public String code() {
        return code;
    }

    /**
     * Compone el mensaje completo en el formato del proyecto.
     *
     * @return mensaje con qué pasó, causa probable y acción sugerida
     */
    public String userMessage() {
        return "%s · %s. Causa probable: %s. Acción sugerida: %s."
                .formatted(code, whatHappened, probableCause, suggestedAction);
    }
}
```

**Excepción base**

```java
package com.bigfito.elastiflix.core.error;

import java.io.Serial;

/**
 * Excepción base de Elastiflix. Siempre lleva un {@link ErrorCode}.
 */
public class ElastiflixException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    /**
     * Crea la excepción con su código y la causa técnica original.
     *
     * @param errorCode código del catálogo
     * @param cause     excepción original; nunca se descarta
     */
    public ElastiflixException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.userMessage(), cause);
        this.errorCode = errorCode;
    }

    /**
     * Crea la excepción con su código, sin causa técnica.
     *
     * @param errorCode código del catálogo
     */
    public ElastiflixException(ErrorCode errorCode) {
        super(errorCode.userMessage());
        this.errorCode = errorCode;
    }

    /**
     * Devuelve el código de error asociado.
     *
     * @return el código de error
     */
    public ErrorCode errorCode() {
        return errorCode;
    }
}
```

**`record` con validación en el constructor compacto**

```java
package com.bigfito.elastiflix.search.query;

import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;

/**
 * Ajustes del componente vectorial (k-NN) elegidos por el usuario.
 *
 * @param vectorField   campo de vector sobre el que se busca
 * @param k             número máximo de resultados
 * @param numCandidates candidatos evaluados por shard
 */
public record KnnSettings(VectorField vectorField, int k, int numCandidates) {

    /** Máximo de candidatos que admite Elasticsearch. */
    public static final int MAX_NUM_CANDIDATES = 10_000;

    /**
     * Valida los ajustes al construir el registro.
     *
     * @throws ElastiflixException si k o num_candidates están fuera de rango
     */
    public KnnSettings {
        if (k < 1) {
            throw new ElastiflixException(ErrorCode.KNN_K_BELOW_ONE);
        }
        if (numCandidates > MAX_NUM_CANDIDATES) {
            throw new ElastiflixException(ErrorCode.KNN_CANDIDATES_ABOVE_MAX);
        }
        if (numCandidates < k) {
            throw new ElastiflixException(ErrorCode.KNN_CANDIDATES_BELOW_K);
        }
    }

    /**
     * Describe los ajustes para la cabecera de resultados.
     *
     * @return texto legible, por ejemplo {@code plot_512 · k 100 · num_candidates 200}
     */
    public String describe() {
        return "%s · k %d · num_candidates %d".formatted(vectorField.fieldName(), k, numCandidates);
    }
}
```

**Jerarquía sellada + `switch` con patrones + logging**

```java
package com.bigfito.elastiflix.search.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Describe en texto los ajustes de una búsqueda, según su modo.
 *
 * <p>Usa {@code switch} con patrones sobre la jerarquía sellada {@link SearchMode}: el compilador
 * avisa si se agrega un modo y no se describe aquí.</p>
 */
@Component
public class SearchRequestDescriber {

    private static final Logger LOG = LoggerFactory.getLogger(SearchRequestDescriber.class);

    /**
     * Devuelve la descripción de los ajustes que se muestra sobre los resultados.
     *
     * @param mode modo de búsqueda con sus ajustes
     * @return descripción legible para la interfaz
     */
    public String describe(SearchMode mode) {
        String description = switch (mode) {
            case SearchMode.FullText fullText -> "Full-text · " + fullText.describe();
            case SearchMode.Vector vector -> "Vectorial · " + vector.knn().describe();
            case SearchMode.Hybrid hybrid -> "Híbrida · " + hybrid.fullText().describe()
                    + " + " + hybrid.knn().describe();
        };
        LOG.debug("Ajustes de búsqueda descritos: {}", description);
        return description;
    }

    /**
     * Modos de búsqueda soportados por la demo.
     */
    public sealed interface SearchMode {

        /**
         * Búsqueda léxica BM25.
         *
         * @param queryType tipo de consulta
         * @param operator  operador entre palabras
         */
        record FullText(String queryType, String operator) implements SearchMode {

            /**
             * Describe los ajustes léxicos.
             *
             * @return texto legible
             */
            public String describe() {
                return queryType + " · " + operator;
            }
        }

        /**
         * Búsqueda vectorial k-NN.
         *
         * @param knn ajustes del componente vectorial
         */
        record Vector(KnnSettings knn) implements SearchMode {
        }

        /**
         * Búsqueda híbrida con fusión RRF.
         *
         * @param fullText componente léxico
         * @param knn      componente vectorial
         */
        record Hybrid(FullText fullText, KnnSettings knn) implements SearchMode {
        }
    }
}
```

**Prueba unitaria**

```java
@DisplayName("KnnSettings")
class KnnSettingsTest {

    @Test
    @DisplayName("rechaza num_candidates menor que k con el código EFX-SRCH-003")
    void shouldRejectWhenNumCandidatesBelowK() {
        // when
        var thrown = catchThrowableOfType(ElastiflixException.class,
                () -> new KnnSettings(VectorField.PLOT_512, 100, 50));

        // then
        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.KNN_CANDIDATES_BELOW_K);
    }
}
```

## 14. Qué se verifica automáticamente

`./mvnw verify` ejecuta todo lo siguiente sobre `src/main/java` y falla ante la primera
violación.

| Regla | Herramienta | Umbral o patrón |
|---|---|---|
| Longitud de método | Checkstyle `MethodLength` | 30 líneas |
| Parámetros | Checkstyle `ParameterNumber` / PMD `ExcessiveParameterList` | 5 |
| Complejidad ciclomática | Checkstyle `CyclomaticComplexity` / PMD | 10 por método |
| Complejidad cognitiva | PMD `CognitiveComplexity` | 15 |
| Sentencias por método / clase | PMD `NcssCount` | 25 / 250 |
| Anidamiento | Checkstyle `NestedIfDepth`, `NestedForDepth`, `NestedTryDepth` | 2 / 1 / 1 |
| Tamaño de archivo y línea | Checkstyle `FileLength`, `LineLength` | 400 / 120 |
| Clases dios / demasiados métodos | PMD `GodClass`, `TooManyMethods` | 20 métodos |
| Javadoc en API pública | Checkstyle `MissingJavadocType`, `MissingJavadocMethod`, `JavadocMethod`, `SummaryJavadoc` | Obligatorio |
| `catch` vacío o genérico | Checkstyle `EmptyCatchBlock`, `IllegalCatch` (`JM-ERR-01`) / PMD | Prohibido |
| Consola y `printStackTrace` | Checkstyle `JM-LOG-01` / PMD `SystemPrintln`, `AvoidPrintStackTrace` | Prohibido |
| Inyección por campo | Checkstyle `JM-SPR-01` | Prohibido |
| `Date`, `Calendar`, `SimpleDateFormat` | Checkstyle `JM-TIME-01` | Prohibido |
| Secretos literales | Checkstyle `JM-SEC-01` | Prohibido |
| Jackson 2 databind | Checkstyle `JM-DEP-01` | Prohibido |
| Cliente de Elasticsearch en web | PMD `JM-DEP-02` + ArchUnit | Prohibido |
| Matriz de dependencias | ArchUnit | §2 |

**Falso positivo conocido:** `JM-SEC-01` marca constantes como `"ELASTICSEARCH_API_KEY"` asignadas a un
nombre que contiene `api_key`. Evítalo: los nombres de variables de entorno solo aparecen en
`application.yml`, nunca en el código.

**Supresiones:** solo con `@SuppressWarnings("checkstyle:<Regla>")` o `@SuppressWarnings("PMD.<Regla>")`
en el elemento más pequeño posible y con un comentario que explique el motivo.

## 15. Decisiones de estilo

Decisiones deliberadas que pueden parecer poco habituales.

| Desviación | Motivo |
|---|---|
| Sin Spring Data Elasticsearch | El proyecto usa directamente el cliente Java de Elasticsearch 9.x para controlar retrievers (`knn`, `rrf`), alias e inferencia |
| `record` de hasta 35 componentes | El documento de película refleja los 34 campos del mapping |
| Identificadores en inglés, textos en español | Coinciden con los nombres de campo del índice; Javadoc y logs en español. La interfaz, en español e inglés, toma sus textos de `messages_es`/`messages_en` |
| Validación de ingesta sin excepciones por registro | Se deben reportar **todos** los errores del archivo de una vez |
| JavaScript mínimo en las páginas Thymeleaf | La interactividad de los mockups (selectores, progreso de importación) no justifica un framework de frontend |

## 16. Lista de revisión de un cambio

- [ ] ¿`./mvnw verify` termina con 0 violaciones y las pruebas de arquitectura en verde?
- [ ] ¿Cada clase tiene una sola responsabilidad que puedes decir en una frase?
- [ ] ¿Solo se usaron patrones de la lista cerrada (§4)?
- [ ] ¿Cada error nuevo tiene código `EFX-…` con qué pasó, causa probable y acción sugerida, en los dos idiomas?
- [ ] ¿Ningún log contiene secretos, vectores completos ni documentos completos?
- [ ] ¿Toda supresión está justificada en el código?
- [ ] ¿Las pruebas de integración de escritura usan su propio alias y lo borran al terminar?
- [ ] ¿Un programador Junior entendería las clases principales sin explicación adicional?
