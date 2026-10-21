package com.bigfito.elastiflix.web.detail;

import java.util.List;
import java.util.Locale;

import com.bigfito.elastiflix.web.locale.UiMessages;
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
     * Construye la lista de chips que reflejan el estado de la búsqueda anterior localizados.
     *
     * @param form     formulario de búsqueda previo
     * @param locale   idioma activo
     * @param messages catálogo de mensajes
     * @return lista de chips
     */
    static List<SearchStateChip> chips(SearchForm form, Locale locale, UiMessages messages) {
        if (form == null) {
            return List.of();
        }
        return List.of(
                new SearchStateChip(messages.getFor(locale, "detail.chip.query"), "«" + form.query() + "»"),
                new SearchStateChip(messages.getFor(locale, "detail.chip.mode"), modeLabel(form.mode(), locale,
                        messages)),
                new SearchStateChip(messages.getFor(locale, "detail.chip.settings"), settingsLabel(form)),
                new SearchStateChip(messages.getFor(locale, "detail.chip.filters"),
                        filtersLabel(form, locale, messages)),
                new SearchStateChip(messages.getFor(locale, "detail.chip.page"),
                        messages.getFor(locale, "detail.chip.pageFormat", form.page(), form.pageSize())));
    }

    private static String modeLabel(String mode, Locale locale, UiMessages messages) {
        if ("vectorial".equalsIgnoreCase(mode)) {
            return messages.getFor(locale, "detail.chip.mode.vector");
        }
        if ("hibrida".equalsIgnoreCase(mode)) {
            return messages.getFor(locale, "detail.chip.mode.hybrid");
        }
        return messages.getFor(locale, "detail.chip.mode.fulltext");
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

    private static String filtersLabel(SearchForm form, Locale locale, UiMessages messages) {
        List<String> chips = AppliedFilters.chips(form, locale, messages);
        if (chips.isEmpty()) {
            return messages.getFor(locale, "detail.chip.noFilters");
        }
        return String.join(DOT_SEPARATOR, chips);
    }
}
