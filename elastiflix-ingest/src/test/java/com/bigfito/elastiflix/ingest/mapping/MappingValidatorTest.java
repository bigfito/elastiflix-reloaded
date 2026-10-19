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
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.json.JsonDocuments;
import com.bigfito.elastiflix.ingest.movie.MovieDocuments;

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
    @DisplayName("validateAndRelease suelta cada nodo crudo de la lista a medida que lo revisa")
    void shouldReleaseRawNodesWhileValidating() {
        // given
        List<JsonNode> documents = new ArrayList<>(new JsonDocuments().read(JSON).documents());

        // when
        ValidatedMovies checked = validator.validateAndRelease(documents, List.of(), Set.of());

        // then
        assertThat(checked.movies()).hasSize(4);
        assertThat(documents).hasSize(4).containsOnlyNulls();
    }

    @Test
    @DisplayName("el JSON de cuatro películas, ya leído, no tiene errores y avisa el texto vacío")
    void shouldAcceptTheJsonFixture() {
        // given: JsonDocuments ya quitó los vectores; el validador solo revisa campos
        List<JsonNode> documents = new JsonDocuments().read(JSON).documents();

        // when
        ValidatedMovies result = validator.validate(documents, Set.of("597", "999"));

        // then
        assertThat(result.report().errors()).isEmpty();
        assertThat(result.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .containsExactly(tuple(ErrorCode.EMPTY_TEXT.code(), "homepage"));
        assertThat(result.replaceCount()).isEqualTo(1);
        assertThat(result.movies()).filteredOn(movie -> "597".equals(movie.id()))
                .singleElement()
                .satisfies(movie -> assertThat(movie.homepage()).isNull());
        assertThat(result.movies()).usingRecursiveComparison()
                .withEqualsForType(Arrays::equals, float[].class)
                .ignoringFields("homepage")
                .isEqualTo(moviesOf(documents));
    }

    @Test
    @DisplayName("un vector o un _id que llegue en el nodo no pasa a la película")
    void shouldNotCopyVectorsNorTheDocumentId() {
        // given: los nodos crudos del fixture, con _id y los tres vectores
        List<JsonNode> raw = documentsOf(JSON);
        assertThat(raw).allSatisfy(node -> assertThat(node.has("plot_1024") && node.has("_id")).isTrue());

        // when
        ValidatedMovies result = validator.validate(raw, Set.of());

        // then
        assertThat(result.report().errors()).isEmpty();
        assertThat(result.movies()).hasSize(4).allSatisfy(movie -> {
            assertThat(movie.plot256()).isNull();
            assertThat(movie.plot512()).isNull();
            assertThat(movie.plot1024()).isNull();
        });
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

        assertThat(csv).isEqualTo("\uFEFF" + """
                registro,id,campo,recibido,esperado
                1,7,plot,"hola, mundo",un texto
                """);
    }

    @Test
    @DisplayName("una celda que parece fórmula lleva apóstrofo")
    void shouldNeutralizeFormulaCells() {
        ValidationIssue issue = new ValidationIssue(
                ErrorCode.TYPE_MISMATCH.code(), "=1", "+2", "-3", "@4", "un texto", false);

        String csv = new ValidationReport(List.of(issue)).toErrorCsv();

        assertThat(csv).contains("'=1,'+2,'-3,'@4,un texto");
    }

    @Test
    @DisplayName("las advertencias del estado se agrupan por código y campo")
    void shouldGroupWarningsByCodeAndField() {
        ValidationIssue first = new ValidationIssue(
                ErrorCode.EMPTY_TEXT.code(), "1", "7", "tagline", "", "sin valor", true);
        ValidationIssue second = new ValidationIssue(
                ErrorCode.EMPTY_TEXT.code(), "2", "8", "tagline", "", "sin valor", true);
        ValidationIssue other = new ValidationIssue(
                ErrorCode.EMPTY_LIST_VALUE.code(), "2", "8", "genres", "", "se omite", true);

        assertThat(new ValidationReport(List.of(first, second, other)).groupedWarnings())
                .containsExactly(
                        new WarningCount(ErrorCode.EMPTY_TEXT.code(), "tagline", 2, "sin valor"),
                        new WarningCount(ErrorCode.EMPTY_LIST_VALUE.code(), "genres", 1, "se omite"));
    }

    @Test
    @DisplayName("un id con espacio se busca con ese mismo texto")
    void shouldKeepTheSpaceInsideAnId() {
        JsonNode documents = MAPPER.readTree("""
                [{"id":" 7","title":"Una","plot":"texto"}]
                """);
        JsonNode id = children(documents).getFirst().get("id");

        ValidatedMovies result = validator.validate(children(documents), Set.of(IdTexts.of(id)));

        assertThat(IdTexts.of(id)).isEqualTo(" 7");
        assertThat(result.replaceCount()).isEqualTo(1);
        assertThat(result.movies().getFirst().id()).isEqualTo(" 7");
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

    private static List<Movie> moviesOf(List<JsonNode> documents) {
        return documents.stream().map(MovieDocuments::fromNode).toList();
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
