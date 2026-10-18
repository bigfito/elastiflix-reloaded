package com.bigfito.elastiflix.search.strategy;

import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;

/**
 * Ejecuta un modo de búsqueda y devuelve la lista cruda.
 */
public interface SearchStrategy {

    /**
     * Dice qué modo ejecuta esta clase.
     *
     * @return full-text, vectorial o híbrida
     */
    SearchMode mode();

    /**
     * Ejecuta la consulta. Quien llama ya la validó.
     *
     * @param query consulta válida
     * @return películas, total y agregaciones si el modo las pide
     */
    StrategyResult search(SearchQuery query);

    /**
     * Cuántas películas devuelve la misma consulta sin filtros.
     *
     * <p>En full-text es un conteo. En vectorial e híbrida la búsqueda pide un solo documento:
     * basta saber si hay alguno.</p>
     *
     * @param query consulta válida
     * @return el total sin filtros; en vectorial e híbrida, 0 o 1
     */
    long foundWithoutFilters(SearchQuery query);
}
