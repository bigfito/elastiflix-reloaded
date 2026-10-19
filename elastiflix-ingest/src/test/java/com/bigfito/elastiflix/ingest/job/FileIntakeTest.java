package com.bigfito.elastiflix.ingest.job;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.service.MovieReaderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * El rechazo de un archivo detiene el paso que lo rechazó: el 2 si falla la comprobación del archivo y el
 * 3 si falla la validación contra el mapping.
 */
class FileIntakeTest {

    private final ElastiflixProperties properties = new ElastiflixProperties(null, null, null, null, null, null, null);
    private final JobStore store = new JobStore(properties, Clock.systemUTC());
    private final FileIntake intake = new FileIntake(
            new FileGate(new MovieReaderFactory(new IndexMapping()), properties),
            mock(CatalogIndex.class), properties, store);

    @TempDir
    private Path folder;

    @Test
    @DisplayName("una doble extensión (EFX-ING-002) detiene el paso 2")
    void shouldStopStepTwoWhenTheFileHasTwoExtensions() throws Exception {
        // given
        Path upload = Files.writeString(folder.resolve("subida.bin"), "id,title,plot\n1,A,p\n");
        String jobId = store.open("peliculas.json.csv", upload, CsvSettings.withHeader());

        // when
        intake.validate(jobId);
        JobView view = store.view(jobId);

        // then
        assertThat(view.status()).isEqualTo("invalido");
        assertThat(view.errors()).extracting(ValidationIssue::code)
                .containsExactly(ErrorCode.DOUBLE_EXTENSION.code());
        assertThat(view.step()).isEqualTo(2);
    }

    @Test
    @DisplayName("una fecha mala (EFX-ING-012) detiene el paso 3")
    void shouldStopStepThreeWhenAFieldHasTheWrongType() throws Exception {
        // given
        Path upload = Files.writeString(folder.resolve("subida.bin"), """
                id,title,plot,release_date
                1,A,p,16/07/1999
                """);
        String jobId = store.open("peliculas.csv", upload, CsvSettings.withHeader());

        // when
        intake.validate(jobId);
        JobView view = store.view(jobId);

        // then
        assertThat(view.status()).isEqualTo("invalido");
        assertThat(view.errors()).extracting(ValidationIssue::code)
                .containsExactly(ErrorCode.TYPE_MISMATCH.code());
        assertThat(view.step()).isEqualTo(3);
    }

    @Test
    @DisplayName("al empezar la validación contra el mapping, el trabajo pasa al paso 3 en curso")
    void shouldMoveToStepThreeWhenTheMappingValidationStarts() throws Exception {
        // given
        String jobId = store.open("peliculas.json", Files.writeString(folder.resolve("a.json"), "[]"),
                CsvSettings.withHeader());
        store.require(jobId).validating();
        assertThat(store.view(jobId).step()).isEqualTo(2);

        // when
        store.fileRead(jobId, 4, List.of());
        JobView view = store.view(jobId);

        // then
        assertThat(view.status()).isEqualTo("validando");
        assertThat(view.step()).isEqualTo(3);
    }
}
