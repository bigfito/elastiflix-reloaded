package com.bigfito.elastiflix.ingest.service;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.job.ImportMode;
import com.bigfito.elastiflix.ingest.job.FileIntake;
import com.bigfito.elastiflix.ingest.job.ImportRunner;
import com.bigfito.elastiflix.ingest.job.IndividualRecord;
import com.bigfito.elastiflix.ingest.job.JobStore;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Una excepción que termina un hilo de la carga queda en el log de la aplicación con el nombre del hilo.
 */
class JobThreadsTest {

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void setUp() {
        logs.start();
        logger().addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger().detachAppender(logs);
    }

    @Test
    @DisplayName("el manejador registra EFX-IMP-007 con el hilo y el tipo, sin el mensaje")
    void shouldLogTheThreadAndTheTypeWhenATaskThrows() throws Exception {
        // when
        Thread thread = JobThreads.start("ingest-validate-j1", () -> {
            throw new IllegalStateException("contenido del archivo");
        });
        thread.join();

        // then
        assertThat(messages()).containsExactly("EFX-IMP-007 hilo=ingest-validate-j1 tipo=IllegalStateException");
        assertThat(logs.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.ERROR);
    }

    @Test
    @DisplayName("cada validación corre en un hilo con el id de su trabajo")
    void shouldNameTheValidationThreadAfterTheJob() {
        // given
        JobStore store = mock(JobStore.class);
        when(store.open(any(), any(), any(), any())).thenReturn("job-7");
        FileIntake intake = mock(FileIntake.class);
        doThrow(new IllegalStateException("lectura")).when(intake).validate("job-7");
        IngestService service = new IngestService(store, intake, mock(ImportRunner.class),
                mock(IndividualRecord.class), new ElastiflixProperties(null, null, null, null, null, null, null));

        // when
        service.acceptFile("a.json", Path.of("a.json"), CsvSettings.withHeader(), ImportMode.APPEND);

        // then
        assertThat(awaitMessages())
                .containsExactly("EFX-IMP-007 hilo=ingest-validate-job-7 tipo=IllegalStateException");
    }

    private List<String> awaitMessages() {
        long end = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < end && messages().isEmpty()) {
            LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
        }
        return messages();
    }

    private List<String> messages() {
        synchronized (logs) {
            return logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        }
    }

    private static Logger logger() {
        return (Logger) LoggerFactory.getLogger(JobThreads.class);
    }
}
