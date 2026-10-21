package com.bigfito.elastiflix.web.home;

import java.util.List;

import com.bigfito.elastiflix.web.locale.SupportedLocale;

/**
 * Sugerencias híbridas en español e inglés. Con AND el léxico no alcanza y decide el vectorial.
 *
 * <p>Las consultas son datos de cada idioma y la fila de Titanic lleva sus puestos medidos el 2026-10-21; las
 * etiquetas salen del catálogo de mensajes.</p>
 */
final class HybridSuggestions {

    private static final String OR = "or";
    private static final String AND = "and";
    private static final String TITLE_AND_PLOT = "titleAndPlot";
    private static final String LEXICAL_ZERO = "andLexicalZero";

    private HybridSuggestions() {
    }

    /**
     * OR (título más trama) y AND (el léxico queda en cero).
     *
     * @param tags etiquetas en el idioma de la persona
     * @return los dos conjuntos, ocultos al empezar
     */
    static List<SuggestionSet> all(SuggestionTags tags) {
        if (SupportedLocale.isEnglish(tags.locale())) {
            return List.of(
                    or(tags, "dark knight joker chaos in the city", "titanic love story on a sinking ship", "4",
                            "ocean's eleven casino heist"),
                    and(tags, "toy story toys that come to life", "pan's labyrinth faun girl in the war"));
        }
        return List.of(
                or(tags, "dark knight joker caos en la ciudad", "titanic historia de amor en un barco que se hunde",
                        "1", "ocean's eleven robo a casinos"),
                and(tags, "toy story juguetes que cobran vida", "laberinto fauno niña en la guerra"));
    }

    private static SuggestionSet or(SuggestionTags tags, String batman, String titanic, String lexical, String heist) {
        return new SuggestionSet("hibrida-or", false, List.of(
                link(batman, tags.fixed(TITLE_AND_PLOT), "🦇", OR),
                link(titanic, tags.hybrid("Titanic", "1", lexical), "🚢", OR),
                link(heist, tags.fixed(TITLE_AND_PLOT), "🎰", OR)));
    }

    private static SuggestionSet and(SuggestionTags tags, String toys, String faun) {
        return new SuggestionSet("hibrida-and", false, List.of(
                link(toys, tags.fixed(LEXICAL_ZERO), "🧸", AND),
                link(faun, tags.fixed(LEXICAL_ZERO), "🌿", AND)));
    }

    private static SuggestionLink link(String text, String tag, String emoji, String operator) {
        return new SuggestionLink(text, tag, emoji, SearchHref.hybrid(text, operator));
    }
}
