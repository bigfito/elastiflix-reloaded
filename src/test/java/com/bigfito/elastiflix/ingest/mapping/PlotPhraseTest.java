package com.bigfito.elastiflix.ingest.mapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.error.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Las reglas nuevas de {@code EFX-ING-025}, {@code 026} y {@code 028} contra los textos reales del catálogo.
 *
 * <p>Los cuatro id del plan están en {@code src/test/resources}, copiados del catálogo anterior a WP-01
 * ({@code dq-r00}): hoy {@code data/} ya tiene su trama de Wikipedia. Las frases que solo se parecen
 * («unable to find work», «the complexities of their relationships», «delves into the struggle») no avisan.</p>
 */
class PlotPhraseTest {

    private static final JsonMapper MAPPER = MovieJsonMapper.create();

    private final MappingValidator validator = new MappingValidator();

    @ParameterizedTest(name = "{0}")
    @MethodSource("assistantOpenings")
    @DisplayName("una negativa al inicio de la sinopsis rechaza el archivo y no avisa")
    void shouldRejectAnAssistantOpening(String id, String plot) {
        // when
        ValidatedMovies result = validate(plot);

        // then
        assertThat(result.report().accepted()).as(id).isFalse();
        assertThat(result.movies()).as(id).isEmpty();
        assertThat(result.report().warnings()).as(id).isEmpty();
        assertThat(result.report().errors())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .containsExactly(tuple(ErrorCode.PLACEHOLDER_PLOT.code(), "plot"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("modelLeftovers")
    @DisplayName("una corrección de año o un encabezado generado se avisa y la película se acepta")
    void shouldWarnAboutModelLeftovers(String id, String mark, String plot) {
        // when
        ValidatedMovies result = validate(plot);

        // then
        assertThat(result.report().errors()).as(id).isEmpty();
        assertThat(result.report().accepted()).as(id).isTrue();
        assertThat(result.movies()).as(id).hasSize(1);
        assertThat(result.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field, ValidationIssue::received)
                .containsExactly(tuple(ErrorCode.SUSPECT_PLOT.code(), "plot", mark));
        assertThat(result.movies().getFirst().plot()).isEqualTo(plot);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("themeEssays")
    @DisplayName("un ensayo de temas se avisa una sola vez y la película se acepta")
    void shouldWarnAboutAThemeEssay(String id, String mark, String plot) {
        // when
        ValidatedMovies result = validate(plot);

        // then
        assertThat(result.report().errors()).as(id).isEmpty();
        assertThat(result.report().accepted()).as(id).isTrue();
        assertThat(result.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field, ValidationIssue::received)
                .containsExactly(tuple(ErrorCode.ESSAY_PLOT.code(), "plot", mark));
        assertThat(result.movies().getFirst().plot()).isEqualTo(plot);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lookalikes")
    @DisplayName("una frase parecida, dentro de la trama o de Wikipedia, no se rechaza ni se avisa")
    void shouldKeepLookalikeProse(String id, String plot) {
        // when
        ValidatedMovies result = validate(plot);

        // then
        assertThat(result.report().warnings()).as(id).isEmpty();
        assertThat(result.report().errors()).as(id).isEmpty();
        assertThat(result.report().accepted()).as(id).isTrue();
        assertThat(result.movies().getFirst().plot()).isEqualTo(plot);
    }

    private static Stream<Arguments> assistantOpenings() throws IOException {
        return Stream.of(
                Arguments.of("671", read("plot-671.txt")),
                Arguments.of("799546", read("plot-799546.txt")),
                Arguments.of("please provide",
                        "Please provide the studio name before this note can describe the picture at all."),
                Arguments.of("do not have any",
                        "I do not have any information about this title in the sources checked today."),
                Arguments.of("dont",
                        "I dont have information about this title in the sources checked for the note today."),
                Arguments.of("no match found",
                        "[ no match found ]\n\nThe rest of this note still asks for a title and a studio."));
    }

    private static Stream<Arguments> modelLeftovers() throws IOException {
        return Stream.of(
                Arguments.of("12703", "is a 2003 film, not 2004", read("plot-12703.txt")),
                Arguments.of("was a", "was a 2014 film, not 2015",
                        "The note says the picture was a 2014 film, not 2015, and then follows the road trip west."),
                Arguments.of("please provide more", "please provide more information",
                        "The review closes with the line please provide more information about the missing reel."),
                Arguments.of("12444", "Here's a summary",
                        "Here's a summary of Harry Potter and the Deathly Hallows – Part 1: The movie begins with "
                                + "Harry Potter (Daniel Radcliffe) and his friends Ron Weasley (Rupert Grint) and "
                                + "Hermione Granger (Emma Watson) leaving Hogwarts School of Witchcraft and Wizardry."),
                Arguments.of("335983", "Here's a plot",
                        "Here's a plot summary of Venom (2018) by Marvel Entertainment: Venom is a 2018 American "
                                + "superhero science fiction film directed by Ruben Fleischer and written by Jeff "
                                + "Pinkner, Scott Rosenberg, and Kelly Marcel."),
                Arguments.of("75174", "Here's a detailed",
                        "Here's a detailed plot summary: The film follows Ottway (Liam Neeson), a former U."));
    }

    private static Stream<Arguments> themeEssays() throws IOException {
        return Stream.of(
                Arguments.of("621707", "At its core", read("plot-621707.txt")),
                Arguments.of("824", "explores the themes of",
                        "a beautiful and tragic love story that explores the themes of obsession, duty, and the "
                                + "power of art to transcend time and circumstance."),
                Arguments.of("9358", "explores the theme of",
                        "suspenseful horror film that explores the theme of fate and mortality."),
                Arguments.of("explore", "explore the theme of",
                        "The friends explore the theme of grief only in the song that closes the picture tonight."),
                Arguments.of("delves", "delves into the complexities",
                        "Later the narrator delves into the complexities of the family that cannot go home again."),
                Arguments.of("delve", "delve into the complexity",
                        "The closing chapter does delve into the complexity of the town they cannot leave behind."),
                Arguments.of("11887", "One of the primary themes",
                        "One of the primary themes of the movie is the bittersweet nature of senior year."),
                Arguments.of("central", "One of the central themes",
                        "One of the central themes of the picture is the house they cannot leave after the storm."));
    }

    private static Stream<Arguments> lookalikes() {
        return Stream.of(
                Arguments.of("212778",
                        "When Riva again demands the standard menu, Carl quits on the spot; Ramsey, served the same "
                                + "dishes, publicly mocks him, and Carl's furious confrontation at the restaurant goes "
                                + "viral, leaving him humiliated and unable to find work."),
                Arguments.of("167073",
                        "Unable to find steady work, she serves part-time in the shop of the spiteful Miss Kelly."),
                Arguments.of("36736",
                        "Foxxy believes their survival depends on getting their show back on the air, while Princess "
                                + "Clara refuses to accept that she is not a real princess and insists that her father "
                                + "can protect them."),
                Arguments.of("111083",
                        "Under pressure he confesses to Peña, but evidence against him accumulates, the watchman "
                                + "wakes to describe a masked gunman, and Peña tells him that there is no record of a "
                                + "woman named Carla at the address he provided."),
                Arguments.of("542",
                        "The film delves into the struggle priests have between desire and obedience."),
                Arguments.of("682532",
                        "As days turn into weeks, tension and desperation mount, revealing the complexities of their "
                                + "relationships and the true nature of their personalities."),
                Arguments.of("complejidades",
                        "The author also delves into themes of trauma, mental health, and the complexities of female "
                                + "relationships."),
                Arguments.of("explores the complexities",
                        "The film explores the complexities of human relationships and never lists them as themes."),
                Arguments.of("here's a story",
                        "Here's a story the family tells every winter, and it never pretends to be a plot summary."),
                Arguments.of("no match en medio",
                        "The detective writes no match found on the folder and keeps reading the case file tonight."),
                Arguments.of("799546 en medio",
                        "However, I do not have information on the specific details of the movie's storyline or its "
                                + "production."),
                Arguments.of("please provide en medio",
                        "The letter asks them to please provide the tickets before the curtain rises tonight."));
    }

    private static String read(String name) throws IOException {
        Path path = Path.of("src", "test", "resources",
                "com", "bigfito", "elastiflix", "ingest", "mapping", name);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private ValidatedMovies validate(String plot) {
        ObjectNode movie = MAPPER.createObjectNode();
        movie.put("id", "1");
        movie.put("title", "Una");
        movie.put("plot", plot);
        return validator.validate(List.of(movie), Set.of());
    }
}
