package com.bigfito.elastiflix.core.embedding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.model.Movie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Mide el coseno de las seis combinaciones de documento contra el catálogo.
 *
 * <p>Solo lee. Escribe la tabla en {@code target/}, para no modificar el diseño en cada
 * verificación. Los números medidos se copian después a {@code docs/diseno/1.md}. Además afirma
 * que la combinación configurada sigue reproduciendo los vectores guardados: si el modelo cambia,
 * la prueba falla y hay que volver a calibrar.</p>
 */
@SpringBootTest
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
     * @param properties alias configurado y combinación de documento elegida
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
    @DisplayName("mide las seis combinaciones, deja la tabla en target y la configurada sirve")
    void shouldMeasureDocumentCombinations() throws IOException {
        // given
        Map<String, Movie> movies = moviesById();

        // when
        List<Measurement> measurements = new ArrayList<>();
        for (Attempt attempt : attempts()) {
            measurements.add(measure(attempt, movies));
        }
        writeTable(measurements);

        // then: cada coseno es un número finito y la combinación que usa la demo sirve
        for (Measurement measurement : measurements) {
            assertThat(measurement.cosines()).allSatisfy(cosine -> assertThat(cosine).isFinite());
        }
        Measurement configured = configuredOf(measurements);
        assertThat(configured.cosines())
                .as("coseno de %s con plot_1024 para %s", configured.attempt().describe(), IDS)
                .allSatisfy(cosine -> assertThat(cosine).isGreaterThanOrEqualTo(USEFUL));
    }

    private Measurement measure(Attempt attempt, Map<String, Movie> movies) {
        List<Double> cosines = new ArrayList<>();
        for (String id : IDS) {
            cosines.add(cosineOf(movies.get(id), attempt));
        }
        return new Measurement(attempt, cosines);
    }

    private double cosineOf(Movie movie, Attempt attempt) {
        float[] generated = embeddings.embedForCalibration(movie.plot(), attempt.inputType(), attempt.prefix());
        return CosineSimilarity.between(generated, movie.plot1024());
    }

    /**
     * Busca la medición de la combinación configurada. Un valor vacío en las propiedades equivale a
     * no enviar el {@code input_type} o no anteponer prefijo, igual que el {@code null} de los intentos.
     */
    private Measurement configuredOf(List<Measurement> measurements) {
        ElastiflixProperties.Embedding embedding = properties.embedding();
        String inputType = blankToNull(embedding.documentInputType());
        String prefix = blankToNull(embedding.documentPrefix());
        return measurements.stream()
                .filter(measurement -> measurement.attempt().matches(inputType, prefix))
                .findFirst()
                .orElseThrow(() -> new AssertionError("La combinación configurada ("
                        + inputType + ", " + prefix + ") no está entre las seis medidas"));
    }

    private Map<String, Movie> moviesById() {
        List<Movie> found = catalog.readWithVectors(properties.index().alias(), IDS);
        Map<String, Movie> byId = new HashMap<>();
        for (Movie movie : found) {
            byId.put(movie.id(), movie);
        }
        // El catálogo vivo puede cambiar con una importación: sin estas películas no hay contra qué
        // medir, así que la prueba se omite en lugar de fallar.
        assumeTrue(byId.keySet().containsAll(IDS),
                () -> "El catálogo ya no tiene las películas " + IDS + "; solo encontró " + byId.keySet());
        for (Movie movie : byId.values()) {
            assertThat(movie.plot()).isNotBlank();
            assertThat(movie.plot1024()).hasSize(VectorProjector.MODEL_DIMENSIONS);
        }
        return byId;
    }

    private static void writeTable(List<Measurement> measurements) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("| input_type | prefijo | " + String.join(" | ", IDS) + " | ¿Sirve? |");
        lines.add("|---|---|---|---|---|---|---|---|");
        for (Measurement measurement : measurements) {
            lines.add(measurement.toRow());
        }
        Files.createDirectories(TABLE.getParent());
        Files.writeString(TABLE, String.join("\n", lines) + "\n");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
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

        boolean matches(String otherInputType, String otherPrefix) {
            return Objects.equals(inputType, otherInputType) && Objects.equals(prefix, otherPrefix);
        }

        String describe() {
            return inputLabel + " y " + prefixLabel;
        }
    }

    private record Measurement(Attempt attempt, List<Double> cosines) {

        boolean isUseful() {
            return cosines.stream().allMatch(cosine -> cosine >= USEFUL);
        }

        String toRow() {
            List<String> cells = new ArrayList<>();
            cells.add(attempt.inputLabel());
            cells.add(attempt.prefixLabel());
            for (double cosine : cosines) {
                cells.add(String.format(Locale.ROOT, "%.6f", cosine));
            }
            cells.add(isUseful() ? "sí" : "no");
            return "| " + String.join(" | ", cells) + " |";
        }
    }
}
