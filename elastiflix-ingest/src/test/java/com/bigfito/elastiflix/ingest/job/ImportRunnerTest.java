package com.bigfito.elastiflix.ingest.job;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.catalog.CatalogName;
import com.bigfito.elastiflix.core.catalog.IndexNames;
import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.embedding.EmbeddingService;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.mapping.ValidatedMovies;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;
import com.bigfito.elastiflix.ingest.vector.PlotVectorizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    @DisplayName("si el aviso del paso 5 lanza después de publicar, el trabajo queda publicado y no fallido")
    void shouldStayPublishedWhenTheStepFiveNoticeFails() throws Exception {
        // given
        String alias = "elastiflix-it-x7k2";
        String live = alias + "-20261018-000000";
        Clock clock = Clock.fixed(Instant.parse("2026-10-18T03:04:05Z"), ZoneOffset.UTC);
        String temp = new IndexNames().timestamped(alias, clock);
        CatalogIndex catalog = mock(CatalogIndex.class);
        when(catalog.describe(alias)).thenReturn(new CatalogName.Alias(alias, live));
        when(catalog.count(temp)).thenReturn(1L);
        EmbeddingService embeddings = mock(EmbeddingService.class);
        float[] vector = new float[1024];
        vector[0] = 1f;
        when(embeddings.embedDocuments(anyList())).thenReturn(List.of(vector));
        ElastiflixProperties withAlias = new ElastiflixProperties(
                null, new ElastiflixProperties.Index(alias), null, null, null, null, null);
        JobStore spied = spy(new JobStore(withAlias, Clock.systemUTC()));
        String jobId = importing(spied);
        doThrow(new ImportException(ErrorCode.UNKNOWN_JOB))
                .when(spied).onProgress(eq(jobId), argThat(notice -> notice.step() == 5));
        ImportJob real = new ImportJob(catalog, new IndexNames(), clock, new PlotVectorizer(embeddings),
                new OrphanCleaner(new IndexNames()));

        // when
        new ImportRunner(real, spied, withAlias).run(jobId);

        // then
        verify(catalog).publish(alias, temp, live);
        assertThat(spied.view(jobId).status()).isEqualTo(JobStatus.PUBLISHED.label());
        assertThat(spied.view(jobId).failureCode()).isEmpty();
    }

    private String importing() throws Exception {
        return importing(store);
    }

    private String importing(JobStore store) throws Exception {
        String jobId = store.open("a.json", Files.writeString(folder.resolve("a.json"), "[]"),
                CsvSettings.withHeader(), ImportMode.BULK);
        Movie movie = MovieJsonMapper.create().readValue("{\"id\":\"1\",\"title\":\"T\",\"plot\":\"p\"}", Movie.class);
        store.ready(jobId, new ValidatedMovies(List.of(movie), ValidationReport.empty(), 0));
        assertThat(store.beginImport(jobId)).isTrue();
        return jobId;
    }
}
