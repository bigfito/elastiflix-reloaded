package com.bigfito.elastiflix.web.search;

import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;

/**
 * Leyendas del mockup de resultados, una por modo.
 */
public final class ModeCopies {

    private static final String BM25 =
            "La puntuación BM25 no tiene una escala fija: depende de la consulta y del catálogo, "
                    + "así que solo sirve para comparar resultados de una misma búsqueda.";
    private static final String COSINE =
            "Con similitud cosine, Elasticsearch devuelve la puntuación (1 + coseno) / 2: "
                    + "va de 0 a 1, y 1 significa que el vector de la sinopsis apunta en la misma "
                    + "dirección que el de la consulta.";
    private static final String HYBRID =
            "En modo híbrido, cada resultado muestra su puntuación BM25 y su similitud k-NN, "
                    + "con su posición en cada lista. RRF ordena por esas posiciones, no por las "
                    + "puntuaciones. Los filtros se aplican a las dos búsquedas antes de la fusión.";

    private ModeCopies() {
    }

    /**
     * Textos del modo de la consulta ya ejecutada.
     *
     * @param query consulta
     * @return chip, orden, leyenda y nota de límite
     */
    public static ModeCopy of(SearchQuery query) {
        if (query.mode() == SearchMode.VECTOR) {
            return vector(query.knn().k());
        }
        if (query.mode() == SearchMode.HYBRID) {
            return hybrid(query.knn().k());
        }
        return new ModeCopy("Full-text", "tone-sky", "Ordenados por puntuación BM25", BM25, "");
    }

    private static ModeCopy vector(int k) {
        String limit = "La búsqueda vectorial devuelve como máximo k resultados (k = " + k
                + "), repartidos en páginas. Los filtros se aplican dentro de la búsqueda k-NN, "
                + "así que las k películas devueltas los cumplen, y sus valores se calculan sobre "
                + "esos resultados.";
        return new ModeCopy("Vectorial", "tone-mint", "Ordenados por similitud k-NN", COSINE, limit);
    }

    private static ModeCopy hybrid(int k) {
        String limit = "La búsqueda híbrida combina hasta k resultados de cada lista "
                + "(rank_window_size = k = " + k
                + ") y permite recorrer hasta " + k
                + " resultados combinados. Los valores de los filtros se calculan sobre esa lista "
                + "combinada.";
        return new ModeCopy("Híbrida", "tone-lavender", "Ordenados por fusión RRF", HYBRID, limit);
    }
}
