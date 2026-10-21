package com.bigfito.elastiflix.web.home;

import java.util.List;
import java.util.Locale;

import com.bigfito.elastiflix.web.locale.SupportedLocale;

/**
 * Sugerencias híbridas en español e inglés. Con AND el léxico no alcanza y decide el vectorial.
 */
final class HybridSuggestions {

    private static final String OR = "or";
    private static final String AND = "and";
    private static final String KEY_OR = "hibrida-or";
    private static final String KEY_AND = "hibrida-and";

    private HybridSuggestions() {
    }

    /**
     * OR (título más trama) y AND (el léxico queda en cero).
     *
     * @param locale idioma destino
     * @return los dos conjuntos, ocultos al empezar
     */
    static List<SuggestionSet> all(Locale locale) {
        boolean en = SupportedLocale.isEnglish(locale);
        return en ? List.of(orEn(), andEn()) : List.of(orEs(), andEs());
    }

    private static SuggestionSet orEs() {
        return new SuggestionSet(KEY_OR, false, List.of(
                link("dark knight joker caos en la ciudad", "título + trama", "🦇", OR),
                link("titanic historia de amor en un barco que se hunde",
                        "Titanic: 1.º vectorial, 1.º léxico", "🚢", OR),
                link("ocean's eleven robo a casinos", "título + trama", "🎰", OR)));
    }

    private static SuggestionSet andEs() {
        String tag = "AND: léxico 0 · decide el vectorial";
        return new SuggestionSet(KEY_AND, false, List.of(
                link("toy story juguetes que cobran vida", tag, "🧸", AND),
                link("laberinto fauno niña en la guerra", tag, "🌿", AND)));
    }

    private static SuggestionSet orEn() {
        String titleAndPlot = "title + plot";
        return new SuggestionSet(KEY_OR, false, List.of(
                link("dark knight joker chaos in the city", titleAndPlot, "🦇", OR),
                link("titanic love story on a sinking ship",
                        "Titanic: 1st vector, 4th lexical", "🚢", OR),
                link("ocean's eleven casino heist", titleAndPlot, "🎰", OR)));
    }

    private static SuggestionSet andEn() {
        String tag = "AND: lexical 0 · vector decides";
        return new SuggestionSet(KEY_AND, false, List.of(
                link("toy story toys that come to life", tag, "🧸", AND),
                link("pan's labyrinth faun girl in the war", tag, "🌿", AND)));
    }

    private static SuggestionLink link(String text, String tag, String emoji, String operator) {
        return new SuggestionLink(text, tag, emoji, SearchHref.hybrid(text, operator));
    }
}
