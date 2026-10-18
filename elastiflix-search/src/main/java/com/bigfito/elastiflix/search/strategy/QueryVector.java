package com.bigfito.elastiflix.search.strategy;

import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.embedding.VectorProjector;
import com.bigfito.elastiflix.search.query.VectorField;

/**
 * Adapta el vector de 1024 del modelo al campo de la consulta.
 */
public final class QueryVector {

    private QueryVector() {
    }

    /**
     * Devuelve el vector tal cual si el campo es {@code plot_1024}. Si no, recorta y renormaliza.
     *
     * @param model vector de 1024 que devolvió la inferencia
     * @param field campo k-NN pedido
     * @return vector listo para {@code query_vector}
     */
    public static float[] forField(float[] model, VectorField field) {
        if (field.dimensions() == VectorProjector.MODEL_DIMENSIONS) {
            return model;
        }
        return VectorProjector.project(model, field.dimensions());
    }

    /**
     * Pasa el vector al tipo que pide el cliente. No lo registra.
     *
     * @param values vector ya recortado
     * @return los mismos números como {@link Float}
     */
    public static List<Float> boxed(float[] values) {
        List<Float> boxed = new ArrayList<>(values.length);
        for (float value : values) {
            boxed.add(value);
        }
        return boxed;
    }
}
