package com.bigfito.elastiflix.ingest.job;

import java.util.List;

import com.bigfito.elastiflix.core.model.Movie;

/**
 * Lotes tomados de una lista que ya está en memoria.
 */
public final class ListMovieSource implements MovieBatchSource {

    private final List<Movie> movies;
    private int cursor;

    /**
     * Copia la lista para que el que llama no la cambie mientras se importa.
     *
     * @param movies películas, en orden
     */
    public ListMovieSource(List<Movie> movies) {
        this.movies = List.copyOf(movies);
    }

    @Override
    public List<Movie> next(int size) {
        if (cursor >= movies.size()) {
            return List.of();
        }
        int end = Math.min(cursor + size, movies.size());
        List<Movie> batch = movies.subList(cursor, end);
        cursor = end;
        return List.copyOf(batch);
    }
}
