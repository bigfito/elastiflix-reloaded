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
import com.bigfito.elastiflix.ingest.mapping.MappingValidator;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.service.MovieReaderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;

/**
 * El informe de un CSV nombra la fila real del archivo, por el mismo camino que una subida.
 */
class CsvRowNumbersTest {

    private final ElastiflixProperties properties = new ElastiflixProperties(null, null, null, null, null, null, null);
    private final JobStore store = new JobStore(properties, Clock.systemUTC());
    private final FileIntake intake = new FileIntake(
            new FileGate(new MovieReaderFactory(new IndexMapping()), properties),
            new MappingValidator(), mock(CatalogIndex.class), properties, store);

    @TempDir
    private Path folder;

    private String storeId;

    @Test
    @DisplayName("la fecha mala de la línea 2 sale como «Fila 2», no como la fila del encabezado")
    void shouldNameTheRealRow() throws Exception {
        // given
        Path file = Files.copy(Path.of("src", "test", "resources", "validation", "csv-bad-date.csv"),
                folder.resolve("csv-bad-date.csv"));

        // when
        List<ValidationIssue> errors = validate(file);

        // then
        assertThat(errors).extracting(ValidationIssue::recordLabel, ValidationIssue::field)
                .containsExactly(tuple("2", "release_date"));
    }

    @Test
    @DisplayName("una fila corta más 2 fechas malas dan 3 errores en orden de fila, con las filas en blanco contadas")
    void shouldReportWidthAndFieldErrorsTogether() throws Exception {
        // given
        Path file = Files.writeString(folder.resolve("mezcla.csv"), """
                id,title,plot,release_date
                1,A,p,16/07/1999

                2,B
                3,C,p,1999-99-99
                """);

        // when
        List<ValidationIssue> errors = validate(file);

        // then
        assertThat(errors).extracting(ValidationIssue::recordLabel, ValidationIssue::code)
                .containsExactly(
                        tuple("2", ErrorCode.TYPE_MISMATCH.code()),
                        tuple("4", ErrorCode.COLUMN_COUNT.code()),
                        tuple("5", ErrorCode.TYPE_MISMATCH.code()));
        assertThat(store.view(storeId).fileCount()).isEqualTo(3);
    }

    private List<ValidationIssue> validate(Path file) throws Exception {
        Path upload = Files.copy(file, folder.resolve("subida-" + file.getFileName()));
        storeId = store.open(file.getFileName().toString(), upload, CsvSettings.withHeader());
        intake.validate(storeId);
        assertThat(store.view(storeId).status()).isEqualTo("invalido");
        return store.require(storeId).kept().report().errors();
    }
}
