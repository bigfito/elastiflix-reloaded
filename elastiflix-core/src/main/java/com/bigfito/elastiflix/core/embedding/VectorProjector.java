package com.bigfito.elastiflix.core.embedding;

import java.util.Arrays;

import com.bigfito.elastiflix.core.error.EmbeddingException;
import com.bigfito.elastiflix.core.error.ErrorCode;

/**
 * Recorta un vector del modelo a 512 o 256 dimensiones y lo renormaliza.
 */
public final class VectorProjector {

    /** Dimensión que devuelve el modelo. */
    public static final int MODEL_DIMENSIONS = 1024;

    private static final int MEDIUM_DIMENSIONS = 512;
    private static final int SMALL_DIMENSIONS = 256;

    private VectorProjector() {
    }

    /**
     * Se queda con los primeros valores y los divide por su norma L2.
     *
     * @param source     vector de {@value #MODEL_DIMENSIONS} dimensiones
     * @param dimensions 512 o 256
     * @return vector recortado, de norma 1
     */
    public static float[] project(float[] source, int dimensions) {
        requireSource(source);
        requireTarget(dimensions);
        float[] slice = Arrays.copyOf(source, dimensions);
        double norm = normOf(slice);
        if (norm == 0) {
            throw new EmbeddingException(ErrorCode.UNEXPECTED_VECTOR);
        }
        scale(slice, norm);
        return slice;
    }

    private static void requireSource(float[] source) {
        if (source == null || source.length != MODEL_DIMENSIONS) {
            throw new EmbeddingException(ErrorCode.UNEXPECTED_VECTOR);
        }
    }

    private static void requireTarget(int dimensions) {
        if (dimensions != MEDIUM_DIMENSIONS && dimensions != SMALL_DIMENSIONS) {
            throw new EmbeddingException(ErrorCode.UNEXPECTED_VECTOR);
        }
    }

    private static double normOf(float[] values) {
        double square = 0;
        for (float value : values) {
            square += (double) value * value;
        }
        return Math.sqrt(square);
    }

    private static void scale(float[] values, double norm) {
        for (int index = 0; index < values.length; index++) {
            values[index] = (float) (values[index] / norm);
        }
    }
}
