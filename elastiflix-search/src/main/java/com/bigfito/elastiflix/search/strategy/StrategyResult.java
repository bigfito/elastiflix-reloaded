package com.bigfito.elastiflix.search.strategy;

import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;

/**
 * Lo que una estrategia devuelve antes de paginar y de armar las facetas.
 *
 * @param hits         películas de esta respuesta; en full-text ya son la página
 * @param total        total exacto en full-text, o el tamaño de la lista en los otros modos
 * @param aggregations agregaciones de full-text; vacío en vectorial e híbrida
 */
public record StrategyResult(
        List<RankedHit> hits,
        long total,
        Map<String, Aggregate> aggregations) {

    /**
     * Copia las colecciones para que nadie las cambie después.
     */
    public StrategyResult {
        hits = List.copyOf(hits);
        aggregations = aggregations == null ? Map.of() : Map.copyOf(aggregations);
    }
}
