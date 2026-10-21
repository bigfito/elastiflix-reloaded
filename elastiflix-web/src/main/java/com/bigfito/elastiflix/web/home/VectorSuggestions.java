package com.bigfito.elastiflix.web.home;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bigfito.elastiflix.web.locale.SupportedLocale;

/**
 * Sugerencias vectoriales en español e inglés. La posición de cada película cambia con el campo.
 *
 * <p>Posiciones medidas el 2026-10-21 sobre 6 899 películas, con {@code k = 100} y 200 candidatos.</p>
 */
final class VectorSuggestions {

    private static final String PLOT_256 = "plot_256";
    private static final String PLOT_512 = "plot_512";
    private static final String PLOT_1024 = "plot_1024";
    private static final String FIRST = "1.º lugar";
    private static final String FIRST_EN = "1st place";
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
        return byField(field, "36.º lugar", "7.º lugar", "4.º lugar");
    }

    private static String inceptionEs(String field) {
        return byField(field, "94.º lugar", "13.º lugar", "8.º lugar");
    }

    private static String lifeOfPiEs(String field) {
        return byField(field, "31.º lugar", "7.º lugar", "5.º lugar");
    }

    private static String spiritedEs(String field) {
        return byField(field, "13.º lugar", "4.º lugar", FIRST);
    }

    private static String extraTerrestrialEs(String field) {
        return byField(field, "3.º lugar", FIRST, FIRST);
    }

    private static String titanicEn(String field) {
        return byField(field, "36th place", "5th place", "3rd place");
    }

    private static String inceptionEn(String field) {
        return byField(field, "outside the top 100", "18th place", "11th place");
    }

    private static String lifeOfPiEn(String field) {
        return byField(field, "3rd place", FIRST_EN, FIRST_EN);
    }

    private static String spiritedEn(String field) {
        return byField(field, "6th place", "2nd place", FIRST_EN);
    }

    private static String extraTerrestrialEn(String field) {
        return byField(field, "7th place", "3rd place", FIRST_EN);
    }

    private static String byField(String field, String at256, String at512, String at1024) {
        if (PLOT_512.equals(field)) {
            return at512;
        }
        if (PLOT_1024.equals(field)) {
            return at1024;
        }
        return at256;
    }
}
