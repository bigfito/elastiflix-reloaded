package com.bigfito.elastiflix.ingest.mapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.ingest.csv.CsvMovieReader;
import com.bigfito.elastiflix.ingest.csv.CsvRead;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Un número fuera de rango es un error de fila con su campo, nunca una excepción que cuelga el trabajo.
 */
class NumberRangeTest {

    private static final String DIGITS_25 = "1234567890123456789012345";

    private final MappingValidator validator = new MappingValidator();
    private final CsvMovieReader csv = new CsvMovieReader(new IndexMapping());

    @TempDir
    private Path folder;

    @ParameterizedTest(name = "{0}={1} en CSV → EFX-ING-012")
    @CsvSource({
        "vote_count, " + DIGITS_25,
        "vote_count, 1e3",
        "vote_count, 12.0",
        "runtime, 12345678901",
        "vote_average, 1e39"})
    @DisplayName("una celda CSV fuera de rango o con decimales en un entero es un error de fila")
    void shouldRejectOutOfRangeCsvCells(String field, String cell) throws Exception {
        // given
        String content = "id,title,plot," + field + "\n1,T,p," + cell + "\n";
        Path file = Files.writeString(folder.resolve("numeros.csv"), content);

        // when
        CsvRead read = csv.read(file, CsvSettings.withHeader());
        ValidatedMovies checked = validator.validate(read.documents(), Set.of());

        // then
        assertThat(checked.report().errors()).singleElement().satisfies(issue -> {
            assertThat(issue.code()).isEqualTo(ErrorCode.TYPE_MISMATCH.code());
            assertThat(issue.field()).isEqualTo(field);
        });
        assertThat(checked.movies()).isEmpty();
    }

    @ParameterizedTest(name = "{0}: {1} en JSON → EFX-ING-012")
    @CsvSource({
        "vote_count, " + DIGITS_25,
        "vote_average, 1e39",
        "budget, 1e400"})
    @DisplayName("un número JSON fuera de rango es un error de registro")
    void shouldRejectOutOfRangeJsonNumbers(String field, String number) {
        JsonNode node = MovieJsonMapper.create().readTree(
                "{\"id\":\"1\",\"title\":\"T\",\"plot\":\"p\",\"" + field + "\":" + number + "}");

        ValidatedMovies checked = validator.validate(List.of(node), Set.of());

        assertThat(checked.report().errors()).singleElement().satisfies(issue -> {
            assertThat(issue.code()).isEqualTo(ErrorCode.TYPE_MISMATCH.code());
            assertThat(issue.field()).isEqualTo(field);
        });
    }

    @Test
    @DisplayName("un id numérico de 25 dígitos se guarda como texto, sin pasar por long")
    void shouldKeepALongNumericIdAsText() {
        JsonNode node = MovieJsonMapper.create().readTree("{\"id\":" + DIGITS_25 + ",\"title\":\"T\",\"plot\":\"p\"}");

        ValidatedMovies checked = validator.validate(List.of(node), Set.of());

        assertThat(checked.report().errors()).isEmpty();
        assertThat(checked.movies()).singleElement().extracting(movie -> movie.id()).isEqualTo(DIGITS_25);
    }

    @Test
    @DisplayName("el id de una palabra clave de 25 dígitos es un error, no una falla al convertir")
    void shouldRejectAHugeKeywordId() {
        JsonNode node = MovieJsonMapper.create().readTree("{\"id\":\"1\",\"title\":\"T\",\"plot\":\"p\","
                + "\"keywords\":[{\"id\":" + DIGITS_25 + ",\"name\":\"x\"}]}");

        ValidatedMovies checked = validator.validate(List.of(node), Set.of());

        assertThat(checked.report().errors()).extracting(ValidationIssue::code)
                .containsExactly(ErrorCode.TYPE_MISMATCH.code());
    }

    @Test
    @DisplayName("el CSV no recorta title ni id: un espacio sigue siendo parte del valor")
    void shouldNotTrimCsvTexts() throws Exception {
        Path file = Files.writeString(folder.resolve("espacios.csv"), "id,title,plot\n 7, Título ,p\n");

        CsvRead read = csv.read(file, CsvSettings.withHeader());

        assertThat(read.documents().getFirst().get("id").asString()).isEqualTo(" 7");
        assertThat(read.documents().getFirst().get("title").asString()).isEqualTo(" Título ");
    }
}
