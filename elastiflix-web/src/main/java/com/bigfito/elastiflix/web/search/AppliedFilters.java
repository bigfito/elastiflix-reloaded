package com.bigfito.elastiflix.web.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bigfito.elastiflix.web.locale.SupportedLocale;
import com.bigfito.elastiflix.web.locale.UiMessages;

/**
 * Chips y nota de los filtros que ya están en la URL, adaptados al idioma activo.
 */
public final class AppliedFilters {

    private AppliedFilters() {
    }

    /**
     * Textos de la fila «Filtros aplicados» en el idioma del contexto actual, con un catálogo propio.
     *
     * <p>Transitorio (revisión 4, U-18): solo lo llama la rama sin catálogo de {@code MovieDetailSearchChips},
     * que el carril del detalle borra. Al integrar los carriles se borra también este método.</p>
     *
     * @param form URL actual
     * @return chips; vacío si no hay filtros
     */
    public static List<String> chips(SearchForm form) {
        return chips(form, SupportedLocale.current(), new UiMessages(UiMessages.createMessageSource()));
    }

    /**
     * Textos de la fila «Filtros aplicados» en el idioma y con el catálogo indicados.
     *
     * @param form     URL actual
     * @param locale   idioma destino
     * @param messages catálogo de mensajes
     * @return chips; vacío si no hay filtros
     */
    public static List<String> chips(SearchForm form, Locale locale, UiMessages messages) {
        List<String> chips = new ArrayList<>();
        adult(chips, form.adult(), locale, messages);
        labeled(chips, messages.getFor(locale, "results.chip.genres"), form.genres(), locale, messages);
        dates(chips, form, locale, messages);
        labeled(chips, messages.getFor(locale, "results.chip.languages"), form.languages(), locale, messages);
        labeled(chips, messages.getFor(locale, "results.chip.rating"), form.ratings(), locale, messages);
        return List.copyOf(chips);
    }

    /**
     * Nota bajo el título de filtros en el idioma y con el catálogo indicados.
     *
     * @param form     URL actual
     * @param locale   idioma destino
     * @param messages catálogo de mensajes
     * @return el texto del mockup
     */
    public static String note(SearchForm form, Locale locale, UiMessages messages) {
        if (chips(form, locale, messages).isEmpty()) {
            return messages.getFor(locale, "results.filters.note.default");
        }
        return messages.getFor(locale, "results.filters.note.filtered");
    }

    private static void adult(List<String> chips, List<String> adult, Locale locale, UiMessages messages) {
        if (adult.contains("false")) {
            chips.add(messages.getFor(locale, "results.chip.adult.safe"));
        } else if (adult.contains("true")) {
            chips.add(messages.getFor(locale, "results.chip.adult.yes"));
        }
    }

    private static void labeled(
            List<String> chips, String prefix, List<String> values, Locale locale, UiMessages messages) {
        if (!values.isEmpty()) {
            // Properties descarta los espacios iniciales de un valor: el catálogo guarda solo la palabra.
            String or = " " + messages.getFor(locale, "results.chip.or").strip() + " ";
            chips.add(prefix + String.join(or, values));
        }
    }

    private static void dates(List<String> chips, SearchForm form, Locale locale, UiMessages messages) {
        if (form.from().isBlank() && form.to().isBlank()) {
            return;
        }
        chips.add(messages.getFor(locale, "results.chip.date") + shown(form.from()) + " → " + shown(form.to()));
    }

    private static String shown(String value) {
        if (value.isBlank()) {
            return "…";
        }
        return value;
    }
}

