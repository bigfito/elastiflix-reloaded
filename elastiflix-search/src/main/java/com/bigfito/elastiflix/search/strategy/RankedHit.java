package com.bigfito.elastiflix.search.strategy;

import com.bigfito.elastiflix.core.model.Movie;

/**
 * Una película tal como la devolvió Elasticsearch, con la puntuación de esa lista.
 *
 * @param movie película sin vectores
 * @param score puntuación de la lista, o {@code null} si Elasticsearch no la envió
 */
public record RankedHit(Movie movie, Double score) {
}
