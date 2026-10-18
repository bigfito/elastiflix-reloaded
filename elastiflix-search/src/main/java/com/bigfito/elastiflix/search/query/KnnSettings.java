package com.bigfito.elastiflix.search.query;

/**
 * Ajustes de la parte vectorial. En modo full-text no se envían.
 *
 * @param field         campo de la sinopsis
 * @param k             máximo de resultados de esa lista
 * @param numCandidates candidatos que se evalúan antes de quedarse con {@code k}
 */
public record KnnSettings(VectorField field, int k, int numCandidates) {
}
