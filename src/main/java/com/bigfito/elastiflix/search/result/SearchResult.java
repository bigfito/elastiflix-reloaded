package com.bigfito.elastiflix.search.result;

import java.util.List;

import com.bigfito.elastiflix.search.facet.Facets;
import com.bigfito.elastiflix.search.page.Pagination;

/**
 * Respuesta de una búsqueda, lista para la página de resultados.
 *
 * @param hits        películas de esta página
 * @param total       películas que cumplen la búsqueda
 * @param facets      conteos del conjunto completo, no solo de la página
 * @param pagination  página, total de páginas y rango
 * @param adjustments etiquetas de los ajustes usados, en el orden del mockup
 * @param empty       estado vacío, o {@code null} cuando el total es mayor que 0
 */
public record SearchResult(
        List<SearchHit> hits,
        long total,
        Facets facets,
        Pagination pagination,
        List<String> adjustments,
        EmptyState empty) {

    /**
     * Copia las listas para que nadie las cambie después.
     */
    public SearchResult {
        hits = List.copyOf(hits);
        adjustments = List.copyOf(adjustments);
    }
}
