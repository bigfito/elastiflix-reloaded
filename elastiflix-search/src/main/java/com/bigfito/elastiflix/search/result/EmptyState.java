package com.bigfito.elastiflix.search.result;

/**
 * Por qué una búsqueda no devolvió películas. Es {@code null} en {@link SearchResult} cuando hay
 * resultados. Los textos que ve la persona los arma la web en su idioma.
 */
public enum EmptyState {

    /** La consulta no encontró películas, ni siquiera sin filtros. */
    NONE,

    /** Había películas, pero los filtros las dejaron fuera. */
    FILTERED
}
