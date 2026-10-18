package com.bigfito.elastiflix.ingest.csv;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.ingest.json.JsonMovieReader;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El CSV de cuatro películas coincide con el JSON y rechaza un encabezado inútil.
 */
class CsvMovieReaderTest {

    private static final Path JSON = Path.of("..", "elasticsearch", "fixtures", "movies-sample.json");
    private static final Path CSV = Path.of("..", "elasticsearch", "fixtures", "movies-sample.csv");

    private final CsvMovieReader reader = new CsvMovieReader(new IndexMapping());
    private final JsonMovieReader json = new JsonMovieReader();

    @TempDir
    private Path folder;

    @Test
    @DisplayName("lee el fixture con cero errores y avisa que los vectores se ignoran")
    void shouldMatchTheJsonWhenTheCsvHasAHeader() {
        CsvRead read = reader.read(CSV, CsvSettings.withHeader());

        assertThat(read.report().errors()).isEmpty();
        assertThat(read.report().warnings()).extracting(ValidationIssue::code)
                .contains(ErrorCode.VECTORS_IGNORED.code());
        assertThat(read.movies()).usingRecursiveComparison()
                .withEqualsForType(Arrays::equals, float[].class)
                .isEqualTo(json.read(JSON));
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
        assertThat(read.movies()).isEmpty();
    }

    @Test
    @DisplayName("una columna director no existe en el mapping")
    void shouldRejectAnUnknownColumn() throws Exception {
        Path file = folder.resolve("director.csv");
        Files.writeString(file, "id,title,plot,director\n1,Una,texto,alguien\n");

        CsvRead read = reader.read(file, CsvSettings.withHeader());

        assertThat(read.report().errors()).extracting(ValidationIssue::code)
                .containsExactly(ErrorCode.UNKNOWN_FIELD.code());
        assertThat(read.movies()).isEmpty();
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
