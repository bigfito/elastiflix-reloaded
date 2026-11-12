package com.bigfito.elastiflix.ingest.job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.file.FileKind;
import com.bigfito.elastiflix.ingest.mapping.MappingValidator;
import com.bigfito.elastiflix.ingest.mapping.ValidatedMovies;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;
import com.bigfito.elastiflix.ingest.service.LoadedDocuments;
import com.bigfito.elastiflix.ingest.service.MovieReaderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las 14 partes de {@code data/} pasan por el mismo camino que una carga, sin rechazos ni advertencias.
 *
 * <p>Los 7 {@code .json} y los 7 {@code .csv} dan las mismas películas, y todas traen {@code enrichment}:
 * desde WP-04, toda sinopsis lleva su etiqueta de origen ({@code wikipedia_plot_AAAA-MM-DD},
 * {@code tmdb_overview} u {@code unverified} cuando se desconoce).</p>
 */
class ShippedCatalogValidationTest {

    private static final int PARTS = 14;
    private static final int MOVIES = 6_899;
    private static final int WITH_TAGS = 6_899;

    private final MovieReaderFactory readers = new MovieReaderFactory(new IndexMapping());
    private final MappingValidator validator = new MappingValidator();

    @Test
    @DisplayName("cada parte json y csv se acepta sin advertencias, y las dos versiones del catálogo coinciden")
    void shouldAcceptEveryShippedPart() throws IOException {
        List<Movie> fromJson = new ArrayList<>();
        List<Movie> fromCsv = new ArrayList<>();
        try (Stream<Path> listed = Files.list(Path.of("data"))) {
            List<Path> parts = listed
                    .filter(ShippedCatalogValidationTest::isPart)
                    .sorted()
                    .toList();
            assertThat(parts).hasSize(PARTS);
            for (Path part : parts) {
                boolean json = part.toString().endsWith(".json");
                (json ? fromJson : fromCsv).addAll(accept(part, json ? FileKind.JSON : FileKind.CSV));
            }
        }

        assertThat(fromJson).hasSize(MOVIES).isEqualTo(fromCsv);
        assertThat(fromJson).filteredOn(movie -> movie.enrichment() != null).hasSize(WITH_TAGS);
        assertThat(fromJson).allSatisfy(movie -> assertThat(movie.enrichment().get("plot"))
                .as("enrichment.plot de %s", movie.id())
                .isNotBlank());
    }

    private List<Movie> accept(Path part, FileKind kind) {
        String name = part.getFileName().toString();
        LoadedDocuments loaded = readers.load(kind, part, CsvSettings.withHeader());
        ValidatedMovies checked = validator.validateAndRelease(
                new ArrayList<>(loaded.documents()), loaded.labels(), Set.of());
        ValidationReport report = ReportOrder.join(loaded.report(), checked.report());

        assertThat(report.errors()).as(name).isEmpty();
        assertThat(checked.movies()).as(name).hasSize(loaded.recordCount());
        assertThat(report.warnings()).as(name).isEmpty();
        return checked.movies();
    }

    private static boolean isPart(Path path) {
        String name = path.getFileName().toString();
        return name.startsWith("elastiflix-reloaded_")
                && (name.endsWith(".json") || name.endsWith(".csv"));
    }
}
