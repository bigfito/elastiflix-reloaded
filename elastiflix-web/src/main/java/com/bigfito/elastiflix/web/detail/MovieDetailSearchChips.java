package com.bigfito.elastiflix.web.detail;

import java.util.List;
import java.util.Locale;

import com.bigfito.elastiflix.web.search.AppliedFilters;
import com.bigfito.elastiflix.web.search.SearchForm;

/**
 * Genera los chips informativos del estado previo de búsqueda.
 */
final class MovieDetailSearchChips {

    private static final String DOT_SEPARATOR = " · ";

    private MovieDetailSearchChips() {
    }

    /**
     * Construye la lista de chips que reflejan el estado de la búsqueda anterior.
     *
     * @param form formulario de búsqueda previo
     * @return lista de chips
     */
    static List<SearchStateChip> chips(SearchForm form) {
        if (form == null) {
            return List.of();
        }
        return List.of(
                new SearchStateChip("Consulta", "«" + form.query() + "»"),
                new SearchStateChip("Modo", modeLabel(form.mode())),
                new SearchStateChip("Ajustes", settingsLabel(form)),
                new SearchStateChip("Filtros", filtersLabel(form)),
                new SearchStateChip("Página", pageLabel(form)));
    }

    private static String modeLabel(String mode) {
        if ("vectorial".equalsIgnoreCase(mode)) {
            return "Vectorial";
        }
        if ("hibrida".equalsIgnoreCase(mode)) {
            return "Híbrida";
        }
        return "Full-text";
    }

    private static String settingsLabel(SearchForm form) {
        if ("vectorial".equalsIgnoreCase(form.mode())) {
            return form.vector() + DOT_SEPARATOR + "k=" + form.k() + DOT_SEPARATOR + "num_candidates="
                    + form.candidates();
        }
        if ("hibrida".equalsIgnoreCase(form.mode())) {
            return "RRF (60)" + DOT_SEPARATOR + form.vector() + DOT_SEPARATOR + "k=" + form.k();
        }
        String field = "multi_match".equalsIgnoreCase(form.type()) ? "best_fields" : form.field();
        String op = form.operator() != null ? form.operator().toUpperCase(Locale.ROOT) : "";
        return form.type() + DOT_SEPARATOR + field + DOT_SEPARATOR + op;
    }

    private static String filtersLabel(SearchForm form) {
        List<String> chips = AppliedFilters.chips(form);
        if (chips.isEmpty()) {
            return "Sin filtros";
        }
        return String.join(DOT_SEPARATOR, chips);
    }

    private static String pageLabel(SearchForm form) {
        return "Página " + form.page() + DOT_SEPARATOR + form.pageSize() + " por página";
    }
}
