package com.bigfito.elastiflix.core.embedding;

/**
 * Coseno entre dos vectores.
 *
 * <p>El código de producción no la usa: la usan las pruebas de core e ingest que comparan embeddings.</p>
 */
public final class CosineSimilarity {

    private CosineSimilarity() {
    }

    /**
     * Calcula el coseno. Si un vector tiene norma cero, el resultado es cero.
     *
     * @param left  primer vector
     * @param right segundo vector, de la misma dimensión
     * @return coseno entre -1 y 1
     */
    public static double between(float[] left, float[] right) {
        if (left.length != right.length) {
            throw new IllegalArgumentException("Los vectores no tienen la misma dimensión");
        }
        double dot = 0;
        double leftSquare = 0;
        double rightSquare = 0;
        for (int index = 0; index < left.length; index++) {
            dot += (double) left[index] * right[index];
            leftSquare += (double) left[index] * left[index];
            rightSquare += (double) right[index] * right[index];
        }
        if (leftSquare == 0 || rightSquare == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftSquare) * Math.sqrt(rightSquare));
    }
}
