package com.bigfito.elastiflix.search.query;

/**
 * Si basta una palabra de la consulta o hacen falta todas.
 */
public enum TextOperator {

    /** Basta una palabra. */
    OR,

    /** Hacen falta todas, y con multi-match en el mismo campo. */
    AND
}
