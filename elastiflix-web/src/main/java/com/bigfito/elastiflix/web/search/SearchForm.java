package com.bigfito.elastiflix.web.search;

import java.util.List;

/**
 * Parámetros de {@code GET /buscar}, todavía como texto de la URL.
 *
 * @param query      consulta
 * @param mode       {@code fulltext}, {@code vectorial} o {@code hibrida}
 * @param type       {@code match} o {@code multi_match}
 * @param field      campo del match
 * @param operator   {@code or} o {@code and}
 * @param vector     campo k-NN
 * @param k          texto de k
 * @param candidates texto de num_candidates
 * @param page       página desde 1
 * @param pageSize   25, 50 o 100
 * @param adult      {@code true} o {@code false}
 * @param genres     géneros marcados
 * @param languages  idiomas marcados
 * @param ratings    clasificaciones marcadas
 * @param from       estreno desde
 * @param to         estreno hasta
 */
public record SearchForm(
        String query,
        String mode,
        String type,
        String field,
        String operator,
        String vector,
        String k,
        String candidates,
        String page,
        String pageSize,
        List<String> adult,
        List<String> genres,
        List<String> languages,
        List<String> ratings,
        String from,
        String to) {

    /**
     * Copia las listas. Una lista ausente queda vacía.
     */
    public SearchForm {
        adult = copy(adult);
        genres = copy(genres);
        languages = copy(languages);
        ratings = copy(ratings);
    }

    private static List<String> copy(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return List.copyOf(values);
    }
}
