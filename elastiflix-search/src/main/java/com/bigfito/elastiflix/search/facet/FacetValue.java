package com.bigfito.elastiflix.search.facet;

/**
 * Cuántas películas traen un valor de filtro.
 *
 * @param label texto del índice, por ejemplo un género o {@code true}
 * @param count películas con ese valor; puede ser 0 si el usuario lo marcó
 */
public record FacetValue(String label, long count) {
}
