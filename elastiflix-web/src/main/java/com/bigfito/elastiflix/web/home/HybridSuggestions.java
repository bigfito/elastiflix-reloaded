package com.bigfito.elastiflix.web.home;

import java.util.List;

/**
 * Sugerencias híbridas. Con AND el léxico no alcanza y decide el vectorial.
 */
final class HybridSuggestions {

    private HybridSuggestions() {
    }

    /**
     * OR (título más trama) y AND (el léxico queda en cero).
     *
     * @return los dos conjuntos, ocultos al empezar
     */
    static List<SuggestionSet> all() {
        return List.of(or(), and());
    }

    private static SuggestionSet or() {
        return new SuggestionSet("hibrida-or", false, List.of(
                link("dark knight joker caos en la ciudad", "título + trama", "🦇", "or"),
                link("titanic historia de amor en un barco que se hunde",
                        "Titanic: 1.º vectorial, 2.º léxico", "🚢", "or"),
                link("ocean's eleven robo a casinos", "título + trama", "🎰", "or")));
    }

    private static SuggestionSet and() {
        String tag = "AND: léxico 0 · decide el vectorial";
        return new SuggestionSet("hibrida-and", false, List.of(
                link("toy story juguetes que cobran vida", tag, "🧸", "and"),
                link("laberinto fauno niña en la guerra", tag, "🌿", "and")));
    }

    private static SuggestionLink link(String text, String tag, String emoji, String operator) {
        return new SuggestionLink(text, tag, emoji, SearchHref.hybrid(text, operator));
    }
}
