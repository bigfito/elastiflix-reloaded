package com.bigfito.elastiflix.ingest.job;

import java.nio.file.Path;
import java.time.Clock;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.IngestValidationException;
import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.movie.MovieDocuments;
import com.bigfito.elastiflix.ingest.service.MovieReaderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.node.JsonNodeFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cada falla de lectura lleva el código de su contexto: CSV o JSON, importación o migración.
 */
class ErrorContextTest {

    private final ElastiflixProperties properties = new ElastiflixProperties(null, null, null, null, null, null, null);

    @TempDir
    private Path folder;

    @ParameterizedTest(name = "{0} ilegible → {1}")
    @CsvSource({"peliculas.csv, EFX-ING-015", "peliculas.json, EFX-ING-013"})
    @DisplayName("un archivo que no se puede abrir da el código de su tipo")
    void shouldNameTheFileKindWhenTheFileCannotBeRead(String name, String code) {
        JobStore store = new JobStore(properties, Clock.systemUTC());
        String jobId = store.open(name, folder.resolve("no-existe.bin"), CsvSettings.withHeader());
        FileGate gate = new FileGate(new MovieReaderFactory(new IndexMapping()));

        assertThatThrownBy(() -> gate.open(store.require(jobId)))
                .isInstanceOf(IngestValidationException.class)
                .extracting(thrown -> ((IngestValidationException) thrown).errorCode().code())
                .isEqualTo(code);
    }

    @Test
    @DisplayName("un valor que no encaja en el modelo es EFX-ING-012, no «El JSON no se puede leer»")
    void shouldUseTheTypeCodeWhenAValueDoesNotFitTheModel() {
        assertThatThrownBy(() -> MovieDocuments.fromNode(JsonNodeFactory.instance.textNode("x")))
                .isInstanceOf(IngestValidationException.class)
                .extracting(thrown -> ((IngestValidationException) thrown).errorCode())
                .isEqualTo(ErrorCode.TYPE_MISMATCH);
    }

    @Test
    @DisplayName("los textos de copia y publicación sirven igual para la importación y para la migración")
    void shouldUseNeutralTextsForCopyAndPublish() {
        assertThat(ErrorCode.REINDEX_COUNT_MISMATCH.userMessage()).contains("importación o la migración");
        assertThat(ErrorCode.REINDEX_FAILED.userMessage()).contains("importación o la migración")
                .doesNotContain("El índice temporal se borra");
        assertThat(ErrorCode.ALIAS_PUBLISH_FAILED.userMessage()).doesNotContain("Se borró el índice temporal")
                .doesNotContain("lo limpia la próxima importación").contains("si quedó, bórralo a mano");
        assertThat(ErrorCode.PUBLISH_UNCERTAIN.userMessage()).doesNotContain("La respuesta no llegó")
                .doesNotContain("índice temporal").contains("El índice nuevo se dejó sin borrar");
        assertThat(ErrorCode.UNREADABLE_JSON.userMessage()).doesNotContain("celda");
    }
}
