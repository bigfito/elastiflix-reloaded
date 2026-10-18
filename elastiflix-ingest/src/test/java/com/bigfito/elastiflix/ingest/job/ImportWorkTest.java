package com.bigfito.elastiflix.ingest.job;

import java.nio.file.Path;

import com.bigfito.elastiflix.ingest.csv.CsvSettings;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Un trabajo cancelado no vuelve atrás.
 */
class ImportWorkTest {

    @Test
    @DisplayName("si se canceló antes de validar, validating() no lo pasa a validando")
    void shouldStayCancelledWhenValidationStartsLate() {
        // given
        ImportWork work = new ImportWork("j1", "a.json", Path.of("no-existe.json"), CsvSettings.withHeader(), 0);
        work.requestCancel();

        // when
        work.validating();

        // then
        assertThat(work.status()).isEqualTo(JobStatus.CANCELLED);
    }
}
