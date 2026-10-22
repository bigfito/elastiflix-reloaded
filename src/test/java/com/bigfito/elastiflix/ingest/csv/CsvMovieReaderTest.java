package com.bigfito.elastiflix.ingest.csv;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.json.JsonDocuments;
import com.bigfito.elastiflix.ingest.mapping.MappingValidator;
import com.bigfito.elastiflix.ingest.mapping.ValidatedMovies;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * El CSV de cuatro películas coincide con el JSON y rechaza un encabezado inútil.
 */
class CsvMovieReaderTest {

    private static final Path JSON = Path.of("elasticsearch", "fixtures", "movies-sample.json");
    private static final Path CSV = Path.of("elasticsearch", "fixtures", "movies-sample.csv");

    private final CsvMovieReader reader = new CsvMovieReader(new IndexMapping());
    private final MappingValidator validator = new MappingValidator();
    private final JsonDocuments json = new JsonDocuments();

    @TempDir
    private Path folder;

    @Test
    @DisplayName("lee el fixture con cero errores y avisa que los vectores se ignoran")
    void shouldMatchTheJsonWhenTheCsvHasAHeader() {
        CsvRead read = reader.read(CSV, CsvSettings.withHeader());

        assertThat(read.report().errors()).isEmpty();
        assertThat(read.report().warnings()).extracting(ValidationIssue::code)
                .contains(ErrorCode.VECTORS_IGNORED.code());
        ValidatedMovies checked = validator.validate(read.documents(), Set.of());
        assertThat(checked.report().errors()).isEmpty();
        assertThat(checked.movies()).usingRecursiveComparison()
                .withEqualsForType(Arrays::equals, float[].class)
                .isEqualTo(validator.validate(json.read(JSON).documents(), Set.of()).movies());
    }

    @Test
    @DisplayName("el mismo archivo sin su primera fila se rechaza")
    void shouldRejectTheFileWhenTheHeaderRowIsMissing() throws Exception {
        Path file = folder.resolve("sin-encabezado.csv");
        Files.writeString(file, withoutHeader());

        CsvRead read = reader.read(file, CsvSettings.withHeader());

        assertThat(read.report().accepted()).isFalse();
        assertThat(read.report().errors()).extracting(ValidationIssue::code)
                .contains(ErrorCode.INVALID_HEADER.code());
        assertThat(read.documents()).isEmpty();
    }

    @Test
    @DisplayName("una fila con menos o más celdas que el encabezado se rechaza")
    void shouldRejectARowWhoseWidthDoesNotMatchTheHeader() throws Exception {
        Path file = folder.resolve("ancho.csv");
        Files.writeString(file, "id,title,plot\n1,Una\n2,Dos,texto,sobra\n");

        CsvRead read = reader.read(file, CsvSettings.withHeader());

        assertThat(read.report().accepted()).isFalse();
        assertThat(read.report().errors()).extracting(ValidationIssue::code, ValidationIssue::recordLabel)
                .contains(
                        tuple(ErrorCode.COLUMN_COUNT.code(), "2"),
                        tuple(ErrorCode.COLUMN_COUNT.code(), "3"));
    }

    @Test
    @DisplayName("una columna director no existe en el mapping, pero las filas se leen sin ella")
    void shouldRejectAnUnknownColumn() throws Exception {
        Path file = folder.resolve("director.csv");
        Files.writeString(file, "id,title,plot,director\n1,Una,texto,alguien\n2,Otra,,\n");

        CsvRead read = reader.read(file, CsvSettings.withHeader());

        assertThat(read.report().errors()).extracting(ValidationIssue::code)
                .containsExactly(ErrorCode.UNKNOWN_FIELD.code());
        assertThat(read.documents()).hasSize(2)
                .allSatisfy(document -> assertThat(document.has("director")).isFalse());
        assertThat(read.rowCount()).isEqualTo(2);
        assertThat(read.columns()).containsExactly(
                new CsvColumn("id", 2), new CsvColumn("title", 2),
                new CsvColumn("plot", 1), new CsvColumn("director", 1));
    }

    @ParameterizedTest(name = "{0} → {1} en {2}")
    @MethodSource("invalidCsv")
    @DisplayName("cada variante CSV produce exactamente su error")
    void shouldReportTheCellError(String resource, String code, String field) throws Exception {
        CsvRead read = reader.read(resource(resource), CsvSettings.withHeader());
        ValidatedMovies checked = validator.validate(read.documents(), Set.of());

        assertThat(read.report().errors()).isEmpty();
        assertThat(checked.movies()).isEmpty();
        assertThat(checked.report().errors())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .containsExactly(tuple(code, field));
    }

    @Test
    @DisplayName("un título [REC] y un eslogan 007 se conservan como texto")
    void shouldKeepATitleThatLooksLikeJsonAndANumericTagline() throws Exception {
        CsvRead read = reader.read(resource("/validation/csv-rec.csv"), CsvSettings.withHeader());
        ValidatedMovies checked = validator.validate(read.documents(), Set.of());

        assertThat(checked.report().errors()).isEmpty();
        Movie movie = checked.movies().getFirst();
        assertThat(movie.title()).isEqualTo("[REC]");
        assertThat(movie.tagline()).isEqualTo("007");
    }

    private static Stream<Arguments> invalidCsv() {
        return Stream.of(
                Arguments.arguments("/validation/csv-id-mismatch.csv", ErrorCode.ID_MISMATCH.code(), "_id"),
                Arguments.arguments("/validation/csv-bad-date.csv", ErrorCode.TYPE_MISMATCH.code(), "release_date"),
                Arguments.arguments("/validation/csv-adult.csv", ErrorCode.TYPE_MISMATCH.code(), "adult"),
                Arguments.arguments("/validation/csv-keyword.csv", ErrorCode.UNKNOWN_FIELD.code(), "keywords.extra"),
                Arguments.arguments("/validation/csv-genres.csv", ErrorCode.TYPE_MISMATCH.code(), "genres"));
    }

    private static Path resource(String name) throws URISyntaxException {
        return Path.of(CsvMovieReaderTest.class.getResource(name).toURI());
    }

    @Test
    @DisplayName("la casilla de encabezado desmarcada rechaza el archivo")
    void shouldRejectWhenTheHeaderIsNotConfirmed() {
        CsvRead read = reader.read(CSV, new CsvSettings(CsvSettings.COMMA, false));

        assertThat(read.report().errors()).extracting(ValidationIssue::code)
                .containsExactly(ErrorCode.HEADER_NOT_CONFIRMED.code());
    }

    private static String withoutHeader() throws Exception {
        String text = Files.readString(CSV, StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        int lineBreak = text.indexOf('\n');
        return text.substring(lineBreak + 1);
    }
}
