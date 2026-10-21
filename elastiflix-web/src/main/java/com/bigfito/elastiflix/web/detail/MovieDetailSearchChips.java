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
                new SearchStateChip(messages.getFor(locale, "detail.chip.query"),
                        messages.getFor(locale, "detail.quoted", form.query())),
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

    // Como el mockup: full-text «tipo · campo · operador», vectorial «vector · k · num_candidates» e híbrida
    // «tipo · operador · vector · k · num_candidates».
    private static String settingsLabel(SearchForm form) {
        if ("vectorial".equalsIgnoreCase(form.mode())) {
            return knnLabel(form);
        }
        if ("hibrida".equalsIgnoreCase(form.mode())) {
            return form.type() + DOT_SEPARATOR + operatorLabel(form) + DOT_SEPARATOR + knnLabel(form);
        }
        String field = "multi_match".equalsIgnoreCase(form.type()) ? "best_fields" : form.field();
        return form.type() + DOT_SEPARATOR + field + DOT_SEPARATOR + operatorLabel(form);
    }

    private static String knnLabel(SearchForm form) {
        return form.vector() + DOT_SEPARATOR + "k=" + form.k() + DOT_SEPARATOR + "num_candidates=" + form.candidates();
    }

    private static String operatorLabel(SearchForm form) {
        return form.operator() != null ? form.operator().toUpperCase(Locale.ROOT) : "";
    }

    private static String filtersLabel(SearchForm form, Locale locale, UiMessages messages) {
        List<String> chips = AppliedFilters.chips(form, locale, messages);
        if (chips.isEmpty()) {
            return messages.getFor(locale, "detail.chip.noFilters");
        }
        return String.join(DOT_SEPARATOR, chips);
    }
}
