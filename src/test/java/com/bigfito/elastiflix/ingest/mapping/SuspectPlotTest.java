package com.bigfito.elastiflix.ingest.mapping;

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
 * EFX-ING-026 distingue un resto de asistente de la prosa que el catálogo dejó a propósito.
 */
class SuspectPlotTest {

    private static final JsonMapper MAPPER = MovieJsonMapper.create();

    private final MappingValidator validator = new MappingValidator();

    @ParameterizedTest(name = "{0}")
    @MethodSource("artifactsFromMain")
    @DisplayName("un resto que main sí marcaba se avisa y la película se acepta")
    void shouldWarnAboutArtifactsThatMainFlagged(String mark, String plot) {
        ValidatedMovies result = validate(plot);

        assertThat(result.report().accepted()).isTrue();
        assertThat(result.movies()).hasSize(1);
        assertThat(result.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field, ValidationIssue::received)
                .containsExactly(tuple(ErrorCode.SUSPECT_PLOT.code(), "plot", mark));
        assertThat(result.movies().getFirst().plot()).isEqualTo(plot);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("legitimateProse")
    @DisplayName("la prosa de las doce sinopsis que quedan no se avisa")
    void shouldKeepLegitimateProse(String id, String plot) {
        ValidatedMovies result = validate(plot);

        assertThat(result.report().warnings()).as(id).isEmpty();
        assertThat(result.report().accepted()).isTrue();
        assertThat(result.movies().getFirst().plot()).isEqualTo(plot);
    }

    private static Stream<Arguments> artifactsFromMain() {
        return Stream.of(
                Arguments.of("I couldn't find",
                        "directed by Chris Kentis, not Plunge Pictures LLC "
                                + "(I couldn't find any information on this production company)."),
                Arguments.of("find any information",
                        "Her stories were often inconsistent, and she couldn't find any information "
                                + "about his business dealings or wealth online."),
                Arguments.of("knowledge cutoff",
                        "The release year moved from 2012 to 2011 as per my knowledge cutoff date."),
                Arguments.of("I could not find",
                        "I could not find a production company for this synopsis in any source I checked."),
                Arguments.of("I was unable to find",
                        "I was unable to find a reliable overview, so this synopsis stops here today."),
                Arguments.of(", not Bandai",
                        "Zatoichi is a 2003 Japanese martial arts film directed by Kinji Fukasaku, not Bandai "
                                + "Visual."),
                Arguments.of(", not Toho.",
                        "Pokémon Ranger and the Temple of the Sea is a 2006 animated film produced by OLM, Inc., "
                                + "not Toho."),
                Arguments.of(", not Agent",
                        "directed by Michael Finch and written by Skip Woods, based on the video game series of "
                                + "the same name. The movie stars Hugh Jackman as Adam Jensen, a cyborg assassin, "
                                + "not Agent 47 (the main character in the game series)."),
                Arguments.of("**",
                        "The turning points of this drama include: 1. **Grief and Loss**: the story follows a family."),
                Arguments.of("# ",
                        "Notes about the film follow.\n# Cast\nThe names below belong in the synopsis itself."));
    }

    private static Stream<Arguments> legitimateProse() {
        return Stream.of(
                Arguments.of("1597",
                        "Pam produces his exam results, and it emerges that the CIA could not find him "
                                + "because his real name is Gaylord."),
                Arguments.of("167073",
                        "Unable to find steady work, she serves part-time in the shop of the spiteful Miss Kelly."),
                Arguments.of("212778",
                        "Carl's furious confrontation at the restaurant goes viral, leaving him humiliated "
                                + "and unable to find work."),
                Arguments.of("317925",
                        "María Laura, disillusioned with men and unable to find acting work, earns a living "
                                + "by putting her skills to a different use."),
                Arguments.of("581726",
                        "Evan McCauley, diagnosed with schizophrenia and unable to find work because of his past, "
                                + "forges a katana for a gangster."),
                Arguments.of("628914",
                        "JoJo Johnson has been unable to find work since her identity was stolen and lives "
                                + "with her mother while trying to earn money with YouTube videos."),
                Arguments.of("845140",
                        "Unable to find a job that matches her skills, she reluctantly accepts a post as the cook "
                                + "at the canteen of a shelter for young migrants."),
                Arguments.of("11283",
                        "Desperate and unable to find anyone suitable, Mrs. Grimstone reluctantly accepts "
                                + "the help of Nanny McPhee."),
                Arguments.of("1574",
                        "In a shocking twist, it is revealed that Velma was actually the one who killed Amos Hart, "
                                + "not Roxie."),
                Arguments.of("4641",
                        "Jamie discovers that the poems she admired in class were written by Connor, not Marco, "
                                + "and the two share a kiss while Lenny performs a song for her."),
                Arguments.of("5955",
                        "A drawing by Ginny shows the man with a black station wagon, not Toby's red pickup, "
                                + "and Jerry uncovers similar unsolved killings of young girls in the region."),
                Arguments.of("242042",
                        "Jay learns that it was her mother, not Daisy, who heard voices, and that Daisy blames "
                                + "herself for her mother's death."),
                Arguments.of("786300",
                        "In its final act the film reveals that Sam, not Tara, is the one in danger, and their "
                                + "consuming romance turns into a tale of transformation and aquatic body horror."));
    }

    private ValidatedMovies validate(String plot) {
        ObjectNode movie = MAPPER.createObjectNode();
        movie.put("id", "1");
        movie.put("title", "Una");
        movie.put("plot", plot);
        return validator.validate(List.of(movie), Set.of());
    }
}
