package com.bigfito.elastiflix.search.strategy;

/**
 * Constantes de Reciprocal Rank Fusion que comparten la petición y el cálculo.
 */
public final class Rrf {

    /** Constante de la fórmula {@code 1 / (rank_constant + posición)}. */
    public static final int RANK_CONSTANT = 60;

    private Rrf() {
    }
}
