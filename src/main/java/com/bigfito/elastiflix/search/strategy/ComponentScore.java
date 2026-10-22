package com.bigfito.elastiflix.search.strategy;

/**
 * Puntuación de una película en una de las dos listas de la híbrida, y su puesto.
 *
 * @param score puntuación de esa lista, o {@code null} si no apareció
 * @param rank  puesto desde 1, o {@code null} si quedó fuera de los {@code k} de esa lista
 * @param k     cuántas películas trae cada lista
 */
public record ComponentScore(Double score, Integer rank, int k) {

    /**
     * Una película que no apareció en esa lista.
     *
     * @param k cuántas películas trae cada lista
     * @return la puntuación vacía, fuera del top {@code k}
     */
    public static ComponentScore outside(int k) {
        return new ComponentScore(null, null, k);
    }

    /**
     * Dice si la película quedó fuera de los {@code k} de esa lista.
     *
     * @return {@code true} si no tiene puesto
     */
    public boolean isOutside() {
        return rank == null;
    }
}
