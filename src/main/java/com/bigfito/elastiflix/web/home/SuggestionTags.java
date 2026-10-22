package com.bigfito.elastiflix.web.home;

import java.util.Locale;

import com.bigfito.elastiflix.web.locale.UiMessages;

/**
 * Etiquetas de las sugerencias de Inicio en el idioma de la persona, desde {@code messages_*.properties}.
 *
 * <p>Las cifras (conteos y posiciones) son las medidas sobre el catálogo; llegan como texto porque son datos
 * fijos de cada sugerencia, no cálculos.</p>
 *
 * @param messages catálogo de mensajes
 * @param locale   idioma
 */
record SuggestionTags(UiMessages messages, Locale locale) {

    private static final String OR = "or";
    private static final String ONE = "1";
    private static final String OUTSIDE = "0";
    private static final int TEEN_START = 11;
    private static final int TEEN_END = 13;
    private static final int HUNDRED = 100;
    private static final int TEN = 10;
    private static final int THIRD = 3;
    private static final int OUTSIDE_K = 100;

    /**
     * Conteo de full-text según el operador, por ejemplo {@code OR: 57 resultados}.
     *
     * @param operator {@code or} o {@code and}
     * @param orCount  total con OR
     * @param andCount total con AND
     * @return la etiqueta
     */
    String count(String operator, String orCount, String andCount) {
        boolean or = OR.equals(operator);
        String count = or ? orCount : andCount;
        String key = (or ? "home.suggest.or." : "home.suggest.and.") + (ONE.equals(count) ? "one" : "many");
        return messages.getFor(locale, key, count);
    }

    /**
     * Conteo de multi-match: con AND las palabras quedan en campos distintos.
     *
     * @param operator {@code or} o {@code and}
     * @param orCount  total con OR
     * @return la etiqueta
     */
    String multi(String operator, String orCount) {
        if (OR.equals(operator)) {
            return count(operator, orCount, orCount);
        }
        return messages.getFor(locale, "home.suggest.and.differentFields");
    }

    /**
     * Etiqueta fija por clave, sin cifras.
     *
     * @param key {@code translatedTitle}, {@code titleAndPlot} o {@code andLexicalZero}
     * @return la etiqueta
     */
    String fixed(String key) {
        return messages.getFor(locale, "home.suggest." + key);
    }

    /**
     * Búsqueda por título original y cuántas películas da la misma consulta en {@code title}.
     *
     * @param inTitle total en {@code title}
     * @return la etiqueta
     */
    String originalTitle(String inTitle) {
        return messages.getFor(locale, "home.suggest.originalTitle", inTitle);
    }

    /**
     * Posición de una película en una búsqueda vectorial, por ejemplo {@code Titanic: 36.º lugar}.
     *
     * @param movie película
     * @param rank  puesto desde 1, o {@code 0} si quedó fuera de los 100
     * @return la etiqueta
     */
    String place(String movie, String rank) {
        String place = OUTSIDE.equals(rank)
                ? messages.getFor(locale, "home.suggest.outsideTop", String.valueOf(OUTSIDE_K))
                : messages.getFor(locale, "home.suggest.place", ordinal(rank));
        return messages.getFor(locale, "home.suggest.movie", movie, place);
    }

    /**
     * La misma película buscada en el otro idioma, por ejemplo {@code en inglés · WALL·E: 1.º lugar}.
     *
     * @param tag etiqueta de la película
     * @return la etiqueta
     */
    String otherLanguage(String tag) {
        return messages.getFor(locale, "home.suggest.otherLanguage", tag);
    }

    /**
     * Puesto de una película en las dos listas de la híbrida.
     *
     * @param movie   película
     * @param vector  puesto en la lista vectorial
     * @param lexical puesto en la lista léxica
     * @return la etiqueta
     */
    String hybrid(String movie, String vector, String lexical) {
        return messages.getFor(locale, "home.suggest.hybridRanks", movie, ordinal(vector), ordinal(lexical));
    }

    private String ordinal(String rank) {
        int number = Integer.parseInt(rank);
        int lastTwo = number % HUNDRED;
        String suffix = "th";
        if (lastTwo < TEEN_START || lastTwo > TEEN_END) {
            suffix = switch (number % TEN) {
                case 1 -> "st";
                case 2 -> "nd";
                case THIRD -> "rd";
                default -> "th";
            };
        }
        return messages.getFor(locale, "home.suggest.ordinal." + suffix, rank);
    }
}
