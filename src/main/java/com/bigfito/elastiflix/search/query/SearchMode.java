package com.bigfito.elastiflix.search.query;

/**
 * Modo de búsqueda. La Fase 4 traduce los nombres de la URL a estos valores.
 */
public enum SearchMode {

    /** Coincidencias de palabras en el título, con BM25. */
    FULL_TEXT,

    /** Cercanía de la sinopsis, con k-NN. */
    VECTOR,

    /** Fusión de las dos listas, con RRF. */
    HYBRID
}
