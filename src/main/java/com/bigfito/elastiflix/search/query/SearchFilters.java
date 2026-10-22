package com.bigfito.elastiflix.search.query;

import java.time.LocalDate;
import java.util.List;

/**
 * Filtros de una búsqueda. Una lista vacía significa que ese filtro no se aplica.
 *
 * @param adult            {@code true}, {@code false}, los dos, o ninguno
 * @param genres           géneros; basta uno
 * @param spokenLanguages  idiomas hablados; basta uno
 * @param ratings          clasificaciones; basta una
 * @param from             estreno desde esta fecha, inclusive, o {@code null}
 * @param to               estreno hasta esta fecha, inclusive, o {@code null}
 */
public record SearchFilters(
        List<Boolean> adult,
        List<String> genres,
        List<String> spokenLanguages,
        List<String> ratings,
        LocalDate from,
        LocalDate to) {

    /**
     * Copia las listas para que nadie las cambie después. {@code null} queda como lista vacía.
     */
    public SearchFilters {
        adult = copy(adult);
        genres = copy(genres);
        spokenLanguages = copy(spokenLanguages);
        ratings = copy(ratings);
    }

    /**
     * Ningún filtro.
     *
     * @return filtros vacíos
     */
    public static SearchFilters none() {
        return new SearchFilters(null, null, null, null, null, null);
    }

    /**
     * Dice si no hay ningún filtro aplicado.
     *
     * @return {@code true} cuando todas las listas están vacías y no hay fechas
     */
    public boolean isEmpty() {
        return adult.isEmpty()
                && genres.isEmpty()
                && spokenLanguages.isEmpty()
                && ratings.isEmpty()
                && from == null
                && to == null;
    }

    private static <T> List<T> copy(List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return List.copyOf(values);
    }
}
