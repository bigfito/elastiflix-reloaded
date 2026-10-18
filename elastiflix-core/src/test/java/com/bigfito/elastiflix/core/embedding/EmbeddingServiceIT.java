package com.bigfito.elastiflix.core.embedding;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bigfito.elastiflix.core.TestApplication;
import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.model.Movie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La combinación calibrada reproduce los vectores ya guardados en el catálogo.
 *
 * <p>Solo lee. No escribe índices ni registra vectores.</p>
 */
@SpringBootTest(classes = TestApplication.class)
class EmbeddingServiceIT {

    private static final Path QUERY = Path.of("..", "elasticsearch", "fixtures", "query-vector-titanic.json");
    private static final List<String> IDS = List.of("597", "27205", "10681", "345887", "134");
    private static final double QUERY_COSINE = 0.999;
    private static final double DOCUMENT_COSINE = 0.999;
    private static final double PROJECTED_COSINE = 0.995;
    private static final int MEDIUM = 512;
    private static final int SMALL = 256;

    private final CatalogIndex catalog;
    private final ElastiflixProperties properties;
    private final EmbeddingService embeddings;

    /**
     * Spring no inyecta el constructor de una prueba si no lleva {@code @Autowired}.
     *
     * @param catalog    lectura del catálogo, con vectores
     * @param properties alias configurado
     * @param embeddings servicio de vectores
     */
    @Autowired
    EmbeddingServiceIT(CatalogIndex catalog, ElastiflixProperties properties, EmbeddingService embeddings) {
        this.catalog = catalog;
        this.properties = properties;
        this.embeddings = embeddings;
    }

    @Test
    @DisplayName("la consulta del fixture queda a coseno 0.999 de vector_1024")
    void shouldMatchTheQueryFixture() {
        JsonNode root = JsonMapper.builder().build().readTree(QUERY.toFile());

        float[] generated = embeddings.embedQuery(root.get("text").asString());

        assertThat(CosineSimilarity.between(generated, floats(root.get("vector_1024"))))
                .isGreaterThanOrEqualTo(QUERY_COSINE);
    }

    @Test
    @DisplayName("las cinco sinopsis quedan cerca de plot_1024, plot_512 y plot_256")
    void shouldMatchStoredDocumentVectors() {
        Map<String, Movie> movies = moviesById();
        List<String> plots = new ArrayList<>();
        for (String id : IDS) {
            plots.add(movies.get(id).plot());
        }

        List<float[]> generated = embeddings.embedDocuments(plots);

        assertThat(generated).hasSize(IDS.size());
        for (int index = 0; index < IDS.size(); index++) {
            assertClose(movies.get(IDS.get(index)), generated.get(index));
        }
    }

    private static void assertClose(Movie movie, float[] generated) {
        assertThat(CosineSimilarity.between(generated, movie.plot1024()))
                .isGreaterThanOrEqualTo(DOCUMENT_COSINE);
        assertThat(CosineSimilarity.between(VectorProjector.project(generated, MEDIUM), movie.plot512()))
                .isGreaterThanOrEqualTo(PROJECTED_COSINE);
        assertThat(CosineSimilarity.between(VectorProjector.project(generated, SMALL), movie.plot256()))
                .isGreaterThanOrEqualTo(PROJECTED_COSINE);
    }

    private Map<String, Movie> moviesById() {
        List<Movie> found = catalog.readWithVectors(properties.index().alias(), IDS);
        Map<String, Movie> byId = new HashMap<>();
        for (Movie movie : found) {
            byId.put(movie.id(), movie);
        }
        assertThat(byId.keySet()).containsExactlyInAnyOrderElementsOf(IDS);
        return byId;
    }

    private static float[] floats(JsonNode node) {
        float[] values = new float[node.size()];
        for (int index = 0; index < node.size(); index++) {
            values[index] = node.get(index).floatValue();
        }
        return values;
    }
}
