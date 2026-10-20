package com.bigfito.elastiflix.web.home;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bigfito.elastiflix.web.locale.SupportedLocale;

/**
 * Sugerencias vectoriales en español e inglés. La posición de cada película cambia con el campo.
 */
final class VectorSuggestions {

    private static final String PLOT_256 = "plot_256";
    private static final String PLOT_512 = "plot_512";
    private static final String PLOT_1024 = "plot_1024";
    private static final String FIRST = "1.º lugar";
    private static final String SECOND = "2.º lugar";
    private static final String THIRD = "3.º lugar";
    private static final String FOURTH = "4.º lugar";
    private static final String TENTH = "10.º lugar";
    private static final String FIRST_EN = "1st place";
    private static final String SECOND_EN = "2nd place";
    private static final String THIRD_EN = "3rd place";
    private static final List<String> FIELDS = List.of(PLOT_256, PLOT_512, PLOT_1024);

    private VectorSuggestions() {
    }

    /**
     * Un conjunto por campo de vector, con k 100 y 200 candidatos.
     *
     * @param locale idioma destino
     * @return los tres conjuntos, ocultos al empezar
     */
    static List<SuggestionSet> all(Locale locale) {
        boolean en = SupportedLocale.isEnglish(locale);
        List<SuggestionSet> sets = new ArrayList<>();
        for (String field : FIELDS) {
            sets.add(en ? ofEnglish(field) : ofSpanish(field));
        }
        return List.copyOf(sets);
    }

    private static SuggestionSet ofSpanish(String field) {
        return new SuggestionSet("vectorial-" + field, false, List.of(
                item("un barco de lujo choca con un iceberg en su viaje inaugural",
                        "Titanic: " + titanicEs(field), "🚢", field),
                item("un ladrón roba secretos entrando en los sueños de las personas",
                        "Inception: " + inceptionEs(field), "🌀", field),
                item("un robot solitario que limpia la Tierra", "WALL·E: 1.º lugar", "🤖", field),
                item("a lonely robot cleaning up Earth", "en inglés · WALL·E: 1.º lugar", "🤖", field),
                item("un joven náufrago comparte un bote salvavidas con un tigre de Bengala en medio del océano",
                        "Life of Pi: " + lifeOfPiEs(field), "🐅", field),
                item("una niña queda atrapada en un mundo de espíritus y trabaja en una casa de baños "
                        + "para salvar a sus padres convertidos en cerdos",
                        "Spirited Away: " + spiritedEs(field), "🏮", field),
                item("un niño esconde a un pequeño extraterrestre perdido y lo ayuda a volver a su planeta",
                        "E.T.: " + extraTerrestrialEs(field), "🚲", field)));
    }

    private static SuggestionSet ofEnglish(String field) {
        return new SuggestionSet("vectorial-" + field, false, List.of(
                item("a luxury liner hits an iceberg on its maiden voyage",
                        "Titanic: " + titanicEn(field), "🚢", field),
                item("a thief steals secrets by entering people's dreams",
                        "Inception: " + inceptionEn(field), "🌀", field),
                item("a lonely robot cleaning up Earth", "WALL·E: 1st place", "🤖", field),
                item("un robot solitario que limpia la Tierra", "in Spanish · WALL·E: 1st place", "🤖", field),
                item("a shipwrecked boy shares a lifeboat with a Bengal tiger in the middle of the ocean",
                        "Life of Pi: " + lifeOfPiEn(field), "🐅", field),
                item("a girl is trapped in a spirit world and works in a bathhouse "
                        + "to save her parents, who were turned into pigs",
                        "Spirited Away: " + spiritedEn(field), "🏮", field),
                item("a boy hides a small lost alien and helps him get back to his planet",
                        "E.T.: " + extraTerrestrialEn(field), "🚲", field)));
    }

    private static SuggestionLink item(String text, String tag, String emoji, String field) {
        return new SuggestionLink(text, tag, emoji, SearchHref.vector(text, field));
    }

    private static String titanicEs(String field) {
        if (PLOT_512.equals(field)) {
            return THIRD;
        }
        if (PLOT_1024.equals(field)) {
            return FIRST;
        }
        return FOURTH;
    }

    private static String inceptionEs(String field) {
        if (PLOT_512.equals(field)) {
            return FOURTH;
        }
        if (PLOT_1024.equals(field)) {
            return THIRD;
        }
        return "fuera del top 10";
    }

    private static String lifeOfPiEs(String field) {
        if (PLOT_512.equals(field)) {
            return THIRD;
        }
        if (PLOT_1024.equals(field)) {
            return SECOND;
        }
        return TENTH;
    }

    private static String spiritedEs(String field) {
        if (PLOT_256.equals(field)) {
            return FOURTH;
        }
        return FIRST;
    }

    private static String extraTerrestrialEs(String field) {
        if (PLOT_256.equals(field)) {
            return THIRD;
        }
        return FIRST;
    }

    private static String titanicEn(String field) {
        if (PLOT_512.equals(field)) {
            return "5th place";
        }
        if (PLOT_1024.equals(field)) {
            return THIRD_EN;
        }
        return "36th place";
    }

    private static String inceptionEn(String field) {
        if (PLOT_512.equals(field)) {
            return "18th place";
        }
        if (PLOT_1024.equals(field)) {
            return "11th place";
        }
        return "outside the top 100";
    }

    private static String lifeOfPiEn(String field) {
        if (PLOT_256.equals(field)) {
            return THIRD_EN;
        }
        return FIRST_EN;
    }

    private static String spiritedEn(String field) {
        if (PLOT_512.equals(field)) {
            return SECOND_EN;
        }
        if (PLOT_1024.equals(field)) {
            return FIRST_EN;
        }
        return "6th place";
    }

    private static String extraTerrestrialEn(String field) {
        if (PLOT_512.equals(field)) {
            return THIRD_EN;
        }
        if (PLOT_1024.equals(field)) {
            return FIRST_EN;
        }
        return "7th place";
    }
}
