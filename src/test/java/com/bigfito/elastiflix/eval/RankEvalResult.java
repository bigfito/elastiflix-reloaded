package com.bigfito.elastiflix.eval;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * Respuesta de {@code _rank_eval} para una métrica: el promedio y el detalle de cada consulta.
 *
 * @param score   promedio de la métrica sobre todas las consultas
 * @param queries detalle por id de consulta, en el orden de la respuesta
 */
public record RankEvalResult(double score, Map<String, QueryScore> queries) {

    /**
     * Copia el detalle para que el registro no cambie.
     */
    public RankEvalResult {
        queries = Map.copyOf(queries);
    }

    /**
     * Resultado de una consulta.
     *
     * @param score métrica de esta consulta
     * @param hits  los primeros resultados, con su calificación si la tienen
     */
    public record QueryScore(double score, List<RatedHit> hits) {

        /**
         * Copia la lista para que el registro no cambie.
         */
        public QueryScore {
            hits = List.copyOf(hits);
        }

        /**
         * Posición de una película entre los resultados devueltos.
         *
         * @param movieId id de la película
         * @return de 1 en adelante, o 0 si no aparece
         */
        public int rankOf(String movieId) {
            for (int i = 0; i < hits.size(); i++) {
                if (hits.get(i).movieId().equals(movieId)) {
                    return i + 1;
                }
            }
            return 0;
        }
    }

    /**
     * Un resultado y su calificación.
     *
     * @param movieId id de la película
     * @param rating  calificación, o {@code null} si nadie la juzgó
     */
    public record RatedHit(String movieId, Integer rating) {
    }

    /**
     * Lee la respuesta JSON.
     *
     * @param response cuerpo de la respuesta de {@code _rank_eval}
     * @return promedio y detalle
     * @throws IllegalStateException si alguna consulta falló
     */
    public static RankEvalResult parse(JsonNode response) {
        JsonNode failures = response.path("failures");
        if (!failures.isEmpty()) {
            throw new IllegalStateException("_rank_eval devolvió errores: " + failures);
        }
        Map<String, QueryScore> queries = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> detail : response.path("details").properties()) {
            queries.put(detail.getKey(), query(detail.getValue()));
        }
        return new RankEvalResult(response.path("metric_score").asDouble(), queries);
    }

    private static QueryScore query(JsonNode detail) {
        List<RatedHit> hits = new ArrayList<>();
        for (JsonNode hit : detail.path("hits")) {
            JsonNode rating = hit.path("rating");
            Integer value = rating.isNumber() ? rating.asInt() : null;
            hits.add(new RatedHit(hit.path("hit").path("_id").asString(), value));
        }
        return new QueryScore(detail.path("metric_score").asDouble(), hits);
    }
}
