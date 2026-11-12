package com.bigfito.elastiflix.search.query;

/**
 * Cómo se buscan las palabras de la consulta.
 */
public enum MatchType {

    /** Un solo campo: {@code title}, {@code original_title} o {@code plot}. */
    MATCH,

    /** Los dos títulos, y se queda con el campo que mejor coincide. */
    MULTI_MATCH
}
