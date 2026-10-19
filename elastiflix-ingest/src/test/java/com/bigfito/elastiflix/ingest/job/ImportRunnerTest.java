package com.bigfito.elastiflix.ingest.job;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.mapping.ValidatedMovies;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * {@link ImportRunner} deja el estado final que corresponde a lo que pasó en {@link ImportJob}, sin
 * Elasticsearch: {@code publicado}, {@code cancelado} o {@code fallido}.
 */
class ImportRunnerTest {

    private final ElastiflixProperties properties = new ElastiflixProperties(null, null, null, null, null, null, null);
    private final JobStore store = new JobStore(properties, Clock.systemUTC());
    private final ImportJob job = mock(ImportJob.class);
    private final ImportRunner runner = new ImportRunner(job, store, properties);

    @TempDir
    private Path folder;

    @Test
    @DisplayName("si la importación termina, el trabajo queda publicado y en el paso 5")
    void shouldPublishWhenTheImportSucceeds() throws Exception {
        // given
        String jobId = importing();

        // when
        runner.run(jobId);

        // then
        assertThat(store.view(jobId).status()).isEqualTo(JobStatus.PUBLISHED.label());
        assertThat(store.view(jobId).step()).isEqualTo(5);
        assertThat(store.view(jobId).failureCode()).isEmpty();
    }

    @Test
    @DisplayName("si la importación se cancela (EFX-IMP-003), el trabajo queda cancelado y no fallido")
    void shouldCancelWhenTheImportIsCancelled() throws Exception {
        // given
        String jobId = importing();
        doThrow(new ImportException(ErrorCode.IMPORT_CANCELLED)).when(job).run(any(), any(), any(), any(), any());

        // when
        runner.run(jobId);

        // then
        assertThat(store.view(jobId).status()).isEqualTo(JobStatus.CANCELLED.label());
        assertThat(store.view(jobId).failureCode()).isEmpty();
    }

    @Test
    @DisplayName("si la importación falla con otro código, el trabajo queda fallido con ese código")
    void shouldFailWithTheCodeWhenTheImportFails() throws Exception {
        // given
        String jobId = importing();
        doThrow(new ImportException(ErrorCode.BULK_REJECTED)).when(job).run(any(), any(), any(), any(), any());

        // when
        runner.run(jobId);

        // then
        assertThat(store.view(jobId).status()).isEqualTo(JobStatus.FAILED.label());
        assertThat(store.view(jobId).failureCode()).isEqualTo(ErrorCode.BULK_REJECTED.code());
    }

    private String importing() throws Exception {
        String jobId = store.open("a.json", Files.writeString(folder.resolve("a.json"), "[]"),
                CsvSettings.withHeader());
        Movie movie = MovieJsonMapper.create().readValue("{\"id\":\"1\",\"title\":\"T\",\"plot\":\"p\"}", Movie.class);
        store.ready(jobId, new ValidatedMovies(List.of(movie), ValidationReport.empty(), 0));
        assertThat(store.beginImport(jobId)).isTrue();
        return jobId;
    }
}
