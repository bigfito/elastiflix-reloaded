package com.bigfito.elastiflix.ingest.mapping;

/**
 * Forma de un campo del documento, según el record {@code Movie}.
 *
 * <p>El mapping dice {@code keyword} tanto para un texto como para una lista. Esta forma es la que
 * usa el validador.</p>
 */
enum FieldKind {
    TEXT,
    KEYWORD,
    TEXT_LIST,
    BOOLEAN,
    INTEGER,
    DECIMAL,
    DATE,
    COLLECTION,
    KEYWORDS,
    /** Objeto plano de etiquetas keyword, como {@code enrichment}. */
    TAGS,
    VECTOR
}
