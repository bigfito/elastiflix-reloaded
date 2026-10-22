package com.bigfito.elastiflix.web.home;

import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.web.locale.SupportedLocale;

/**
 * Sugerencias vectoriales en español e inglés. La posición de cada película cambia con el campo.
 *
 * <p>Posiciones medidas el 2026-10-21 sobre 6 899 películas, con {@code k = 100} y 200 candidatos, en el orden
 * {@code plot_256 plot_512 plot_1024}; {@code 0} es fuera de los 100. Las consultas son datos de cada idioma;
 * las etiquetas salen del catálogo de mensajes.</p>
 */
final class VectorSuggestions {

    private static final List<String> FIELDS = List.of("plot_256", "plot_512", "plot_1024");
    private static final String WALL_E = "WALL·E";
    private static final String ROBOT_ES = "un robot solitario que limpia la Tierra";
    private static final String ROBOT_EN = "a lonely robot cleaning up Earth";
    private static final String ROBOT = "🤖";
    private static final String FIRST_EVERYWHERE = "1 1 1";

    private static final List<Example> SPANISH = List.of(
            new Example("Titanic", "🚢", "un barco de lujo choca con un iceberg en su viaje inaugural", "36 7 4", false),
            new Example("Inception", "🌀", "un ladrón roba secretos entrando en los sueños de las personas",
                    "94 13 8", false),
            new Example(WALL_E, ROBOT, ROBOT_ES, FIRST_EVERYWHERE, false),
            new Example(WALL_E, ROBOT, ROBOT_EN, FIRST_EVERYWHERE, true),
            new Example("Life of Pi", "🐅",
                    "un joven náufrago comparte un bote salvavidas con un tigre de Bengala en medio del océano",
                    "31 7 5", false),
            new Example("Spirited Away", "🏮", "una niña queda atrapada en un mundo de espíritus y trabaja en una "
                    + "casa de baños para salvar a sus padres convertidos en cerdos", "13 4 1", false),
            new Example("E.T.", "🚲",
                    "un niño esconde a un pequeño extraterrestre perdido y lo ayuda a volver a su planeta",
                    "3 1 1", false));

    private static final List<Example> ENGLISH = List.of(
            new Example("Titanic", "🚢", "a luxury liner hits an iceberg on its maiden voyage", "36 5 3", false),
            new Example("Inception", "🌀", "a thief steals secrets by entering people's dreams", "0 18 11", false),
            new Example(WALL_E, ROBOT, ROBOT_EN, FIRST_EVERYWHERE, false),
            new Example(WALL_E, ROBOT, ROBOT_ES, FIRST_EVERYWHERE, true),
            new Example("Life of Pi", "🐅",
                    "a shipwrecked boy shares a lifeboat with a Bengal tiger in the middle of the ocean",
                    "3 1 1", false),
            new Example("Spirited Away", "🏮", "a girl is trapped in a spirit world and works in a bathhouse "
                    + "to save her parents, who were turned into pigs", "6 2 1", false),
            new Example("E.T.", "🚲", "a boy hides a small lost alien and helps him get back to his planet",
                    "7 3 1", false));

    private VectorSuggestions() {
    }

    /**
     * Un conjunto por campo de vector, con k 100 y 200 candidatos.
     *
     * @param tags etiquetas en el idioma de la persona
     * @return los tres conjuntos, ocultos al empezar
     */
    static List<SuggestionSet> all(SuggestionTags tags) {
        List<Example> examples = SupportedLocale.isEnglish(tags.locale()) ? ENGLISH : SPANISH;
        List<SuggestionSet> sets = new ArrayList<>();
        for (int index = 0; index < FIELDS.size(); index++) {
            String field = FIELDS.get(index);
            List<SuggestionLink> links = new ArrayList<>();
            for (Example example : examples) {
                links.add(example.link(field, index, tags));
            }
            sets.add(new SuggestionSet("vectorial-" + field, false, List.copyOf(links)));
        }
        return List.copyOf(sets);
    }

    /**
     * Una consulta de ejemplo y la posición medida de su película en cada campo.
     *
     * @param movie         película que debería encontrar
     * @param emoji         icono de la sugerencia
     * @param query         consulta
     * @param ranks         posiciones en {@code plot_256 plot_512 plot_1024}, separadas por espacios
     * @param otherLanguage {@code true} si la consulta está en el otro idioma
     */
    private record Example(String movie, String emoji, String query, String ranks, boolean otherLanguage) {

        SuggestionLink link(String field, int index, SuggestionTags tags) {
            String tag = tags.place(movie, ranks.split(" ")[index]);
            if (otherLanguage) {
                tag = tags.otherLanguage(tag);
            }
            return new SuggestionLink(query, tag, emoji, SearchHref.vector(query, field));
        }
    }
}
