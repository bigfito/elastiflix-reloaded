package com.bigfito.elastiflix.ingest.mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.error.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * EFX-ING-028 avisa de un ensayo de temas y deja pasar la prosa que solo menciona temas.
 */
class EssayPlotTest {

    private static final JsonMapper MAPPER = MovieJsonMapper.create();

    private final MappingValidator validator = new MappingValidator();

    @ParameterizedTest(name = "{0}")
    @MethodSource("essays")
    @DisplayName("un ensayo de temas se avisa y la película se acepta")
    void shouldWarnAboutAThemeEssay(String mark, String plot) {
        ValidatedMovies result = validate(plot);

        assertThat(result.report().errors()).isEmpty();
        assertThat(result.report().accepted()).isTrue();
        assertThat(result.movies()).hasSize(1);
        assertThat(result.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field, ValidationIssue::received)
                .containsExactly(tuple(ErrorCode.ESSAY_PLOT.code(), "plot", mark));
        assertThat(result.movies().getFirst().plot()).isEqualTo(plot);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("prose")
    @DisplayName("mencionar temas en la prosa no se avisa")
    void shouldKeepProseThatMentionsThemes(String id, String plot) {
        ValidatedMovies result = validate(plot);

        assertThat(result.report().warnings()).as(id).isEmpty();
        assertThat(result.report().errors()).as(id).isEmpty();
        assertThat(result.report().accepted()).isTrue();
        assertThat(result.movies().getFirst().plot()).isEqualTo(plot);
    }

    @Test
    @DisplayName("un ensayo con markdown avisa las dos cosas y no rechaza la película")
    void shouldWarnAboutAnEssayAndLeftoversWithoutRejecting() {
        String plot = "The main themes of this drama include 1. **Grief and Loss** and a family that stays.";

        ValidatedMovies result = validate(plot);

        assertThat(result.report().errors()).isEmpty();
        assertThat(result.report().accepted()).isTrue();
        assertThat(result.movies()).hasSize(1);
        assertThat(result.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::received)
                .containsExactly(
                        tuple(ErrorCode.SUSPECT_PLOT.code(), "**"),
                        tuple(ErrorCode.ESSAY_PLOT.code(), "main themes"));
        assertThat(result.movies().getFirst().plot()).isEqualTo(plot);
    }

    private static Stream<Arguments> essays() {
        return Stream.of(
                Arguments.of("main themes",
                        "One of the main themes of this story is grief that never quite leaves the town."),
                Arguments.of("MAIN THEMES",
                        "MAIN THEMES of the picture are grief and the long winter that follows the flood."),
                Arguments.of("themes emerge:",
                        "Two friends drift apart and several themes emerge:"),
                Arguments.of("themes, including:",
                        "The film explores several themes, including:\nfamily, work and the long road home."),
                Arguments.of("The main theme of",
                        "The main theme of the film revolves around a family that cannot go home again."),
                Arguments.of("the central theme of",
                        "After the storm, the central theme of the picture is the house they cannot leave."),
                Arguments.of("Themes:",
                        "Themes: friendship and loss keep this small town story moving forward."),
                Arguments.of("Key themes:",
                        "Key themes: friendship and loss keep this small town story moving forward."),
                Arguments.of("Major themes:",
                        "Major themes: a quiet town learns to share one long winter together."));
    }

    private static Stream<Arguments> prose() {
        return Stream.of(
                Arguments.of("62320",
                        "Sweeping images of deserts, forests, ice caps, farmland and megacities illustrate its "
                                + "central themes: water shortages, deforestation, melting ice, the depletion of "
                                + "natural resources and the impact of human activity on climate change."),
                Arguments.of("12",
                        "The film features stunning animation, lovable characters, and a story that explores "
                                + "themes of parenthood, friendship, and perseverance."),
                Arguments.of("prosa",
                        "The theme song plays while the family drives home after the storm."));
    }

    private ValidatedMovies validate(String plot) {
        ObjectNode movie = MAPPER.createObjectNode();
        movie.put("id", "1");
        movie.put("title", "Una");
        movie.put("plot", plot);
        return validator.validate(List.of(movie), Set.of());
    }
}
