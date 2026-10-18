package com.bigfito.elastiflix.core.embedding;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.error.EmbeddingException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * El recorte deja norma 1 y reproduce los vectores guardados del fixture.
 */
class VectorProjectorTest {

    private static final Path FIXTURE = Path.of("..", "elasticsearch", "fixtures", "movies-sample.json");
    private static final String TITANIC_ID = "597";
    private static final double NORM_TOLERANCE = 1e-6;
    private static final double STORED_COSINE = 0.9999;
    private static final int MEDIUM = 512;
    private static final int SMALL = 256;

    @Test
    @DisplayName("un vector de unos recortado a 512 queda con norma 1")
    void shouldHaveUnitNormWhenProjected() {
        float[] source = new float[VectorProjector.MODEL_DIMENSIONS];
        for (int index = 0; index < source.length; index++) {
            source[index] = 1f;
        }

        float[] projected = VectorProjector.project(source, MEDIUM);

        assertThat(projected).hasSize(MEDIUM);
        assertThat(normOf(projected)).isCloseTo(1.0, within(NORM_TOLERANCE));
    }

    @Test
    @DisplayName("el recorte de plot_1024 del fixture reproduce plot_512 y plot_256")
    void shouldMatchStoredVectorsWhenFixtureIsProjected() {
        Movie titanic = movie(TITANIC_ID);

        float[] medium = VectorProjector.project(titanic.plot1024(), MEDIUM);
        float[] small = VectorProjector.project(titanic.plot1024(), SMALL);

        assertThat(CosineSimilarity.between(medium, titanic.plot512())).isGreaterThanOrEqualTo(STORED_COSINE);
        assertThat(CosineSimilarity.between(small, titanic.plot256())).isGreaterThanOrEqualTo(STORED_COSINE);
    }

    @Test
    @DisplayName("un vector que no tiene 1024 dimensiones se rechaza")
    void shouldRejectWhenSourceLengthIsWrong() {
        assertThatThrownBy(() -> VectorProjector.project(new float[SMALL], MEDIUM))
                .isInstanceOf(EmbeddingException.class)
                .extracting(error -> ((EmbeddingException) error).errorCode())
                .isEqualTo(ErrorCode.UNEXPECTED_VECTOR);
    }

    private static double normOf(float[] values) {
        double square = 0;
        for (float value : values) {
            square += (double) value * value;
        }
        return Math.sqrt(square);
    }

    private static Movie movie(String id) {
        JsonMapper mapper = MovieJsonMapper.create();
        JsonNode documents = mapper.readTree(FIXTURE.toFile()).get("documents");
        List<Movie> movies = new ArrayList<>();
        for (JsonNode document : documents) {
            movies.add(mapper.treeToValue(document, Movie.class));
        }
        return movies.stream().filter(movie -> id.equals(movie.id())).findFirst().orElseThrow();
    }
}
