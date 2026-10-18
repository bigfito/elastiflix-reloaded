package com.bigfito.elastiflix.search.query;

/**
 * Campo de sinopsis sobre el que corre k-NN.
 */
public enum VectorField {

    /** Primeros 256 valores del modelo, renormalizados. */
    PLOT_256("plot_256", Dimensions.SMALL),

    /** Primeros 512 valores del modelo, renormalizados. */
    PLOT_512("plot_512", Dimensions.MEDIUM),

    /** Vector completo del modelo. */
    PLOT_1024("plot_1024", Dimensions.FULL);

    private final String fieldName;
    private final int dimensions;

    VectorField(String fieldName, int dimensions) {
        this.fieldName = fieldName;
        this.dimensions = dimensions;
    }

    /**
     * Nombre del campo en el índice.
     *
     * @return {@code plot_256}, {@code plot_512} o {@code plot_1024}
     */
    public String fieldName() {
        return fieldName;
    }

    /**
     * Cuántos números tiene el vector de consulta para este campo.
     *
     * @return 256, 512 o 1024
     */
    public int dimensions() {
        return dimensions;
    }

    /**
     * Las tres dimensiones viven aquí para no repetir números sueltos en el enum.
     */
    static final class Dimensions {
        static final int SMALL = 256;
        static final int MEDIUM = 512;
        static final int FULL = 1024;

        private Dimensions() {
        }
    }
}
