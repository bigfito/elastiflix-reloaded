package com.bigfito.elastiflix.ingest.job;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.time.Clock;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.json.JsonDocuments;
import com.bigfito.elastiflix.ingest.mapping.MappingValidator;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;
import com.bigfito.elastiflix.ingest.mapping.ValidatedMovies;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Un error imprevisto no deja el trabajo en {@code validando} ni en {@code importando}.
 */
class JobThreadFailureTest {

    private static final ElastiflixProperties NO_SETTINGS =
            new ElastiflixProperties(null, null, null, null, null, null, null);

    @TempDir
    private Path folder;

    @Test
    @DisplayName("si la lectura truena, el trabajo queda fallido con EFX-IMP-007")
    void shouldFailTheJobWhenValidationThrows() throws Exception {
        JobStore store = newStore();
        String jobId = store.open("peliculas.json", temp(), CsvSettings.withHeader(), ImportMode.BULK);
        FileGate gate = mock(FileGate.class);
        doThrow(new IllegalStateException("lectura")).when(gate).open(any());
        FileIntake intake = new FileIntake(gate, new MappingValidator(), mock(CatalogIndex.class), null, store);

        assertThatThrownBy(() -> intake.validate(jobId)).isInstanceOf(IllegalStateException.class);

        assertThat(store.view(jobId).status()).isEqualTo(JobStatus.FAILED.label());
        assertThat(store.view(jobId).failureCode()).isEqualTo(ErrorCode.JOB_FAILED.code());
    }

    @Test
    @DisplayName("si la importación truena, el trabajo no se queda importando")
    void shouldFailTheJobWhenImportThrows() throws Exception {
        JobStore store = newStore();
        String jobId = store.open("peliculas.json", temp(), CsvSettings.withHeader(), ImportMode.BULK);
        store.require(jobId).ready(new ValidatedMovies(List.of(), new ValidationReport(List.of()), 0));
        store.beginImport(jobId);
        ImportJob job = mock(ImportJob.class);
        doThrow(new IllegalStateException("lote")).when(job).run(any(), any(), any(), any(), any());
        ImportRunner runner = new ImportRunner(job, mock(AppendJob.class), store, NO_SETTINGS);

        assertThatThrownBy(() -> runner.run(jobId)).isInstanceOf(IllegalStateException.class);

        assertThat(store.view(jobId).status()).isEqualTo(JobStatus.FAILED.label());
        assertThat(store.view(jobId).failureCode()).isEqualTo(ErrorCode.JOB_FAILED.code());
    }

    @Test
    @DisplayName("al cerrar el trabajo se sueltan las películas")
    void shouldReleaseMoviesWhenTheJobEnds() throws Exception {
        JobStore store = newStore();
        String jobId = store.open("peliculas.json", temp(), CsvSettings.withHeader(), ImportMode.BULK);
        ValidatedMovies checked = new MappingValidator().validate(
                new JsonDocuments().read(Path.of("..", "elasticsearch", "fixtures", "movies-sample.json")).documents(),
                Set.of());
        store.require(jobId).ready(checked);
        assertThat(store.require(jobId).movies()).isNotEmpty();

        store.failed(jobId, new ImportException(ErrorCode.JOB_FAILED));

        assertThat(store.require(jobId).movies()).isEmpty();
    }

    @Test
    @DisplayName("el avance de un trabajo no se anota en otro")
    void shouldKeepProgressOnTheJobThatMoved() throws Exception {
        JobStore store = newStore();
        String first = store.open("a.json", temp(), CsvSettings.withHeader(), ImportMode.BULK);
        String second = store.open("b.json", temp(), CsvSettings.withHeader(), ImportMode.BULK);
        store.onProgress(first, new ImportProgress(4, 1, 3, 3, "temporal"));

        assertThat(store.view(first).written()).isEqualTo(3);
        assertThat(store.view(first).vectorized()).isEqualTo(3);
        assertThat(store.view(first).tempIndex()).isEqualTo("temporal");
        // La cifra de «Reemplazarán existentes» es la de la validación: el avance no la mueve.
        assertThat(store.view(first).replaceCount()).isZero();
        assertThat(store.view(second).written()).isZero();
    }

    private Path temp() throws Exception {
        return Files.createTempFile(folder, "elastiflix-job-", ".json");
    }

    private static JobStore newStore() {
        return new JobStore(NO_SETTINGS, Clock.systemUTC());
    }
}
