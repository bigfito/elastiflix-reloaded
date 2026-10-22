package com.bigfito.elastiflix.search.query;

/**
 * Cómo se buscan las palabras del título.
 */
public enum MatchType {

    /** Un solo campo: {@code title} u {@code original_title}. */
    MATCH,

    /** Los dos títulos, y se queda con el campo que mejor coincide. */
    MULTI_MATCH
}
