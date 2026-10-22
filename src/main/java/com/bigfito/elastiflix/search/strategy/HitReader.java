package com.bigfito.elastiflix.search.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bigfito.elastiflix.core.model.Movie;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;

/**
 * Pasa la respuesta de Elasticsearch a {@link RankedHit} sin copiar los vectores.
 */
public final class HitReader {

    private HitReader() {
    }

    /**
     * Lee solo el id y la puntuación. El documento no vino en la respuesta.
     *
     * @param response parte léxica o vectorial del {@code _msearch}
     * @return hits con una película que solo tiene id
     */
    public static List<RankedHit> ids(ResponseBody<Movie> response) {
        List<RankedHit> hits = new ArrayList<>();
        for (Hit<Movie> hit : response.hits().hits()) {
            addId(hits, hit);
        }
        return hits;
    }

    /**
     * Lee las películas de una respuesta. Omite un hit sin documento.
     *
     * @param response respuesta de {@code _search} o una parte de {@code _msearch}
     * @return películas en el orden de Elasticsearch
     */
    public static List<RankedHit> hits(ResponseBody<Movie> response) {
        List<RankedHit> hits = new ArrayList<>();
        for (Hit<Movie> hit : response.hits().hits()) {
            add(hits, hit);
        }
        return hits;
    }

    /**
     * Total exacto cuando viene {@code hits.total}; si no, el tamaño de la página.
     *
     * @param response respuesta de {@code _search}
     * @return el total
     */
    public static long total(ResponseBody<Movie> response) {
        return totalOf(response.hits());
    }

    /**
     * Agregaciones de la respuesta, o un mapa vacío.
     *
     * @param response respuesta de {@code _search}
     * @return agregaciones por nombre
     */
    public static Map<String, Aggregate> aggregations(ResponseBody<Movie> response) {
        if (response.aggregations() == null) {
            return Map.of();
        }
        return response.aggregations();
    }

    private static void addId(List<RankedHit> hits, Hit<Movie> hit) {
        if (hit.id() != null) {
            hits.add(RankedHit.scored(Movie.identified(hit.id()), hit.score()));
        }
    }

    private static void add(List<RankedHit> hits, Hit<Movie> hit) {
        if (hit.source() != null) {
            hits.add(RankedHit.scored(hit.source(), hit.score()));
        }
    }

    private static long totalOf(HitsMetadata<Movie> hits) {
        if (hits.total() == null) {
            return hits.hits().size();
        }
        return hits.total().value();
    }
}
