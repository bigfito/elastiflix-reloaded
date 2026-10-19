package com.bigfito.elastiflix.ingest.job;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.mapping.ValidatedMovies;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Los trabajos no retienen películas al terminar y la JVM no guarda trabajos para siempre.
 */
class JobRetentionTest {

    private final MovingClock clock = new MovingClock();
    private final JobStore store =
            new JobStore(new ElastiflixProperties(null, null, null, null, null, null, null), clock);

    @TempDir
    private Path folder;

    @Test
    @DisplayName("después de un estado final, el trabajo no retiene películas ni el archivo local")
    void shouldReleaseMoviesAfterAFinalState() throws Exception {
        // given
        Path file = upload();
        String jobId = store.open("a.json", file, CsvSettings.withHeader());
        store.ready(jobId, validated());
        assertThat(store.require(jobId).movies()).hasSize(1);

        // when
        store.cancel(jobId);

        // then
        assertThat(store.view(jobId).status()).isEqualTo("cancelado");
        assertThat(store.require(jobId).movies()).isEmpty();
        assertThat(file).doesNotExist();
    }

    @Test
    @DisplayName("con más de 20 trabajos terminados, se olvidan los más viejos")
    void shouldForgetTheOldestFinishedJobs() throws Exception {
        // given
        List<String> ids = new ArrayList<>();
        for (int index = 0; index < 22; index++) {
            String jobId = store.open("a.json", upload(), CsvSettings.withHeader());
            store.invalid(jobId, ValidationReport.empty(), 3);
            ids.add(jobId);
            clock.advance(Duration.ofSeconds(1));
        }

        // when
        store.open("b.json", upload(), CsvSettings.withHeader());

        // then
        assertUnknown(ids.get(0));
        assertUnknown(ids.get(1));
        assertThat(store.view(ids.get(2)).status()).isEqualTo("invalido");
        assertThat(store.view(ids.get(21)).status()).isEqualTo("invalido");
    }

    @Test
    @DisplayName("un trabajo importando no se expulsa ni vence, aunque sea el más viejo")
    void shouldNeverEvictAnImportingJob() throws Exception {
        // given
        String importing = store.open("a.json", upload(), CsvSettings.withHeader());
        store.ready(importing, validated());
        assertThat(store.beginImport(importing)).isTrue();
        clock.advance(Duration.ofHours(2));
        for (int index = 0; index < 25; index++) {
            String jobId = store.open("b.json", upload(), CsvSettings.withHeader());
            store.invalid(jobId, ValidationReport.empty(), 3);
        }

        // when
        store.open("c.json", upload(), CsvSettings.withHeader());

        // then
        assertThat(store.view(importing).status()).isEqualTo("importando");
        assertThat(store.require(importing).movies()).hasSize(1);
    }

    @Test
    @DisplayName("un trabajo valido sin actividad vence: queda cancelado y suelta las películas")
    void shouldExpireAnIdleValidJob() throws Exception {
        // given
        String jobId = store.open("a.json", upload(), CsvSettings.withHeader());
        store.ready(jobId, validated());
        clock.advance(JobRetention.IDLE_VALID.plusMinutes(1));

        // when
        store.open("b.json", upload(), CsvSettings.withHeader());

        // then
        assertThat(store.view(jobId).status()).isEqualTo("cancelado");
        assertThat(store.require(jobId).movies()).isEmpty();
    }

    @Test
    @DisplayName("un valido vence también cuando la página consulta otro trabajo, sin otra subida")
    void shouldExpireAnIdleValidJobWhenAnotherJobIsViewed() throws Exception {
        // given
        String idle = store.open("a.json", upload(), CsvSettings.withHeader());
        store.ready(idle, validated());
        String other = store.open("b.json", upload(), CsvSettings.withHeader());
        clock.advance(JobRetention.IDLE_VALID.plusMinutes(1));

        // when
        store.view(other);

        // then
        assertThat(store.require(idle).status()).isEqualTo(JobStatus.CANCELLED);
        assertThat(store.require(idle).movies()).isEmpty();
    }

    @Test
    @DisplayName("un valido vencido ya no se importa")
    void shouldNotImportAnExpiredValidJob() throws Exception {
        // given
        String jobId = store.open("a.json", upload(), CsvSettings.withHeader());
        store.ready(jobId, validated());
        clock.advance(JobRetention.IDLE_VALID.plusMinutes(1));

        // when
        boolean started = store.beginImport(jobId);

        // then
        assertThat(started).isFalse();
        assertThat(store.view(jobId).status()).isEqualTo("cancelado");
    }

    private void assertUnknown(String jobId) {
        assertThatThrownBy(() -> store.view(jobId)).isInstanceOf(ImportException.class);
    }

    private Path upload() throws Exception {
        return Files.writeString(Files.createTempFile(folder, "subida-", ".json"), "[]");
    }

    private static ValidatedMovies validated() {
        Movie movie = MovieJsonMapper.create().readValue("{\"id\":\"1\",\"title\":\"T\",\"plot\":\"p\"}", Movie.class);
        return new ValidatedMovies(List.of(movie), ValidationReport.empty(), 0);
    }

    /** Reloj que solo avanza cuando la prueba lo pide. */
    private static final class MovingClock extends Clock {

        private Instant now = Instant.parse("2026-10-18T03:04:05Z");

        void advance(Duration step) {
            now = now.plus(step);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
