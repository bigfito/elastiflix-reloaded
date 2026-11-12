package com.bigfito.elastiflix.ingest.job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.file.FileKind;
import com.bigfito.elastiflix.ingest.mapping.MappingValidator;
import com.bigfito.elastiflix.ingest.mapping.ValidatedMovies;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;
import com.bigfito.elastiflix.ingest.service.LoadedDocuments;
import com.bigfito.elastiflix.ingest.service.MovieReaderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las 14 partes de {@code data/} pasan por el mismo camino que una carga y no se rechaza ninguna.
 */
class ShippedCatalogValidationTest {

    private static final Set<String> EXPECTED_WARNINGS = Set.of(
            ErrorCode.DUPLICATE_LIST_VALUE.code(),
            ErrorCode.SUSPECT_PLOT.code(),
            ErrorCode.DUPLICATE_PLOT.code());

    private final MovieReaderFactory readers = new MovieReaderFactory(new IndexMapping());
    private final MappingValidator validator = new MappingValidator();

    @Test
    @DisplayName("cada parte json y csv del catálogo se acepta, con solo advertencias de lista o de sinopsis")
    void shouldAcceptEveryShippedPart() throws IOException {
        try (Stream<Path> listed = Files.list(Path.of("data"))) {
            List<Path> parts = listed
                    .filter(ShippedCatalogValidationTest::isPart)
                    .sorted()
                    .toList();
            assertThat(parts).hasSize(14);
            for (Path part : parts) {
                accept(part);
            }
        }
    }

    private void accept(Path part) {
        FileKind kind = part.toString().endsWith(".json") ? FileKind.JSON : FileKind.CSV;
        LoadedDocuments loaded = readers.load(kind, part, CsvSettings.withHeader());
        ValidatedMovies checked = validator.validateAndRelease(
                new ArrayList<>(loaded.documents()), loaded.labels(), Set.of());
        ValidationReport report = ReportOrder.join(loaded.report(), checked.report());

        assertThat(report.errors()).as(part.getFileName().toString()).isEmpty();
        assertThat(checked.movies()).as(part.getFileName().toString()).hasSize(loaded.recordCount());
        assertThat(report.warnings()).as(part.getFileName().toString())
                .extracting(ValidationIssue::code)
                .allMatch(EXPECTED_WARNINGS::contains);
    }

    private static boolean isPart(Path path) {
        String name = path.getFileName().toString();
        return name.startsWith("elastiflix-reloaded_")
                && (name.endsWith(".json") || name.endsWith(".csv"));
    }
}
