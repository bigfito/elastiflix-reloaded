package com.bigfito.elastiflix.web.search;

import java.util.Locale;

import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.web.locale.UiMessages;

/**
 * Leyendas del mockup de resultados, una por modo y traducidas según el idioma activo.
 */
public final class ModeCopies {

    private ModeCopies() {
    }

    /**
     * Textos del modo de la consulta en el idioma y con el catálogo indicados.
     *
     * @param query    consulta
     * @param locale   idioma destino
     * @param messages catálogo de mensajes
     * @return chip, orden, leyenda y nota de límite
     */
    public static ModeCopy of(SearchQuery query, Locale locale, UiMessages messages) {
        if (query.mode() == SearchMode.VECTOR) {
            return vector(query.knn().k(), locale, messages);
        }
        if (query.mode() == SearchMode.HYBRID) {
            return hybrid(query.knn().k(), locale, messages);
        }
        return new ModeCopy(
                messages.getFor(locale, "results.mode.fulltext.label"),
                "tone-sky",
                messages.getFor(locale, "results.mode.fulltext.orderedBy"),
                messages.getFor(locale, "results.mode.fulltext.legend"),
                "",
                messages.getFor(locale, "results.mode.fulltext.scoreTitle"));
    }

    private static ModeCopy vector(int k, Locale locale, UiMessages messages) {
        String limit = messages.getFor(locale, "results.mode.vector.limit", k);
        return new ModeCopy(
                messages.getFor(locale, "results.mode.vector.label"),
                "tone-mint",
                messages.getFor(locale, "results.mode.vector.orderedBy"),
                messages.getFor(locale, "results.mode.vector.legend"),
                limit,
                messages.getFor(locale, "results.mode.vector.scoreTitle"));
    }

    private static ModeCopy hybrid(int k, Locale locale, UiMessages messages) {
        String limit = messages.getFor(locale, "results.mode.hybrid.limit", k, k);
        return new ModeCopy(
                messages.getFor(locale, "results.mode.hybrid.label"),
                "tone-lavender",
                messages.getFor(locale, "results.mode.hybrid.orderedBy"),
                messages.getFor(locale, "results.mode.hybrid.legend"),
                limit,
                messages.getFor(locale, "results.mode.hybrid.scoreTitle"));
    }
}

