package com.bigfito.elastiflix.search.strategy;

import com.bigfito.elastiflix.core.model.Movie;

/**
 * Una película tal como la devolvió una lista, con las puntuaciones que ya se puedan decir.
 *
 * <p>En full-text y en vectorial solo viene {@code score}. En híbrida {@code score} es la de RRF,
 * y {@code lexical} y {@code vector} dicen la puntuación y la posición en cada componente.</p>
 *
 * @param movie   película sin vectores
 * @param score   puntuación de la lista principal, o {@code null}
 * @param lexical puntuación BM25 y posición, o {@code null} fuera de la híbrida
 * @param vector  similitud y posición, o {@code null} fuera de la híbrida
 */
public record RankedHit(
        Movie movie,
        Double score,
        ComponentScore lexical,
        ComponentScore vector) {

    /**
     * Una película de una sola lista, sin componentes híbridos.
     *
     * @param movie película
     * @param score puntuación de esa lista
     * @return el resultado
     */
    public static RankedHit scored(Movie movie, Double score) {
        return new RankedHit(movie, score, null, null);
    }
}
