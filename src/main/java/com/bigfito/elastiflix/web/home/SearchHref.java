package com.bigfito.elastiflix.web.home;

import com.bigfito.elastiflix.web.EncodedQuery;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * Arma la URL de una sugerencia. Solo incluye los parámetros que usa ese modo.
 */
public final class SearchHref {

    /** k inicial del mockup. */
    static final int DEFAULT_K = 100;

    /** num_candidates inicial del mockup. */
    static final int DEFAULT_CANDIDATES = 200;

    private SearchHref() {
    }

    /**
     * Enlace de full-text.
     *
     * @param query    texto
     * @param type     {@code match} o {@code multi_match}
     * @param field    {@code title} u {@code original_title}; vacío en multi-match
     * @param operator {@code or} o {@code and}
     * @return ruta con query string
     */
    public static String fullText(String query, String type, String field, String operator) {
        UriComponentsBuilder builder = base(query, "fulltext")
                .queryParam("tipo", type)
                .queryParam("operador", operator);
        if (!field.isBlank()) {
            builder.queryParam("campo", field);
        }
        return EncodedQuery.of(builder);
    }

    /**
     * Enlace vectorial.
     *
     * @param query  texto
     * @param vector {@code plot_256}, {@code plot_512} o {@code plot_1024}
     * @return ruta con {@code k} 100 y {@code candidatos} 200
     */
    public static String vector(String query, String vector) {
        return EncodedQuery.of(knn(base(query, "vectorial"), vector));
    }

    /**
     * Enlace híbrido: full-text y k-NN con los valores iniciales del mockup.
     *
     * @param query    texto
     * @param operator {@code or} o {@code and}
     * @return ruta con match sobre title, plot_256, k 100 y candidatos 200
     */
    public static String hybrid(String query, String operator) {
        UriComponentsBuilder builder = base(query, "hibrida")
                .queryParam("tipo", "match")
                .queryParam("campo", "title")
                .queryParam("operador", operator);
        return EncodedQuery.of(knn(builder, "plot_256"));
    }

    private static UriComponentsBuilder base(String query, String mode) {
        return UriComponentsBuilder.fromPath("/buscar").queryParam("q", query).queryParam("modo", mode);
    }

    private static UriComponentsBuilder knn(UriComponentsBuilder builder, String vector) {
        return builder.queryParam("vector", vector)
                .queryParam("k", DEFAULT_K)
                .queryParam("candidatos", DEFAULT_CANDIDATES);
    }
}
