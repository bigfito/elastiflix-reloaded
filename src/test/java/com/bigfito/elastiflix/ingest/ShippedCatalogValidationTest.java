package com.bigfito.elastiflix.ingest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.csv.CsvMovieReader;
import com.bigfito.elastiflix.ingest.csv.CsvRead;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.json.JsonDocuments;
import com.bigfito.elastiflix.ingest.json.JsonRead;
import com.bigfito.elastiflix.ingest.mapping.MappingValidator;
import com.bigfito.elastiflix.ingest.mapping.ValidatedMovies;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las 14 partes de {@code data/} se cargan sin rechazos y traen la procedencia del sidecar.
 */
class ShippedCatalogValidationTest {

    private static final int MOVIES = 6_899;
    private static final int WITH_TAGS = 6_898;
    private static final String WITHOUT_TAGS = "751432";
    private static final Path DATA = Path.of("data");

    private final MappingValidator validator = new MappingValidator();
    private final JsonDocuments json = new JsonDocuments();
    private final CsvMovieReader csv = new CsvMovieReader(new IndexMapping());

    @Test
    @DisplayName("los 7 JSON y los 7 CSV se aceptan, con procedencia en 6 898 películas")
    void shouldLoadEveryPartWithEnrichment() {
        Map<String, Integer> jsonWarnings = new TreeMap<>();
        Map<String, Integer> csvWarnings = new TreeMap<>();
        List<Movie> fromJson = new ArrayList<>();
        List<Movie> fromCsv = new ArrayList<>();

        for (int part = 1; part <= 7; part++) {
            String name = "elastiflix-reloaded_0" + part;
            fromJson.addAll(readJson(DATA.resolve(name + ".json"), jsonWarnings));
            fromCsv.addAll(readCsv(DATA.resolve(name + ".csv"), csvWarnings));
        }

        assertThat(fromJson).hasSize(MOVIES).isEqualTo(fromCsv);
        assertThat(fromJson).filteredOn(movie -> movie.enrichment() != null).hasSize(WITH_TAGS);
        assertThat(fromJson).filteredOn(movie -> WITHOUT_TAGS.equals(movie.id()))
                .singleElement()
                .extracting(Movie::enrichment)
                .isNull();
        assertThat(csvWarnings).isEqualTo(jsonWarnings);
        assertThat(jsonWarnings).containsExactlyInAnyOrderEntriesOf(Map.of(
                ErrorCode.DUPLICATE_LIST_VALUE.code(), 268,
                ErrorCode.SUSPECT_PLOT.code(), 686,
                ErrorCode.DUPLICATE_PLOT.code(), 1));
    }

    private List<Movie> readJson(Path file, Map<String, Integer> warnings) {
        assertThat(file).as("parte JSON del catálogo").isRegularFile();
        JsonRead read = json.read(file);
        assertThat(read.warnings()).as(file::toString).isEmpty();
        return accepted(file, validator.validate(read.documents(), Set.of()), warnings);
    }

    private List<Movie> readCsv(Path file, Map<String, Integer> warnings) {
        CsvRead read = csv.read(file, CsvSettings.withHeader());
        assertThat(read.report().errors()).as(file::toString).isEmpty();
        count(warnings, read.report());
        return accepted(file, validator.validate(read.documents(), Set.of()), warnings);
    }

    private static List<Movie> accepted(Path file, ValidatedMovies checked, Map<String, Integer> warnings) {
        assertThat(checked.report().errors()).as(file::toString).isEmpty();
        assertThat(checked.report().accepted()).as(file::toString).isTrue();
        count(warnings, checked.report());
        return checked.movies();
    }

    private static void count(Map<String, Integer> warnings, ValidationReport report) {
        for (ValidationIssue issue : report.warnings()) {
            warnings.merge(issue.code(), 1, Integer::sum);
        }
    }
}
