package com.bigfito.elastiflix.ingest.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.catalog.IndexNames;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.embedding.EmbeddingService;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.job.FileGate;
import com.bigfito.elastiflix.ingest.job.FileIntake;
import com.bigfito.elastiflix.ingest.job.ImportJob;
import com.bigfito.elastiflix.ingest.job.ImportRunner;
import com.bigfito.elastiflix.ingest.job.IndividualRecord;
import com.bigfito.elastiflix.ingest.job.JobStore;
import com.bigfito.elastiflix.ingest.job.JobView;
import com.bigfito.elastiflix.ingest.job.OrphanCleaner;
import com.bigfito.elastiflix.ingest.mapping.MappingValidator;
import com.bigfito.elastiflix.ingest.vector.PlotVectorizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * La fachada rechaza un trabajo desconocido y un archivo con mala extensión sin llamar al catálogo.
 */
@ExtendWith(MockitoExtension.class)
class IngestServiceTest {

    private static final String ALIAS = "elastiflix-it-unit";

    @Mock
    private CatalogIndex catalog;

    @Mock
    private EmbeddingService embeddings;

    /** JUnit lo borra al terminar, pase o falle la prueba. */
    @TempDir
    private Path folder;

    @Test
    @DisplayName("un id que no está lanza EFX-IMP-005")
    void shouldRejectAnUnknownJob() {
        IngestService service = service();

        assertThatThrownBy(() -> service.job("no-esta"))
                .isInstanceOf(ImportException.class)
                .extracting(exception -> ((ImportException) exception).errorCode())
                .isEqualTo(ErrorCode.UNKNOWN_JOB);
    }

    @Test
    @DisplayName("una extensión que no es json ni csv no consulta el catálogo y borra el archivo subido")
    void shouldRejectABadExtensionWithoutCallingTheCatalog() throws Exception {
        // given
        IngestService service = service();
        Path path = Files.writeString(folder.resolve("carga.txt"), "hola");

        // when: el estado pasa a invalido un instante antes de que se borre el archivo, así que se esperan
        // las dos cosas
        String jobId = service.acceptFile("peliculas.txt", path, CsvSettings.withHeader());
        JobView view = waitUntilSettled(service, jobId, path);

        // then
        assertThat(view.status()).isEqualTo("invalido");
        assertThat(view.errors()).extracting("code").contains(ErrorCode.BAD_EXTENSION.code());
        verify(catalog, never()).existingIds(anyString(), anyList());
        verify(catalog, never()).indexOne(anyString(), org.mockito.ArgumentMatchers.any());
        assertThat(path).doesNotExist();
    }

    private IngestService service() {
        ElastiflixProperties properties = new ElastiflixProperties(
                null, new ElastiflixProperties.Index(ALIAS), null, null, null, null, null);
        JobStore store = new JobStore(properties, Clock.systemUTC());
        IndexNames names = new IndexNames();
        PlotVectorizer vectorizer = new PlotVectorizer(embeddings);
        return new IngestService(
                store,
                new FileIntake(new FileGate(new MovieReaderFactory(new IndexMapping())),
                        new MappingValidator(), catalog, properties, store),
                new ImportRunner(new ImportJob(
                        catalog, names, Clock.systemUTC(), vectorizer, new OrphanCleaner(names)),
                        store, properties),
                new IndividualRecord(new MappingValidator(), vectorizer, catalog),
                properties);
    }

    /** Espera, como mucho 5 segundos, a que el trabajo deje de validar y el archivo subido ya no exista. */
    private static JobView waitUntilSettled(IngestService service, String jobId, Path upload) {
        long end = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        JobView view = service.job(jobId);
        while (System.nanoTime() < end && (stillRunning(view.status()) || Files.exists(upload))) {
            LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
            view = service.job(jobId);
        }
        return view;
    }

    private static boolean stillRunning(String status) {
        return "recibido".equals(status) || "validando".equals(status);
    }
}
