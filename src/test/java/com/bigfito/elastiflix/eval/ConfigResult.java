package com.bigfito.elastiflix.eval;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Las cuatro métricas de una configuración.
 *
 * @param config  configuración evaluada
 * @param metrics resultado de {@code _rank_eval} por métrica
 */
public record ConfigResult(EvalConfig config, Map<Metric, RankEvalResult> metrics) {

    /**
     * Copia el mapa para que el registro no cambie.
     */
    public ConfigResult {
        metrics = new EnumMap<>(metrics);
    }

    /**
     * Promedio de la métrica sobre todas las consultas.
     *
     * @param metric métrica
     * @return entre 0 y 1
     */
    public double score(Metric metric) {
        return metrics.get(metric).score();
    }

    /**
     * Promedio de la métrica sobre un subconjunto de consultas.
     *
     * @param metric    métrica
     * @param judgments todas las consultas
     * @param subset    cuáles entran en el promedio
     * @return entre 0 y 1, o {@code NaN} si el subconjunto está vacío
     */
    public double score(Metric metric, List<Judgment> judgments, Predicate<Judgment> subset) {
        Map<String, RankEvalResult.QueryScore> queries = metrics.get(metric).queries();
        return judgments.stream()
                .filter(subset)
                .mapToDouble(judgment -> queries.get(judgment.id()).score())
                .average()
                .orElse(Double.NaN);
    }

    /**
     * Posición de la película buscada entre los 10 primeros resultados.
     *
     * @param judgment consulta
     * @return de 1 a 10, o 0 si no aparece
     */
    public int targetRank(Judgment judgment) {
        return hits(judgment).rankOf(judgment.target());
    }

    /**
     * Los 10 primeros resultados de una consulta, con su calificación.
     *
     * @param judgment consulta
     * @return resultados en orden
     */
    public RankEvalResult.QueryScore hits(Judgment judgment) {
        return metrics.get(Metric.NDCG).queries().get(judgment.id());
    }
}
