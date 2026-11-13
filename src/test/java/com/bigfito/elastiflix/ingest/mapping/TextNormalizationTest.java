package com.bigfito.elastiflix.ingest.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.core.model.MovieKeyword;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Al validar, el texto se recorta, el espacio duro pasa a espacio y el resultado queda en NFC.
 */
class TextNormalizationTest {

    private static final String PLOT = "Sinopsis de prueba suficientemente larga para validar.";
    private static final String HARD = "\u00a0";
    private static final JsonMapper MAPPER = MovieJsonMapper.create();

    private final MappingValidator validator = new MappingValidator();

    @Test
    @DisplayName("un título con espacios y espacio duro se guarda limpio y avisa una vez")
    void shouldWarnOnceAndStoreTheNormalizedTitle() {
        String title = "  Una" + HARD;
        ValidatedMovies result = validate(movie("title", json(title)));

        assertThat(result.report().errors()).isEmpty();
        assertThat(result.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field, ValidationIssue::received,
                        ValidationIssue::expected)
                .containsExactly(tuple(ErrorCode.TEXT_NORMALIZED.code(), "title", title,
                        IngestPhrase.NORMALIZED_TEXT.text()));
        assertThat(result.movies().getFirst().title()).isEqualTo("Una");
    }

    @Test
    @DisplayName("una lista, una palabra clave, una colección y la procedencia guardan el texto normalizado")
    void shouldNormalizeListItemsKeywordNamesAndLabels() {
        String json = """
                {"id":"1","title":"Una","plot":"%s",
                "cast":[" Ana%s","Ana"],
                "keywords":[{"id":1,"name":" beta "}],
                "belongs_to_collection":{"id":7,"name":"  Saga"},
                "enrichment":{"plot":" tmdb "}}
                """.formatted(PLOT, HARD);
        ValidatedMovies result = validate(json);

        assertThat(result.report().errors()).isEmpty();
        assertThat(result.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .containsExactly(
                        tuple(ErrorCode.TEXT_NORMALIZED.code(), "cast"),
                        tuple(ErrorCode.DUPLICATE_LIST_VALUE.code(), "cast"),
                        tuple(ErrorCode.TEXT_NORMALIZED.code(), "keywords.name"),
                        tuple(ErrorCode.TEXT_NORMALIZED.code(), "belongs_to_collection.name"),
                        tuple(ErrorCode.TEXT_NORMALIZED.code(), "enrichment.plot"));
        Movie movie = result.movies().getFirst();
        assertThat(movie.cast()).containsExactly("Ana");
        assertThat(movie.keywords()).singleElement().extracting(MovieKeyword::name).isEqualTo("beta");
        assertThat(movie.belongsToCollection().name()).isEqualTo("Saga");
        assertThat(movie.enrichment()).containsEntry("plot", "tmdb");
    }

    @Test
    @DisplayName("un texto que ya está limpio no avisa")
    void shouldStayQuietWhenTheTextIsAlreadyNormalized() {
        ValidatedMovies result = validate(movie("title", json("Una")));

        assertThat(result.report().warnings()).isEmpty();
        assertThat(result.movies().getFirst().title()).isEqualTo("Una");
    }

    @Test
    @DisplayName("los signos combinantes de otras escrituras se conservan: el virama de ಕನ್ನಡ y de हिन्दी no avisa")
    void shouldKeepCombiningMarksOfOtherScripts() {
        String kannada = "ಕನ್ನಡ";
        String hindi = "हिन्दी";
        ValidatedMovies result = validate(movie("spoken_languages", "[" + json(hindi) + "," + json(kannada) + "]"));

        assertThat(result.report().warnings()).isEmpty();
        assertThat(result.movies().getFirst().spokenLanguages()).containsExactly(hindi, kannada);
    }

    @Test
    @DisplayName("un texto que solo es espacio duro sigue la regla de vacío y no avisa de normalizar")
    void shouldUseTheEmptyWarningWhenOnlyAHardSpaceRemains() {
        ValidatedMovies result = validate(movie("tagline", json(HARD)));

        assertThat(result.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .containsExactly(tuple(ErrorCode.EMPTY_TEXT.code(), "tagline"));
        assertThat(result.movies().getFirst().tagline()).isNull();
    }

    @Test
    @DisplayName("la sinopsis se revisa ya normalizada y una forma que no es NFC se compone")
    void shouldCheckThePlotAfterNormalizationAndComposeNfc() {
        String plot = " " + PLOT + HARD;
        ValidatedMovies spaced = validate(movie("plot", json(plot)));
        assertThat(spaced.report().accepted()).isTrue();
        assertThat(spaced.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .containsExactly(tuple(ErrorCode.TEXT_NORMALIZED.code(), "plot"));
        assertThat(spaced.movies().getFirst().plot()).isEqualTo(PLOT);

        String decomposed = "Caf\u0065\u0301";
        ValidatedMovies composed = validate(movie("title", json(decomposed)));
        assertThat(composed.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .containsExactly(tuple(ErrorCode.TEXT_NORMALIZED.code(), "title"));
        assertThat(composed.movies().getFirst().title()).isEqualTo("Caf\u00e9");
    }

    private ValidatedMovies validate(String json) {
        JsonNode node = MAPPER.readTree(json);
        List<JsonNode> documents = new ArrayList<>();
        documents.add(node);
        return validator.validate(documents, Set.of());
    }

    private static String movie(String field, String value) {
        if ("title".equals(field)) {
            return "{\"id\":\"1\",\"title\":" + value + ",\"plot\":\"" + PLOT + "\"}";
        }
        if ("plot".equals(field)) {
            return "{\"id\":\"1\",\"title\":\"Una\",\"plot\":" + value + "}";
        }
        return "{\"id\":\"1\",\"title\":\"Una\",\"plot\":\"" + PLOT + "\",\"" + field + "\":" + value + "}";
    }

    private static String json(String text) {
        return MAPPER.writeValueAsString(text);
    }
}
