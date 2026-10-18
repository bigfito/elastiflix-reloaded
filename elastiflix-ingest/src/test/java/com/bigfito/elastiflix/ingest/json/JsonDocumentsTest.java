package com.bigfito.elastiflix.ingest.json;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.IngestValidationException;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.MappingValidator;
import com.bigfito.elastiflix.ingest.mapping.ValidatedMovies;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El fixture de cuatro películas se lee como exportación, arreglo y NDJSON.
 */
class JsonDocumentsTest {

    private static final Path FIXTURE = Path.of("..", "elasticsearch", "fixtures", "movies-sample.json");
    private static final String EQUALIZER_ID = "345887";

    private final JsonDocuments reader = new JsonDocuments();
    private final JsonMapper mapper = MovieJsonMapper.create();

    @TempDir
    private Path folder;

    @Test
    @DisplayName("los nodos leídos no traen vectores y la lectura avisa EFX-ING-020 una vez por campo")
    void shouldDropVectorsWhileReading() {
        // when
        JsonRead read = reader.read(FIXTURE);

        // then
        assertThat(read.documents()).hasSize(4).allSatisfy(node -> assertThat(node.has("plot_256")
                || node.has("plot_512") || node.has("plot_1024")).isFalse());
        assertThat(read.warnings()).extracting(ValidationIssue::field)
                .containsExactlyInAnyOrder("plot_256", "plot_512", "plot_1024");
        assertThat(read.warnings()).allSatisfy(issue -> {
            assertThat(issue.code()).isEqualTo(ErrorCode.VECTORS_IGNORED.code());
            assertThat(issue.warning()).isTrue();
        });
    }

    @Test
    @DisplayName("lee las cuatro películas de la exportación y descarta los vectores")
    void shouldReadTheFixtureWhenTheFileIsAnExport() {
        List<Movie> movies = movies(reader.read(FIXTURE).documents());

        assertThat(movies).extracting(Movie::id).containsExactly("597", "27205", "10681", EQUALIZER_ID);
        Movie titanic = movie(movies, "597");
        assertThat(titanic.title()).isEqualTo("Titanic");
        assertThat(titanic.plot()).isNotBlank();
        assertThat(titanic.plot1024()).isNull();
        assertThat(movie(movies, EQUALIZER_ID).belongsToCollection().name())
                .isEqualTo("The Equalizer Collection");
    }

    @Test
    @DisplayName("un arreglo y un NDJSON de las mismas películas dan los mismos valores")
    void shouldReadTheSameMoviesWhenTheShapeChanges() throws Exception {
        JsonNode documents = mapper.readTree(FIXTURE.toFile()).get("documents");
        List<Movie> fromExport = movies(reader.read(FIXTURE).documents());
        List<Movie> fromArray = movies(reader.read(writeArray(documents)).documents());
        List<Movie> fromNdjson = movies(reader.read(writeNdjson(documents)).documents());

        assertThat(fromArray).usingRecursiveComparison()
                .withEqualsForType(Arrays::equals, float[].class)
                .isEqualTo(fromExport);
        assertThat(fromNdjson).usingRecursiveComparison()
                .withEqualsForType(Arrays::equals, float[].class)
                .isEqualTo(fromExport);
    }

    @Test
    @DisplayName("document_count distinto del largo de documents se rechaza")
    void shouldRejectACountThatDoesNotMatch() throws Exception {
        Path file = folder.resolve("corto.json");
        Files.writeString(file, """
                {"document_count": 1, "documents": [
                  {"id": 1, "title": "Una", "plot": "a"},
                  {"id": 2, "title": "Otra", "plot": "b"}
                ]}
                """);

        assertThatThrownBy(() -> reader.read(file))
                .isInstanceOf(IngestValidationException.class)
                .extracting(thrown -> ((IngestValidationException) thrown).errorCode())
                .isEqualTo(ErrorCode.DOCUMENT_COUNT_MISMATCH);
    }

    @Test
    @DisplayName("un JSON a medias no se puede leer")
    void shouldRejectABrokenFile() throws Exception {
        Path file = folder.resolve("roto.json");
        Files.writeString(file, "{");

        assertThatThrownBy(() -> reader.read(file))
                .isInstanceOf(IngestValidationException.class)
                .extracting(thrown -> ((IngestValidationException) thrown).errorCode())
                .isEqualTo(ErrorCode.UNREADABLE_JSON);
    }

    @Test
    @DisplayName("lo que sigue al primer valor JSON se rechaza")
    void shouldRejectContentAfterTheFirstValue() throws Exception {
        Path array = folder.resolve("sobra-arreglo.json");
        Path export = folder.resolve("sobra-exportacion.json");
        Files.writeString(array, "[{\"id\":1}] {\"id\":2}");
        Files.writeString(export, "{\"documents\":[]} true");

        assertThatThrownBy(() -> reader.read(array))
                .isInstanceOf(IngestValidationException.class)
                .extracting(thrown -> ((IngestValidationException) thrown).errorCode())
                .isEqualTo(ErrorCode.UNREADABLE_JSON);
        assertThatThrownBy(() -> reader.read(export))
                .isInstanceOf(IngestValidationException.class)
                .extracting(thrown -> ((IngestValidationException) thrown).errorCode())
                .isEqualTo(ErrorCode.UNREADABLE_JSON);
    }

    private Path writeArray(JsonNode documents) {
        Path file = folder.resolve("arreglo.json");
        mapper.writeValue(file.toFile(), documents);
        return file;
    }

    private Path writeNdjson(JsonNode documents) throws Exception {
        List<String> lines = new ArrayList<>();
        for (JsonNode document : documents) {
            lines.add(mapper.writeValueAsString(document));
        }
        Path file = folder.resolve("peliculas.ndjson");
        Files.write(file, lines);
        return file;
    }

    /** Por el camino real: el validador del mapping, sin errores, es quien arma las películas. */
    private static List<Movie> movies(List<JsonNode> documents) {
        ValidatedMovies checked = new MappingValidator().validate(documents, Set.of());
        assertThat(checked.report().errors()).isEmpty();
        return checked.movies();
    }

    private static Movie movie(List<Movie> movies, String id) {
        return movies.stream().filter(item -> id.equals(item.id())).findFirst().orElseThrow();
    }
}
