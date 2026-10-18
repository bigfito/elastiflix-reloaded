package com.bigfito.elastiflix.web.home;

import java.util.ArrayList;
import java.util.List;

/**
 * Sugerencias vectoriales. La posición de cada película cambia con el campo.
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
    private static final List<String> FIELDS = List.of(PLOT_256, PLOT_512, PLOT_1024);

    private VectorSuggestions() {
    }

    /**
     * Un conjunto por campo de vector, con k 100 y 200 candidatos.
     *
     * @return los tres conjuntos, ocultos al empezar
     */
    static List<SuggestionSet> all() {
        List<SuggestionSet> sets = new ArrayList<>();
        for (String field : FIELDS) {
            sets.add(of(field));
        }
        return List.copyOf(sets);
    }

    private static SuggestionSet of(String field) {
        return new SuggestionSet("vectorial-" + field, false, List.of(
                item("un barco de lujo choca con un iceberg en su viaje inaugural",
                        "Titanic: " + titanic(field), "🚢", field),
                item("un ladrón roba secretos entrando en los sueños de las personas",
                        "Inception: " + inception(field), "🌀", field),
                item("un robot solitario que limpia la Tierra", "WALL·E: 1.º lugar", "🤖", field),
                item("a lonely robot cleaning up Earth", "en inglés · WALL·E: 1.º lugar", "🤖", field),
                item("un joven náufrago comparte un bote salvavidas con un tigre de Bengala en medio del océano",
                        "Life of Pi: " + lifeOfPi(field), "🐅", field),
                item("una niña queda atrapada en un mundo de espíritus y trabaja en una casa de baños "
                        + "para salvar a sus padres convertidos en cerdos",
                        "Spirited Away: " + spirited(field), "🏮", field),
                item("un niño esconde a un pequeño extraterrestre perdido y lo ayuda a volver a su planeta",
                        "E.T.: " + extraTerrestrial(field), "🚲", field)));
    }

    private static SuggestionLink item(String text, String tag, String emoji, String field) {
        return new SuggestionLink(text, tag, emoji, SearchHref.vector(text, field));
    }

    private static String titanic(String field) {
        if (PLOT_512.equals(field)) {
            return THIRD;
        }
        if (PLOT_1024.equals(field)) {
            return FIRST;
        }
        return FOURTH;
    }

    private static String inception(String field) {
        if (PLOT_512.equals(field)) {
            return FOURTH;
        }
        if (PLOT_1024.equals(field)) {
            return THIRD;
        }
        return "fuera del top 10";
    }

    private static String lifeOfPi(String field) {
        if (PLOT_512.equals(field)) {
            return THIRD;
        }
        if (PLOT_1024.equals(field)) {
            return SECOND;
        }
        return TENTH;
    }

    private static String spirited(String field) {
        if (PLOT_256.equals(field)) {
            return FOURTH;
        }
        return FIRST;
    }

    private static String extraTerrestrial(String field) {
        if (PLOT_256.equals(field)) {
            return THIRD;
        }
        return FIRST;
    }
}
