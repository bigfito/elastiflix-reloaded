package com.bigfito.elastiflix.eval;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Métricas de {@code _rank_eval}, todas sobre los 10 primeros resultados.
 *
 * <p>Para MRR, precision y recall, una película es relevante si tiene calificación 2 o 3. nDCG usa la
 * calificación completa. Una película sin calificar cuenta como no relevante.</p>
 */
public enum Metric {

    /** Inverso de la posición de la primera película relevante. */
    MRR("mrr@10"),
    /** Fracción de los 10 resultados que es relevante. */
    PRECISION("p@10"),
    /** Fracción de las películas relevantes que aparece entre los 10 primeros. */
    RECALL("recall@10"),
    /** DCG normalizado: premia las calificaciones altas en las primeras posiciones. */
    NDCG("ndcg@10");

    /** Profundidad de todas las métricas. */
    public static final int K = 10;
    /** Calificación mínima de una película relevante. */
    public static final int RELEVANT = 2;

    private final String key;

    Metric(String key) {
        this.key = key;
    }

    /**
     * Nombre de la métrica en el informe y en {@code eval/thresholds.json}.
     *
     * @return por ejemplo {@code ndcg@10}
     */
    public String key() {
        return key;
    }

    /**
     * El objeto {@code metric} de la petición {@code _rank_eval}.
     *
     * @return por ejemplo {@code {"dcg": {"k": 10, "normalize": true}}}
     */
    public ObjectNode spec() {
        ObjectNode metric = JsonNodeFactory.instance.objectNode();
        switch (this) {
            case MRR -> metric.putObject("mean_reciprocal_rank")
                    .put("k", K).put("relevant_rating_threshold", RELEVANT);
            case PRECISION -> metric.putObject("precision")
                    .put("k", K).put("relevant_rating_threshold", RELEVANT).put("ignore_unlabeled", false);
            case RECALL -> metric.putObject("recall")
                    .put("k", K).put("relevant_rating_threshold", RELEVANT);
            case NDCG -> metric.putObject("dcg").put("k", K).put("normalize", true);
        }
        return metric;
    }
}
