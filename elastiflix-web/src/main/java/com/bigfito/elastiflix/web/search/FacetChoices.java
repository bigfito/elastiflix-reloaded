package com.bigfito.elastiflix.web.search;

import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.search.facet.FacetValue;

/**
 * Marca los valores aplicados y deja los primeros seis a la vista.
 */
public final class FacetChoices {

    /** Cuántos valores se ven antes de «Ver todos». */
    static final int VISIBLE = 6;

    private FacetChoices() {
    }

    /**
     * Opciones de un filtro de casillas.
     *
     * @param values   faceta
     * @param selected valores de la URL
     * @return opciones en el orden de la faceta
     */
    public static List<FacetChoice> of(List<FacetValue> values, List<String> selected) {
        List<FacetChoice> choices = new ArrayList<>();
        int index = 0;
        for (FacetValue value : values) {
            choices.add(choice(value, selected, index));
            index++;
        }
        return List.copyOf(choices);
    }

    private static FacetChoice choice(FacetValue value, List<String> selected, int index) {
        return new FacetChoice(value.label(), value.count(), selected.contains(value.label()), index >= VISIBLE);
    }

    /**
     * Suma los conteos de una faceta.
     *
     * @param values faceta
     * @return total
     */
    public static long total(List<FacetValue> values) {
        long sum = 0;
        for (FacetValue value : values) {
            sum = sum + value.count();
        }
        return sum;
    }

    /**
     * Conserva los valores de la URL cuando la búsqueda no llegó a ejecutarse.
     *
     * @param selected valores marcados
     * @return opciones ya seleccionadas, con conteo 0
     */
    public static List<FacetChoice> kept(List<String> selected) {
        List<FacetChoice> choices = new ArrayList<>();
        for (String value : selected) {
            choices.add(new FacetChoice(value, 0, true, false));
        }
        return List.copyOf(choices);
    }

    /**
     * Conteo de un valor, o 0 si no está.
     *
     * @param values faceta
     * @param label  por ejemplo {@code false}
     * @return conteo
     */
    public static long countOf(List<FacetValue> values, String label) {
        for (FacetValue value : values) {
            if (label.equals(value.label())) {
                return value.count();
            }
        }
        return 0;
    }
}
