package com.bigfito.elastiflix.ingest.mapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.core.model.MovieKeyword;
import com.bigfito.elastiflix.ingest.csv.CsvMovieReader;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Ceros desconocidos, textos vacíos, listas sin repetidos y las revisiones de la sinopsis.
 */
class IngestChecksTest {

    private static final String PLOT = "Sinopsis de prueba suficientemente larga para validar.";
    private static final String OTHER = "Otra sinopsis de prueba suficientemente larga y distinta.";
    private static final JsonMapper MAPPER = MovieJsonMapper.create();

    private final MappingValidator validator = new MappingValidator();

    @TempDir
    private Path folder;

    @Test
    @DisplayName("un texto opcional vacío o en blanco se guarda sin valor")
    void shouldDropBlankOptionalTexts() {
        ValidatedMovies result = validate("""
                {"id":"1","title":"Una","plot":"%s","homepage":"","tagline":"   ","imdb_id":""}
                """.formatted(PLOT));

        assertThat(result.report().errors()).isEmpty();
        assertThat(result.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .containsExactlyInAnyOrder(
                        tuple(ErrorCode.EMPTY_TEXT.code(), "homepage"),
                        tuple(ErrorCode.EMPTY_TEXT.code(), "tagline"),
                        tuple(ErrorCode.EMPTY_TEXT.code(), "imdb_id"));
        Movie movie = result.movies().getFirst();
        assertThat(movie.homepage()).isNull();
        assertThat(movie.tagline()).isNull();
        assertThat(movie.imdbId()).isNull();
    }

    @Test
    @DisplayName("un 0 en presupuesto, recaudación, duración o votos se omite; popularidad y user_score no")
    void shouldTreatZeroAsUnknownAndKeepARealBudget() {
        ValidatedMovies result = validate("""
                {"id":"1","title":"Una","plot":"%s","budget":0,"revenue":0.0,"runtime":0,
                "vote_average":0,"vote_count":0,"popularity":0,"user_score":0}
                """.formatted(PLOT));

        assertThat(result.report().errors()).isEmpty();
        assertThat(result.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .containsExactlyInAnyOrder(
                        tuple(ErrorCode.ZERO_AS_NULL.code(), "budget"),
                        tuple(ErrorCode.ZERO_AS_NULL.code(), "revenue"),
                        tuple(ErrorCode.ZERO_AS_NULL.code(), "runtime"),
                        tuple(ErrorCode.ZERO_AS_NULL.code(), "vote_average"),
                        tuple(ErrorCode.ZERO_AS_NULL.code(), "vote_count"));
        Movie movie = result.movies().getFirst();
        assertThat(movie.budget()).isNull();
        assertThat(movie.revenue()).isNull();
        assertThat(movie.runtime()).isNull();
        assertThat(movie.voteAverage()).isNull();
        assertThat(movie.voteCount()).isNull();
        assertThat(movie.popularity()).isEqualTo(0.0);
        assertThat(movie.userScore()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("un presupuesto negativo se conserva")
    void shouldKeepANegativeBudget() {
        ValidatedMovies result = validate("""
                {"id":"1","title":"Una","plot":"%s","budget":-12}
                """.formatted(PLOT));

        assertThat(result.report().warnings()).isEmpty();
        assertThat(result.movies().getFirst().budget()).isEqualTo(-12.0);
    }

    @Test
    @DisplayName("una celda CSV en 0 pasa por el mismo aviso que el JSON")
    void shouldTreatZeroCsvCellsAsUnknown() throws Exception {
        Path file = folder.resolve("ceros.csv");
        Files.writeString(file, """
                id,title,plot,budget,revenue,runtime,vote_average,vote_count,popularity
                1,T,%s,0,0.0,0,0.0,0,0
                """.formatted(PLOT));

        ValidatedMovies result = validator.validate(
                new CsvMovieReader(new IndexMapping()).read(file, CsvSettings.withHeader()).documents(), Set.of());

        assertThat(result.report().errors()).isEmpty();
        assertThat(result.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .containsExactlyInAnyOrder(
                        tuple(ErrorCode.ZERO_AS_NULL.code(), "budget"),
                        tuple(ErrorCode.ZERO_AS_NULL.code(), "revenue"),
                        tuple(ErrorCode.ZERO_AS_NULL.code(), "runtime"),
                        tuple(ErrorCode.ZERO_AS_NULL.code(), "vote_average"),
                        tuple(ErrorCode.ZERO_AS_NULL.code(), "vote_count"));
        assertThat(result.movies().getFirst().popularity()).isEqualTo(0.0);
        assertThat(result.movies().getFirst().budget()).isNull();
    }

    @Test
    @DisplayName("una lista de textos guarda la primera aparición y avisa del resto")
    void shouldKeepTheFirstCopyOfARepeatedText() {
        ValidatedMovies result = validate("""
                {"id":"1","title":"Una","plot":"%s",
                "cast":["Ana","Bo","Ana","Cia"],
                "production_companies":["Estudio","Estudio"]}
                """.formatted(PLOT));

        assertThat(result.report().errors()).isEmpty();
        assertThat(result.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field, ValidationIssue::received)
                .containsExactly(
                        tuple(ErrorCode.DUPLICATE_LIST_VALUE.code(), "cast", "Ana"),
                        tuple(ErrorCode.DUPLICATE_LIST_VALUE.code(), "production_companies", "Estudio"));
        Movie movie = result.movies().getFirst();
        assertThat(movie.cast()).containsExactly("Ana", "Bo", "Cia");
        assertThat(movie.productionCompanies()).containsExactly("Estudio");
    }

    @Test
    @DisplayName("las palabras clave repetidas se quitan por id y se conserva el orden")
    void shouldKeepTheFirstKeywordWithARepeatedId() {
        ValidatedMovies result = validate("""
                {"id":"1","title":"Una","plot":"%s","keywords":[
                {"id":1,"name":"alpha"},{"id":2,"name":"beta"},{"id":1,"name":"gamma"}]}
                """.formatted(PLOT));

        assertThat(result.report().errors()).isEmpty();
        assertThat(result.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field, ValidationIssue::received)
                .containsExactly(tuple(ErrorCode.DUPLICATE_LIST_VALUE.code(), "keywords", "1"));
        assertThat(result.movies().getFirst().keywords())
                .extracting(MovieKeyword::id, MovieKeyword::name)
                .containsExactly(tuple(1L, "alpha"), tuple(2L, "beta"));
    }

    @ParameterizedTest(name = "plot = {0}")
    @ValueSource(strings = {
        "[No information found]",
        "no information found",
        "[ no information found ]",
        "NO INFORMATION FOUND"})
    @DisplayName("un marcador de sinopsis rechaza el archivo")
    void shouldRejectAPlaceholderPlot(String plot) {
        ValidatedMovies result = validate("""
                [{"id":"1","title":"Una","plot":"%s"},{"id":"2","title":"Otra","plot":"%s"}]
                """.formatted(plot, OTHER));

        assertThat(result.movies()).isEmpty();
        assertThat(result.report().accepted()).isFalse();
        assertThat(result.report().errors())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .contains(tuple(ErrorCode.PLACEHOLDER_PLOT.code(), "plot"));
    }

    @Test
    @DisplayName("una sinopsis de menos de 40 caracteres se rechaza y una de 40 pasa")
    void shouldRejectAPlotShorterThanTheMinimum() {
        String shortPlot = "a".repeat(PlotChecks.MIN_PLOT_LENGTH - 1);
        String enough = "b".repeat(PlotChecks.MIN_PLOT_LENGTH);

        ValidatedMovies rejected = validate("""
                {"id":"1","title":"Una","plot":"%s"}
                """.formatted(shortPlot));
        ValidatedMovies accepted = validate("""
                {"id":"1","title":"Una","plot":"%s"}
                """.formatted(enough));

        assertThat(rejected.movies()).isEmpty();
        assertThat(rejected.report().errors())
                .extracting(ValidationIssue::code, ValidationIssue::expected)
                .containsExactly(tuple(ErrorCode.PLACEHOLDER_PLOT.code(),
                        IngestPhrase.REAL_PLOT.text(PlotChecks.MIN_PLOT_LENGTH)));
        assertThat(accepted.report().errors()).isEmpty();
        assertThat(accepted.movies().getFirst().plot()).isEqualTo(enough);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "I couldn't find a better source for this synopsis at all",
        "knowledge cutoff still limits what this synopsis can say",
        "I'm sorry this synopsis still sounds like an assistant",
        "I apologize for the gap this synopsis could not close today"})
    @DisplayName("un resto de modelo se avisa y la película se acepta")
    void shouldWarnAboutModelLeftovers(String plot) {
        String text = "a".repeat(PlotChecks.MIN_PLOT_LENGTH) + " " + plot;
        ValidatedMovies result = validate("""
                {"id":"1","title":"Una","plot":"%s"}
                """.formatted(text));

        assertThat(result.report().accepted()).isTrue();
        assertThat(result.movies()).hasSize(1);
        assertThat(result.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .containsExactly(tuple(ErrorCode.SUSPECT_PLOT.code(), "plot"));
        assertThat(result.movies().getFirst().plot()).isEqualTo(text);
    }

    @Test
    @DisplayName("el markdown se avisa y «, not» solo si sigue a directed by o produced by")
    void shouldWarnAboutMarkdownAndNotClarifications() {
        String markdown = "a".repeat(PlotChecks.MIN_PLOT_LENGTH) + " **nota**";
        String credit = "a".repeat(20) + " directed by Wes Anderson, not Disney";
        String prose = "a".repeat(PlotChecks.MIN_PLOT_LENGTH) + ", not Disney";

        assertThat(validate(movie("1", markdown)).report().warnings())
                .extracting(ValidationIssue::code)
                .containsExactly(ErrorCode.SUSPECT_PLOT.code());
        assertThat(validate(movie("1", credit)).report().warnings())
                .extracting(ValidationIssue::received)
                .containsExactly(", not Disney");
        assertThat(validate(movie("1", prose)).report().warnings()).isEmpty();
    }

    @Test
    @DisplayName("mencionar «no information found» dentro de una sinopsis larga no la rechaza")
    void shouldKeepALongPlotThatMentionsThePlaceholderWords() {
        String plot = "a".repeat(PlotChecks.MIN_PLOT_LENGTH) + " no information found later";

        ValidatedMovies result = validate(movie("1", plot));

        assertThat(result.report().errors()).isEmpty();
        assertThat(result.movies().getFirst().plot()).isEqualTo(plot);
    }

    @Test
    @DisplayName("dos películas con la misma sinopsis se avisan y las dos se aceptan")
    void shouldWarnWhenTwoMoviesShareAPlot() {
        String repeated = "  " + PLOT.toLowerCase(Locale.ROOT).replace("sinopsis", "sinopsis   ") + "  ";
        ValidatedMovies result = validate("""
                [{"id":"441663","title":"Una","plot":"%s"},{"id":"11313","title":"Otra","plot":"%s"}]
                """.formatted(PLOT, repeated));

        assertThat(result.report().accepted()).isTrue();
        assertThat(result.movies()).extracting(Movie::id).containsExactly("441663", "11313");
        assertThat(result.report().warnings())
                .extracting(ValidationIssue::code, ValidationIssue::id, ValidationIssue::received)
                .containsExactly(
                        tuple(ErrorCode.TEXT_NORMALIZED.code(), "11313", repeated),
                        tuple(ErrorCode.DUPLICATE_PLOT.code(), "11313", "441663"));
        assertThat(result.movies().get(1).plot()).isEqualTo(PLOT.toLowerCase(Locale.ROOT));
    }

    @Test
    @DisplayName("el mismo id con la misma sinopsis no suma un aviso de sinopsis repetida")
    void shouldNotWarnWhenTheSameIdRepeatsThePlot() {
        ValidatedMovies result = validate("""
                [{"id":"7","title":"Una","plot":"%s"},{"id":"7","title":"Otra","plot":"%s"}]
                """.formatted(PLOT, PLOT));

        assertThat(result.report().errors())
                .extracting(ValidationIssue::code)
                .containsExactly(ErrorCode.DUPLICATE_ID.code());
        assertThat(result.report().warnings()).isEmpty();
    }

    private ValidatedMovies validate(String json) {
        JsonNode node = MAPPER.readTree(json);
        List<JsonNode> documents = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(documents::add);
        } else {
            documents.add(node);
        }
        return validator.validate(documents, Set.of());
    }

    private static String movie(String id, String plot) {
        return "{\"id\":\"" + id + "\",\"title\":\"Una\",\"plot\":\"" + plot + "\"}";
    }
}
