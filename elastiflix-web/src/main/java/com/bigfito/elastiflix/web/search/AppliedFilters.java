package com.bigfito.elastiflix.web.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Chips y nota de los filtros que ya están en la URL.
 */
public final class AppliedFilters {

    private AppliedFilters() {
    }

    /**
     * Textos de la fila «Filtros aplicados», en el orden del mockup.
     *
     * @param form URL actual
     * @return chips; vacío si no hay filtros
     */
    public static List<String> chips(SearchForm form) {
        List<String> chips = new ArrayList<>();
        adult(chips, form.adult());
        labeled(chips, "Géneros: ", form.genres());
        dates(chips, form);
        labeled(chips, "Idiomas: ", form.languages());
        labeled(chips, "Clasificación: ", form.ratings());
        return List.copyOf(chips);
    }

    /**
     * Nota bajo el título de filtros. Cambia cuando la URL ya trae alguno.
     *
     * @param form URL actual
     * @return el texto del mockup
     */
    public static String note(SearchForm form) {
        if (chips(form).isEmpty()) {
            return "Los valores salen de los resultados actuales y se actualizan cada vez que cambian.";
        }
        return "Valores actualizados con los resultados filtrados.";
    }

    private static void adult(List<String> chips, List<String> adult) {
        if (adult.contains("false")) {
            chips.add("Adultos: Solo no adultos");
        } else if (adult.contains("true")) {
            chips.add("Adultos: Solo adultos");
        }
    }

    private static void labeled(List<String> chips, String prefix, List<String> values) {
        if (!values.isEmpty()) {
            chips.add(prefix + String.join(" o ", values));
        }
    }

    private static void dates(List<String> chips, SearchForm form) {
        if (form.from().isBlank() && form.to().isBlank()) {
            return;
        }
        chips.add("Estreno: " + shown(form.from()) + " → " + shown(form.to()));
    }

    private static String shown(String value) {
        if (value.isBlank()) {
            return "…";
        }
        return value;
    }
}
