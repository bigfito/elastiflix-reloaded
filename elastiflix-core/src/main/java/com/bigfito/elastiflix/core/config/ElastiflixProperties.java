package com.bigfito.elastiflix.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración de la demo bajo el prefijo {@code elastiflix}.
 *
 * <p>Los grupos que no vienen en el entorno usan el mismo valor por defecto que {@code application.yml}.
 * Los de embeddings no están en el YAML: {@link DefaultValue} crea el grupo con los valores calibrados.</p>
 *
 * @param elasticsearch URL y API key del proyecto Serverless
 * @param index         nombre del alias del catálogo
 * @param inference     id del endpoint de embeddings
 * @param ingest        tamaño de archivo y de lote
 * @param poster        prefijo del CDN de pósters
 * @param bootstrap     si la migración a alias debe ejecutarse
 * @param embedding     cómo se vectorizan las sinopsis
 */
@ConfigurationProperties(prefix = "elastiflix")
@Validated
public record ElastiflixProperties(
        Elasticsearch elasticsearch,
        Index index,
        Inference inference,
        Ingest ingest,
        Poster poster,
        Bootstrap bootstrap,
        @DefaultValue Embedding embedding) {

    /**
     * Rellena los grupos ausentes para que el contexto de prueba de core también arranque.
     */
    public ElastiflixProperties {
        elasticsearch = elasticsearch == null ? new Elasticsearch("", "") : elasticsearch;
        index = index == null ? Index.fallback() : index;
        inference = inference == null ? Inference.fallback() : inference;
        ingest = ingest == null ? Ingest.fallback() : ingest;
        poster = poster == null ? Poster.fallback() : poster;
        bootstrap = bootstrap == null ? Bootstrap.fallback() : bootstrap;
        embedding = embedding == null ? new Embedding(null, null) : embedding;
    }

    /**
     * Dirección del proyecto y su API key. Pueden llegar vacías: el arranque las rechaza antes.
     *
     * @param url    valor de {@code ELASTICSEARCH_URL}
     * @param apiKey valor de {@code ELASTICSEARCH_API_KEY}
     */
    public record Elasticsearch(String url, String apiKey) {

        /**
         * No incluye la API key. Un log de esta propiedad solo ve la máscara.
         *
         * @return la URL y {@code ****} en lugar de la clave
         */
        @Override
        public String toString() {
            return "Elasticsearch[url=" + url + ", apiKey=****]";
        }
    }

    /**
     * Nombre lógico del catálogo.
     *
     * @param alias alias o, antes de la migración, índice concreto
     */
    public record Index(String alias) {

        private static final String DEFAULT_ALIAS = "elastiflix-reloaded";

        /**
         * Usa el alias de la demo cuando la propiedad no viene.
         */
        public Index {
            if (alias == null || alias.isBlank()) {
                alias = DEFAULT_ALIAS;
            }
        }

        /**
         * Alias por defecto de la demo.
         *
         * @return el alias {@code elastiflix-reloaded}
         */
        public static Index fallback() {
            return new Index(DEFAULT_ALIAS);
        }
    }

    /**
     * Endpoint de inferencia de Elasticsearch.
     *
     * @param id id del modelo, por ejemplo {@code .jina-embeddings-v5-omni-small}
     */
    public record Inference(String id) {

        private static final String DEFAULT_ID = ".jina-embeddings-v5-omni-small";

        /**
         * Usa el modelo de la demo cuando la propiedad no viene.
         */
        public Inference {
            if (id == null || id.isBlank()) {
                id = DEFAULT_ID;
            }
        }

        /**
         * Id de inferencia por defecto.
         *
         * @return el endpoint de la demo
         */
        public static Inference fallback() {
            return new Inference(DEFAULT_ID);
        }
    }

    /**
     * Límites de una carga de archivo.
     *
     * @param maxFileSize tamaño máximo aceptado
     * @param batchSize   documentos por lote de escritura
     */
    public record Ingest(DataSize maxFileSize, int batchSize) {

        private static final int DEFAULT_MEGABYTES = 150;
        private static final int DEFAULT_BATCH_SIZE = 100;

        /**
         * Completa un tamaño o un lote que no vino en la configuración.
         */
        public Ingest {
            if (maxFileSize == null) {
                maxFileSize = DataSize.ofMegabytes(DEFAULT_MEGABYTES);
            }
            if (batchSize <= 0) {
                batchSize = DEFAULT_BATCH_SIZE;
            }
        }

        /**
         * 150 MB y lotes de 100, como el plan.
         *
         * @return límites por defecto
         */
        public static Ingest fallback() {
            return new Ingest(null, 0);
        }
    }

    /**
     * Prefijo público de las imágenes de TMDB.
     *
     * @param baseUrl URL sin el {@code poster_path}
     */
    public record Poster(String baseUrl) {

        private static final String DEFAULT_BASE_URL = "https://image.tmdb.org/t/p/w342";

        /**
         * Usa el CDN de TMDB cuando la propiedad no viene.
         */
        public Poster {
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = DEFAULT_BASE_URL;
            }
        }

        /**
         * URL de pósters por defecto.
         *
         * @return el CDN w342 de TMDB
         */
        public static Poster fallback() {
            return new Poster(DEFAULT_BASE_URL);
        }
    }

    /**
     * Interruptor de la migración a alias. En el YAML queda en falso.
     *
     * @param enabled {@code true} solo en el comando de la tarea 1.8
     */
    public record Bootstrap(boolean enabled) {

        /**
         * La migración no corre sola.
         *
         * @return interruptor apagado
         */
        public static Bootstrap fallback() {
            return new Bootstrap(false);
        }
    }

    /**
     * Cómo pide la demo los vectores de una sinopsis.
     *
     * <p>La calibración de la tarea 1.9 dejó los documentos en {@code ingest} y sin prefijo: fue la única
     * combinación con coseno ≥ 0.999 en las cinco películas. Esos dos literales viven solo en
     * {@link #DEFAULT_DOCUMENT_INPUT_TYPE} y {@link #DEFAULT_DOCUMENT_PREFIX}: los usan el
     * {@link DefaultValue} del enlace y el constructor de quien crea el record a mano. Las consultas no se
     * configuran (PLAN §6): {@code InferenceEmbeddingService} usa siempre {@code search} y {@code Query: }.</p>
     *
     * @param documentInputType {@code input_type} de los documentos; vacío significa no enviarlo
     * @param documentPrefix    texto que se antepone a la sinopsis; vacío significa no anteponer nada
     */
    public record Embedding(
            @DefaultValue(DEFAULT_DOCUMENT_INPUT_TYPE) String documentInputType,
            @DefaultValue(DEFAULT_DOCUMENT_PREFIX) String documentPrefix) {

        /** {@code input_type} calibrado para las sinopsis. */
        public static final String DEFAULT_DOCUMENT_INPUT_TYPE = "ingest";

        /** Prefijo calibrado para las sinopsis: ninguno. */
        public static final String DEFAULT_DOCUMENT_PREFIX = "";

        /**
         * Aplica los valores calibrados si quien crea el record no los pasa.
         */
        public Embedding {
            if (documentInputType == null) {
                documentInputType = DEFAULT_DOCUMENT_INPUT_TYPE;
            }
            if (documentPrefix == null) {
                documentPrefix = DEFAULT_DOCUMENT_PREFIX;
            }
        }
    }
}
