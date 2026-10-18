package com.bigfito.elastiflix.search.strategy;

/**
 * Constantes de Reciprocal Rank Fusion que comparten la petición y el cálculo.
 */
public final class Rrf {

    /** Constante de la fórmula {@code 1 / (rank_constant + posición)}. */
    public static final int RANK_CONSTANT = 60;

    /** Texto de una película que no está en una de las dos listas. */
    public static final String OUTSIDE_PREFIX = "fuera de su top ";

    private Rrf() {
    }

    /**
     * Un sumando de la fórmula: {@code 1 / (60 + posición)}.
     *
     * @param position puesto desde 1
     * @return el sumando
     */
    public static double term(int position) {
        return 1d / (RANK_CONSTANT + position);
    }
}
