package com.bigfito.elastiflix.web.search;

/**
 * Textos del modo en la página de resultados.
 *
 * @param label     chip del modo
 * @param tone      clase de color
 * @param orderedBy cómo se ordenó la lista
 * @param legend    explicación del pie
 * @param limit     nota de k, o vacía en full-text
 */
public record ModeCopy(String label, String tone, String orderedBy, String legend, String limit) {
}
