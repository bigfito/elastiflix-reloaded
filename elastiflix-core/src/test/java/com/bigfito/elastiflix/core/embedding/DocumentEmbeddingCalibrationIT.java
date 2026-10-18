package com.bigfito.elastiflix.core.embedding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.bigfito.elastiflix.core.TestApplication;
import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.model.Movie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mide el coseno de las seis combinaciones de documento contra el catálogo.
 *
 * <p>Solo lee. Escribe la tabla en {@code target/}, para no modificar el diseño en cada
 * verificación. Los números medidos se copian después a {@code docs/diseno/1.md}.</p>
 */
@SpringBootTest(classes = TestApplication.class)
class DocumentEmbeddingCalibrationIT {

    private static final List<String> IDS = List.of("597", "27205", "10681", "345887", "134");
    private static final double USEFUL = 0.999;
    private static final String DOCUMENT_PREFIX = "Document: ";
    private static final Path TABLE = Path.of("target", "calibration-table.md");

    private final CatalogIndex catalog;
    private final ElastiflixProperties properties;
    private final InferenceEmbeddingService embeddings;

    /**
     * Spring no inyecta el constructor de una prueba si no lleva {@code @Autowired}.
     *
     * @param catalog    lectura del catálogo, con vectores
     * @param properties alias configurado
     * @param embeddings servicio que expone la calibración
     */
    @Autowired
    DocumentEmbeddingCalibrationIT(
            CatalogIndex catalog, ElastiflixProperties properties, InferenceEmbeddingService embeddings) {
        this.catalog = catalog;
        this.properties = properties;
        this.embeddings = embeddings;
    }

    @Test
    @DisplayName("mide las seis combinaciones y deja la tabla en target")
    void shouldMeasureDocumentCombinations() throws IOException {
        Map<String, Movie> movies = moviesById();
        List<String> lines = new ArrayList<>();
        lines.add(header());
        lines.add("|---|---|---|---|---|---|---|---|");
        for (Attempt attempt : attempts()) {
            lines.add(measure(attempt, movies));
        }
        Files.createDirectories(TABLE.getParent());
        Files.writeString(TABLE, String.join("\n", lines) + "\n");
        assertThat(TABLE).isRegularFile();
    }

    private String measure(Attempt attempt, Map<String, Movie> movies) {
        List<String> cells = new ArrayList<>();
        cells.add(attempt.inputLabel());
        cells.add(attempt.prefixLabel());
        boolean useful = true;
        for (String id : IDS) {
            double cosine = cosineOf(movies.get(id), attempt);
            cells.add(String.format(Locale.ROOT, "%.6f", cosine));
            if (cosine < USEFUL) {
                useful = false;
            }
        }
        cells.add(useful ? "sí" : "no");
        return "| " + String.join(" | ", cells) + " |";
    }

    private double cosineOf(Movie movie, Attempt attempt) {
        float[] generated = embeddings.embedForCalibration(movie.plot(), attempt.inputType(), attempt.prefix());
        return CosineSimilarity.between(generated, movie.plot1024());
    }

    private Map<String, Movie> moviesById() {
        List<Movie> found = catalog.readWithVectors(properties.index().alias(), IDS);
        Map<String, Movie> byId = new HashMap<>();
        for (Movie movie : found) {
            byId.put(movie.id(), movie);
        }
        assertThat(byId.keySet()).containsExactlyInAnyOrderElementsOf(IDS);
        for (Movie movie : byId.values()) {
            assertThat(movie.plot()).isNotBlank();
            assertThat(movie.plot1024()).hasSize(VectorProjector.MODEL_DIMENSIONS);
        }
        return byId;
    }

    private static String header() {
        return "| input_type | prefijo | " + String.join(" | ", IDS) + " | ¿Sirve? |";
    }

    private static List<Attempt> attempts() {
        return List.of(
                new Attempt("ingest", DOCUMENT_PREFIX, "`ingest`", "`Document: `"),
                new Attempt("ingest", null, "`ingest`", "sin prefijo"),
                new Attempt("search", DOCUMENT_PREFIX, "`search`", "`Document: `"),
                new Attempt("search", null, "`search`", "sin prefijo"),
                new Attempt(null, DOCUMENT_PREFIX, "ausente", "`Document: `"),
                new Attempt(null, null, "ausente", "sin prefijo"));
    }

    private record Attempt(String inputType, String prefix, String inputLabel, String prefixLabel) {
    }
}
