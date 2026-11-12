package com.bigfito.elastiflix.ingest.mapping;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.csv.CsvMovieReader;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * {@code enrichment} es opcional: null, ausente o vacío se aceptan, y un objeto solo pasa si sus claves
 * están en el mapping y sus valores son textos keyword.
 */
class EnrichmentValidationTest {

    private static final JsonMapper MAPPER = MovieJsonMapper.create();

    private final MappingValidator validator = new MappingValidator();

    @TempDir
    private Path folder;

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "{\"id\":\"1\",\"title\":\"T\",\"plot\":\"P\"}",
            "{\"id\":\"1\",\"title\":\"T\",\"plot\":\"P\",\"enrichment\":null}",
            "{\"id\":\"1\",\"title\":\"T\",\"plot\":\"P\",\"enrichment\":{}}"
    })
    @DisplayName("ausente, null o vacío se aceptan y la película queda sin procedencia")
    void shouldAcceptAMovieWithoutTags(String json) {
        Movie movie = validate(json).movies().getFirst();

        assertThat(movie.enrichment()).isNull();
    }

    @Test
    @DisplayName("guarda las etiquetas conocidas y omite un texto en blanco con EFX-ING-022")
    void shouldKeepKnownTagsAndSkipABlankOne() {
        ValidatedMovies checked = validate("""
                {"id":"1","title":"T","plot":"P","enrichment":{"plot":"tmdb_overview","budget":"  ","cast":null}}
                """);

        assertThat(checked.report().errors()).isEmpty();
        assertThat(checked.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .containsExactly(tuple(ErrorCode.EMPTY_TEXT.code(), "enrichment.budget"));
        assertThat(checked.movies().getFirst().enrichment()).containsExactly(Map.entry("plot", "tmdb_overview"));
    }

    @Test
    @DisplayName("una clave que el mapping no declara es EFX-ING-007 con la ruta completa")
    void shouldRejectAnUnknownTag() {
        ValidationReport report = validate("""
                {"id":"1","title":"T","plot":"P","enrichment":{"director":"tmdb"}}
                """).report();

        assertThat(report.errors())
                .extracting(ValidationIssue::code, ValidationIssue::field, ValidationIssue::received)
                .containsExactly(tuple(ErrorCode.UNKNOWN_FIELD.code(), "enrichment.director",
                        "Un campo llamado enrichment.director"));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "{\"id\":\"1\",\"title\":\"T\",\"plot\":\"P\",\"enrichment\":[]}",
            "{\"id\":\"1\",\"title\":\"T\",\"plot\":\"P\",\"enrichment\":\"tmdb\"}",
            "{\"id\":\"1\",\"title\":\"T\",\"plot\":\"P\",\"enrichment\":{\"plot\":1}}"
    })
    @DisplayName("un valor que no es un objeto de textos es EFX-ING-012")
    void shouldRejectATagThatIsNotText(String json) {
        assertThat(validate(json).report().errors())
                .extracting(ValidationIssue::code)
                .containsExactly(ErrorCode.TYPE_MISMATCH.code());
    }

    @Test
    @DisplayName("una etiqueta más larga que un término de Lucene es EFX-ING-012")
    void shouldRejectATagLongerThanALuceneTerm() {
        String tag = "a".repeat(32_767);
        ValidationReport report = validate(
                "{\"id\":\"1\",\"title\":\"T\",\"plot\":\"P\",\"enrichment\":{\"plot\":\"" + tag + "\"}}").report();

        assertThat(report.errors())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .containsExactly(tuple(ErrorCode.TYPE_MISMATCH.code(), "enrichment.plot"));
    }

    @Test
    @DisplayName("en CSV el objeto va como JSON compacto en la celda y una celda vacía es null")
    void shouldReadEnrichmentFromAJsonCell() throws Exception {
        String cell = "{\"plot\":\"tmdb_overview\",\"user_score\":\"wikidata\"}";
        String csv = "id,title,plot,enrichment\r\n1,T,P,\"" + cell.replace("\"", "\"\"")
                + "\"\r\n2,U,Q,\r\n";
        Path file = folder.resolve("peliculas.csv");
        Files.writeString(file, csv, StandardCharsets.UTF_8);

        ValidatedMovies checked = validator.validate(
                new CsvMovieReader(new IndexMapping()).read(file, CsvSettings.withHeader()).documents(), Set.of());

        assertThat(checked.report().errors()).isEmpty();
        assertThat(checked.movies()).extracting(Movie::id, Movie::enrichment).containsExactly(
                tuple("1", Map.of("plot", "tmdb_overview", "user_score", "wikidata")),
                tuple("2", null));
    }

    private ValidatedMovies validate(String json) {
        return validator.validate(List.of(MAPPER.readTree(json)), Set.of());
    }
}
