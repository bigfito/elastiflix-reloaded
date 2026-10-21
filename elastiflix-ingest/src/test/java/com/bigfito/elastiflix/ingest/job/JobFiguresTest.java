package com.bigfito.elastiflix.ingest.job;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.time.Clock;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;
import com.bigfito.elastiflix.ingest.mapping.ValidatedMovies;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las cifras de la tarjeta de validación salen del archivo leído, también cuando es inválido.
 */
class JobFiguresTest {

    @TempDir
    private Path folder;

    @Test
    @DisplayName("512 leídos y 23 errores en 17 registros dan 495 válidos")
    void shouldCountValidRecordsWhenSomeHaveErrors() throws Exception {
        // given
        JobStore store = store(100);
        String jobId = store.open("peliculas-nuevas.csv", file(), CsvSettings.withHeader());
        store.fileRead(jobId, 512, List.of());

        // when
        store.invalid(jobId, new ValidationReport(errorsInRecords(23, 17)), 3);
        JobView view = store.view(jobId);

        // then
        assertThat(view.status()).isEqualTo("invalido");
        assertThat(view.fileCount()).isEqualTo(512);
        assertThat(view.errorTotal()).isEqualTo(23);
        assertThat(view.invalidRecords()).isEqualTo(17);
        assertThat(view.validCount()).isEqualTo(495);
        assertThat(view.errors()).hasSize(6);
        assertThat(view.kind()).isEqualTo("csv");
    }

    @Test
    @DisplayName("cada consulta de estado reutiliza las cifras del informe en lugar de recalcularlas")
    void shouldReuseTheReportFiguresWhenThePagePolls() throws Exception {
        // given
        JobStore store = store(100);
        String jobId = store.open("peliculas-nuevas.csv", file(), CsvSettings.withHeader());
        store.invalid(jobId, new ValidationReport(errorsInRecords(23, 17)), 3);

        // when
        JobView first = store.view(jobId);
        JobView second = store.view(jobId);

        // then
        assertThat(second.errors()).isSameAs(first.errors());
        assertThat(second.warnings()).isSameAs(first.warnings());
        assertThat(second.invalidRecords()).isEqualTo(17);
    }

    @Test
    @DisplayName("un error del encabezado no resta registros válidos")
    void shouldNotCountHeaderErrorsAsRecords() {
        List<ValidationIssue> errors = List.of(
                issue("encabezado", "director"), issue("37", "release_date"), issue("37", "adult"));

        assertThat(JobFigures.invalidRecords(errors)).isEqualTo(1);
        assertThat(JobFigures.validCount(512, 1)).isEqualTo(511);
    }

    @Test
    @DisplayName("los lotes siguen elastiflix.ingest.batch-size")
    void shouldUseTheConfiguredBatchSize() throws Exception {
        JobStore store = store(50);
        String jobId = store.open("a.json", file(), CsvSettings.withHeader());
        store.fileRead(jobId, 4322, List.of());

        assertThat(store.view(jobId).batchSize()).isEqualTo(50);
        assertThat(store.view(jobId).batches()).isEqualTo(87);
        assertThat(JobFigures.batches(4322, 100)).isEqualTo(44);
    }

    @Test
    @DisplayName("las comprobaciones no incluyen la de vectores que PLAN §1 elimina")
    void shouldLeaveOutTheVectorCheck() {
        assertThat(JobFigures.checks(JobStatus.VALID, 0, 4322))
                .containsExactly("4 322 registros leídos, con id, title y plot.", "Ningún id repetido.")
                .noneMatch(check -> check.contains("vector"));
    }

    @Test
    @DisplayName("mientras se valida contra el mapping no hay comprobaciones, aunque ya se leyó el archivo")
    void shouldShowNoChecksWhenTheJobIsStillValidating() throws Exception {
        // given
        JobStore store = store(100);
        String jobId = store.open("a.json", file(), CsvSettings.withHeader());
        store.require(jobId).validating();

        // when
        store.fileRead(jobId, 4322, List.of());
        JobView view = store.view(jobId);

        // then
        assertThat(view.status()).isEqualTo("validando");
        assertThat(view.fileCount()).isEqualTo(4322);
        assertThat(view.checks()).isEmpty();
    }

    @Test
    @DisplayName("un trabajo fallido con registros leídos no muestra comprobaciones")
    void shouldShowNoChecksWhenTheJobFailed() throws Exception {
        // given
        JobStore store = store(100);
        String jobId = store.open("a.json", file(), CsvSettings.withHeader());
        store.require(jobId).validating();
        store.fileRead(jobId, 4322, List.of());

        // when
        store.failed(jobId, new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE));
        JobView view = store.view(jobId);

        // then
        assertThat(view.status()).isEqualTo("fallido");
        assertThat(view.fileCount()).isEqualTo(4322);
        assertThat(view.checks()).isEmpty();
    }

    @Test
    @DisplayName("un trabajo valido muestra las dos comprobaciones")
    void shouldShowBothChecksWhenTheJobIsValid() throws Exception {
        // given
        JobStore store = store(100);
        String jobId = store.open("a.json", file(), CsvSettings.withHeader());
        Movie movie = MovieJsonMapper.create().readValue("{\"id\":\"1\",\"title\":\"T\",\"plot\":\"p\"}", Movie.class);

        // when
        store.ready(jobId, new ValidatedMovies(List.of(movie), ValidationReport.empty(), 0));

        // then
        assertThat(store.view(jobId).checks())
                .containsExactly("1 registros leídos, con id, title y plot.", "Ningún id repetido.");
    }

    @Test
    @DisplayName("el mensaje de falla es solo el «qué pasó»; el código lo pone la página")
    void shouldGiveTheSummaryWithoutTheCode() {
        assertThat(JobFigures.failureMessage("EFX-IMP-007")).isEqualTo(ErrorCode.JOB_FAILED.summary());
    }

    private JobStore store(int batchSize) {
        return new JobStore(new ElastiflixProperties(null, null, null,
                new ElastiflixProperties.Ingest(batchSize), null, null, null),
                Clock.systemUTC());
    }

    private Path file() throws Exception {
        return Files.createTempFile(folder, "subida-", ".bin");
    }

    private static List<ValidationIssue> errorsInRecords(int errors, int records) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (int index = 0; index < errors; index++) {
            issues.add(issue(Integer.toString(2 + index % records), "release_date"));
        }
        return issues;
    }

    private static ValidationIssue issue(String record, String field) {
        return new ValidationIssue("EFX-ING-010", record, "1", field, "x", "y", false);
    }
}
