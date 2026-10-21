package com.bigfito.elastiflix.ingest.job;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.mapping.ValidatedMovies;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;
import com.bigfito.elastiflix.ingest.mapping.WarningCount;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Un trabajo no guarda su informe entero: como mucho {@link JobRetention#MAX_KEPT_ERRORS} errores, y las
 * advertencias solo agrupadas. Las cifras de la página siguen siendo las del archivo entero.
 */
class ReportCapTest {

    private static final int CAP = JobRetention.MAX_KEPT_ERRORS;
    private static final int EXTRA = 5;

    private final JobStore store =
            new JobStore(new ElastiflixProperties(null, null, null, null, null, null, null), Clock.systemUTC());

    @TempDir
    private Path folder;

    @Test
    @DisplayName("con más errores que el tope, guarda los primeros y la página conserva el total real")
    void shouldKeepTheFirstErrorsAndTheRealTotalWhenTheReportPassesTheCap() throws Exception {
        // given
        String jobId = store.open("peliculas.csv", upload(), CsvSettings.withHeader());
        ValidationReport full = new ValidationReport(oneErrorAndOneWarningPerRow(CAP + EXTRA));

        // when
        store.invalid(jobId, full, JobSteps.VALIDATION);

        // then
        JobView view = store.view(jobId);
        assertThat(view.errorTotal()).isEqualTo(CAP + EXTRA);
        assertThat(view.invalidRecords()).isEqualTo(CAP + EXTRA);
        assertThat(view.warnings()).singleElement().extracting(WarningCount::count).isEqualTo(CAP + EXTRA);
        List<ValidationIssue> kept = store.require(jobId).kept().report().issues();
        assertThat(kept).hasSize(CAP).noneMatch(ValidationIssue::warning);
        assertThat(kept.getFirst().recordLabel()).isEqualTo("2");
        assertThat(kept.getLast().recordLabel()).isEqualTo(Integer.toString(CAP + 1));
        assertThat(store.errorReport(jobId).lines()).hasSize(CAP + 1);
    }

    @Test
    @DisplayName("por debajo del tope, el informe descargable trae todos los errores")
    void shouldKeepEveryErrorWhenTheReportIsBelowTheCap() throws Exception {
        // given
        String jobId = store.open("peliculas.csv", upload(), CsvSettings.withHeader());

        // when
        store.invalid(jobId, new ValidationReport(oneErrorAndOneWarningPerRow(3)), JobSteps.VALIDATION);

        // then
        assertThat(store.view(jobId).errorTotal()).isEqualTo(3);
        assertThat(store.require(jobId).kept().report().issues()).hasSize(3);
        assertThat(store.errorReport(jobId).lines()).hasSize(4);
    }

    @Test
    @DisplayName("un trabajo valido no guarda sus advertencias sueltas, solo agrupadas")
    void shouldKeepOnlyGroupedWarningsWhenTheFileIsValid() throws Exception {
        // given
        String jobId = store.open("peliculas.json", upload(), CsvSettings.withHeader());
        List<ValidationIssue> warnings = new ArrayList<>();
        for (int row = 1; row <= 50; row++) {
            warnings.add(warning(Integer.toString(row)));
        }
        Movie movie = MovieJsonMapper.create().readValue("{\"id\":\"1\",\"title\":\"T\",\"plot\":\"p\"}", Movie.class);

        // when
        store.ready(jobId, new ValidatedMovies(List.of(movie), new ValidationReport(warnings), 0));

        // then
        assertThat(store.view(jobId).status()).isEqualTo("valido");
        assertThat(store.view(jobId).warnings()).singleElement().extracting(WarningCount::count).isEqualTo(50);
        assertThat(store.require(jobId).kept().report().issues()).isEmpty();
    }

    private Path upload() throws Exception {
        return Files.writeString(Files.createTempFile(folder, "subida-", ".csv"), "id,title,plot\n");
    }

    private static List<ValidationIssue> oneErrorAndOneWarningPerRow(int rows) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (int row = 2; row < 2 + rows; row++) {
            String label = Integer.toString(row);
            issues.add(new ValidationIssue("EFX-ING-012", label, label, "runtime", "\"x\"", "un número entero", false));
            issues.add(warning(label));
        }
        return issues;
    }

    private static ValidationIssue warning(String label) {
        return new ValidationIssue("EFX-ING-022", label, label, "tagline", "en blanco", "sin valor", true);
    }
}
