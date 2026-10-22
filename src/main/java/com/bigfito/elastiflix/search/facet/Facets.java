package com.bigfito.elastiflix.search.facet;

import java.time.LocalDate;
import java.util.List;

/**
 * Facetas de una búsqueda, ya ordenadas.
 *
 * <p>Las fechas son el estreno más antiguo y el más reciente del mismo conjunto. Las dos son
 * {@code null} cuando ninguna película trae fecha.</p>
 *
 * @param genres           géneros
 * @param spokenLanguages  idiomas hablados
 * @param ratings          clasificaciones
 * @param adult            {@code true} y {@code false}, como los guarda el índice
 * @param earliestRelease  estreno más antiguo, o {@code null}
 * @param latestRelease    estreno más reciente, o {@code null}
 */
public record Facets(
        List<FacetValue> genres,
        List<FacetValue> spokenLanguages,
        List<FacetValue> ratings,
        List<FacetValue> adult,
        LocalDate earliestRelease,
        LocalDate latestRelease) {

    /**
     * Copia las listas para que nadie las cambie después.
     */
    public Facets {
        genres = List.copyOf(genres);
        spokenLanguages = List.copyOf(spokenLanguages);
        ratings = List.copyOf(ratings);
        adult = List.copyOf(adult);
    }
}
