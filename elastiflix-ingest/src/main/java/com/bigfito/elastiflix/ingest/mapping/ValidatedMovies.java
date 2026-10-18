package com.bigfito.elastiflix.ingest.mapping;

import java.util.List;

import com.bigfito.elastiflix.core.model.Movie;

/**
 * Resultado de validar un archivo.
 *
 * @param movies       películas sin vectores; vacía si hay errores
 * @param report       errores y advertencias
 * @param replaceCount ids del archivo que ya están en el catálogo
 */
public record ValidatedMovies(List<Movie> movies, ValidationReport report, int replaceCount) {

    /**
     * Copia la lista de películas para que nadie la cambie después.
     */
    public ValidatedMovies {
        movies = List.copyOf(movies);
    }
}
