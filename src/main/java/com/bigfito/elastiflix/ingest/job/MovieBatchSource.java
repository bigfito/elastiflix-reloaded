package com.bigfito.elastiflix.ingest.job;

import java.util.List;

import com.bigfito.elastiflix.core.model.Movie;

/**
 * Entrega películas de a lotes. Una lista vacía significa que ya no hay más.
 */
public interface MovieBatchSource {

    /**
     * Siguiente lote, o vacío si el archivo ya se leyó.
     *
     * @param size máximo de películas
     * @return el lote, sin {@code null}
     */
    List<Movie> next(int size);
}
