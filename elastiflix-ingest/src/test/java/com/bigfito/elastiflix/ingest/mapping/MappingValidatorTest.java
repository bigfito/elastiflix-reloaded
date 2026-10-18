package com.bigfito.elastiflix.ingest.mapping;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.ingest.csv.CsvMovieReader;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.json.JsonMovieReader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Las variantes inválidas producen el error de su caso y los fixtures válidos ninguno.
 */
class MappingValidatorTest {

    private static final Path JSON = Path.of("..", "elasticsearch", "fixtures", "movies-sample.json");
    private static final Path CSV = Path.of("..", "elasticsearch", "fixtures", "movies-sample.csv");
    private static final JsonMapper MAPPER = MovieJsonMapper.create();

    private final MappingValidator validator = new MappingValidator();

    @Test
    @DisplayName("el JSON de cuatro películas no tiene errores y avisa vectores y el texto vacío")
    void shouldAcceptTheJsonFixture() {
        ValidatedMovies result = validator.validate(documentsOf(JSON), Set.of("597", "999"));

        assertThat(result.report().errors()).isEmpty();
        assertThat(result.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .contains(
                        tuple(ErrorCode.VECTORS_IGNORED.code(), "plot_256"),
                        tuple(ErrorCode.VECTORS_IGNORED.code(), "plot_512"),
                        tuple(ErrorCode.VECTORS_IGNORED.code(), "plot_1024"),
                        tuple(ErrorCode.EMPTY_TEXT.code(), "homepage"));
        assertThat(result.replaceCount()).isEqualTo(1);
        assertThat(result.movies()).allSatisfy(movie -> {
            assertThat(movie.plot256()).isNull();
            assertThat(movie.plot512()).isNull();
            assertThat(movie.plot1024()).isNull();
        });
        assertThat(result.movies()).usingRecursiveComparison()
                .withEqualsForType(Arrays::equals, float[].class)
                .isEqualTo(new JsonMovieReader().read(JSON));
    }

    @Test
    @DisplayName("el CSV de cuatro películas, ya leído, tampoco tiene errores de tipo")
    void shouldAcceptTheCsvFixture() {
        List<JsonNode> documents = new CsvMovieReader(new IndexMapping())
                .read(CSV, CsvSettings.withHeader())
                .documents();

        ValidatedMovies result = validator.validate(documents, Set.of());

        assertThat(result.report().errors()).isEmpty();
        assertThat(result.movies()).hasSize(4);
    }

    @ParameterizedTest(name = "{0} → {1} en {2}")
    @MethodSource("invalidFiles")
    @DisplayName("cada variante inválida produce exactamente su error")
    void shouldReportTheExpectedError(String resource, String code, String field) throws Exception {
        ValidatedMovies result = validator.validate(resource(resource), Set.of());

        assertThat(result.movies()).isEmpty();
        assertThat(result.report().errors())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .containsExactly(tuple(code, field));
    }

    @Test
    @DisplayName("un null opcional no avisa y un hueco de lista sí")
    void shouldWarnOnlyForAHoleInsideAList() {
        JsonNode documents = MAPPER.readTree("""
                [{"id":"1","title":"Una","plot":"texto","homepage":null,"genres":["Drama",""]}]
                """);

        ValidatedMovies result = validator.validate(children(documents), Set.of());

        assertThat(result.report().errors()).isEmpty();
        assertThat(result.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .containsExactly(tuple(ErrorCode.EMPTY_LIST_VALUE.code(), "genres"));
        assertThat(result.movies().getFirst().genres()).containsExactly("Drama");
    }

    @Test
    @DisplayName("el CSV del informe trae solo los errores y entrecomilla las comas")
    void shouldExportErrorsAsCsv() {
        ValidationIssue issue = new ValidationIssue(
                ErrorCode.TYPE_MISMATCH.code(), "1", "7", "plot", "hola, mundo", "un texto", false);
        ValidationIssue warning = new ValidationIssue(
                ErrorCode.EMPTY_TEXT.code(), "1", "7", "tagline", "en blanco", "sin valor", true);

        String csv = new ValidationReport(List.of(issue, warning)).toErrorCsv();

        assertThat(csv).isEqualTo("""
                registro,id,campo,recibido,esperado
                1,7,plot,"hola, mundo",un texto
                """);
    }

    private static Stream<Arguments> invalidFiles() {
        return Stream.of(
                Arguments.arguments("/validation/bad-date.json", ErrorCode.TYPE_MISMATCH.code(), "release_date"),
                Arguments.arguments("/validation/bad-boolean.json", ErrorCode.TYPE_MISMATCH.code(), "adult"),
                Arguments.arguments("/validation/bad-integer.json", ErrorCode.TYPE_MISMATCH.code(), "runtime"),
                Arguments.arguments("/validation/bad-nested.json", ErrorCode.UNKNOWN_FIELD.code(), "director"),
                Arguments.arguments("/validation/duplicate-id.json", ErrorCode.DUPLICATE_ID.code(), "id"),
                Arguments.arguments("/validation/id-mismatch.json", ErrorCode.ID_MISMATCH.code(), "_id"),
                Arguments.arguments("/validation/blank-title.json", ErrorCode.REQUIRED_FIELD.code(), "title"));
    }

    private static List<JsonNode> documentsOf(Path path) {
        return children(MAPPER.readTree(path).get("documents"));
    }

    private static List<JsonNode> resource(String name) throws Exception {
        try (InputStream input = MappingValidatorTest.class.getResourceAsStream(name)) {
            return children(MAPPER.readTree(input));
        }
    }

    private static List<JsonNode> children(JsonNode array) {
        List<JsonNode> documents = new ArrayList<>();
        for (JsonNode item : array) {
            documents.add(item);
        }
        return documents;
    }
}
