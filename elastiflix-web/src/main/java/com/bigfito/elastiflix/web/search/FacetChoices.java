package com.bigfito.elastiflix.web.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * Valores de la faceta y, al final con conteo 0, los marcados que la faceta ya no trae.
     *
     * <p>Como {@code mk(...)} del mockup: un filtro marcado siempre se puede ver y desmarcar, aunque los
     * resultados ya no lo tengan (por ejemplo en «Ningún resultado cumple los filtros»). Un valor marcado
     * nunca queda oculto detrás de «Ver todos».</p>
     *
     * @param values   conteos de la faceta, en su orden
     * @param selected valores marcados en la petición
     * @return opciones para la plantilla
     */
    public static List<FacetChoice> of(List<FacetValue> values, List<String> selected) {
        List<FacetChoice> choices = new ArrayList<>();
        Set<String> shown = new HashSet<>();
        for (FacetValue value : values) {
            boolean on = selected.contains(value.label());
            choices.add(new FacetChoice(value.label(), value.count(), on, !on && shown.size() >= VISIBLE));
            shown.add(value.label());
        }
        for (String value : selected) {
            if (shown.add(value)) {
                choices.add(new FacetChoice(value, 0, true, false));
            }
        }
        return List.copyOf(choices);
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
