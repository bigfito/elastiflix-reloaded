package com.bigfito.elastiflix.web.search;

/**
 * Un valor de filtro con su conteo.
 *
 * @param value    texto del índice
 * @param count    películas
 * @param selected si ya está aplicado
 * @param extra    si «Ver todos» lo esconde al principio
 */
public record FacetChoice(String value, long count, boolean selected, boolean extra) {
}
