package com.bigfito.elastiflix.search.strategy;

/**
 * Puntuación de una película en una de las dos listas de la híbrida, y su puesto.
 *
 * @param score    puntuación de esa lista, o {@code null} si no apareció
 * @param position puesto desde 1, o {@code fuera de su top } seguido de {@code k}
 */
public record ComponentScore(Double score, String position) {
}
