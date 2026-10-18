package com.bigfito.elastiflix.web.search;

/**
 * Un número de la paginación.
 *
 * @param number  página
 * @param href    enlace
 * @param current si es la página abierta
 */
public record PageButton(int number, String href, boolean current) {
}
