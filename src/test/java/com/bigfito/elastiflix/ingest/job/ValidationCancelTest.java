package com.bigfito.elastiflix.ingest.job;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.mapping.MappingValidator;
import com.bigfito.elastiflix.ingest.service.MovieReaderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Cancelar durante {@code validando} corta entre etapas: no valida un archivo ya leído ni pregunta a
 * Elasticsearch por los ids de un archivo ya validado.
 */
class ValidationCancelTest {

    private final ElastiflixProperties properties = new ElastiflixProperties(null, null, null, null, null, null, null);
    private final JobStore store = new JobStore(properties, Clock.systemUTC());
    private final CatalogIndex catalog = mock(CatalogIndex.class);
    private final FileGate gate = spy(new FileGate(new MovieReaderFactory(new IndexMapping())));
    private final MappingValidator validator = spy(new MappingValidator());
    private final FileIntake intake = new FileIntake(gate, validator, catalog, properties, store);

    @TempDir
    private Path folder;

    @Test
    @DisplayName("cancelar mientras se lee el archivo no lo valida contra el mapping")
    void shouldNotValidateWhenCancelledWhileReading() throws Exception {
        // given
        String jobId = upload();
        doAnswer(invocation -> {
            store.cancel(jobId);
            return invocation.callRealMethod();
        }).when(gate).open(any());

        // when
        intake.validate(jobId);

        // then
        assertThat(store.view(jobId).status()).isEqualTo("cancelado");
        verify(validator, never()).validateAndRelease(anyList(), anyList(), any());
        verifyNoInteractions(catalog);
    }

    @Test
    @DisplayName("cancelar mientras se valida no pregunta a Elasticsearch qué ids ya existen")
    void shouldNotAskForExistingIdsWhenCancelledWhileValidating() throws Exception {
        // given
        String jobId = upload();
        doAnswer(invocation -> {
            store.cancel(jobId);
            return invocation.callRealMethod();
        }).when(validator).validateAndRelease(anyList(), anyList(), any());

        // when
        intake.validate(jobId);

        // then
        assertThat(store.view(jobId).status()).isEqualTo("cancelado");
        verify(catalog, never()).existingIds(anyString(), anyList());
    }

    @Test
    @DisplayName("un trabajo cancelado antes de empezar ni siquiera lee el archivo")
    void shouldNotReadWhenCancelledBeforeStarting() throws Exception {
        // given
        String jobId = upload();
        store.cancel(jobId);

        // when
        intake.validate(jobId);

        // then
        assertThat(store.view(jobId).status()).isEqualTo("cancelado");
        verify(gate, never()).open(any());
    }

    private String upload() throws Exception {
        Path file = Files.writeString(folder.resolve("subida.bin"), "id,title,plot\n1,A,Sinopsis de prueba suficientemente larga para validar.\n2,B,Otra sinopsis de prueba suficientemente larga y distinta.\n");
        return store.open("peliculas.csv", file, CsvSettings.withHeader(), ImportMode.BULK);
    }
}
