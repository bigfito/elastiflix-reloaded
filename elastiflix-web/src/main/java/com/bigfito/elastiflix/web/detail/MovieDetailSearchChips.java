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
     * Construye la lista de chips que reflejan el estado de la búsqueda anterior en español.
     *
     * @param form formulario de búsqueda previo
     * @return lista de chips
     */
    static List<SearchStateChip> chips(SearchForm form) {
        return chips(form, Locale.forLanguageTag("es"), null);
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
                new SearchStateChip(msg(messages, locale, "detail.chip.query", "Consulta"), "«" + form.query() + "»"),
                new SearchStateChip(msg(messages, locale, "detail.chip.mode", "Modo"),
                        modeLabel(form.mode(), locale, messages)),
                new SearchStateChip(msg(messages, locale, "detail.chip.settings", "Ajustes"), settingsLabel(form)),
                new SearchStateChip(msg(messages, locale, "detail.chip.filters", "Filtros"),
                        filtersLabel(form, locale, messages)),
                new SearchStateChip(msg(messages, locale, "detail.chip.page", "Página"),
                        pageLabel(form, locale, messages)));
    }

    private static String modeLabel(String mode, Locale locale, UiMessages messages) {
        if ("vectorial".equalsIgnoreCase(mode)) {
            return msg(messages, locale, "detail.chip.mode.vector", "Vectorial");
        }
        if ("hibrida".equalsIgnoreCase(mode)) {
            return msg(messages, locale, "detail.chip.mode.hybrid", "Híbrida");
        }
        return msg(messages, locale, "detail.chip.mode.fulltext", "Full-text");
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
        List<String> chips = messages != null
                ? AppliedFilters.chips(form, locale, messages)
                : AppliedFilters.chips(form);
        if (chips.isEmpty()) {
            return msg(messages, locale, "detail.chip.noFilters", "Sin filtros");
        }
        return String.join(DOT_SEPARATOR, chips);
    }

    private static String pageLabel(SearchForm form, Locale locale, UiMessages messages) {
        if (messages != null) {
            return messages.getFor(locale, "detail.chip.pageFormat", form.page(), form.pageSize());
        }
        return "Página " + form.page() + DOT_SEPARATOR + form.pageSize() + " por página";
    }

    private static String msg(UiMessages messages, Locale locale, String key, String fallback) {
        return messages != null ? messages.getFor(locale, key) : fallback;
    }
}
