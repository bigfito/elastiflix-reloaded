package com.bigfito.elastiflix.ingest.job;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Solo {@code bulk} reemplaza el catálogo; cualquier otro valor es append, el modo que no borra nada.
 */
class ImportModeTest {

    @ParameterizedTest(name = "«{0}» es bulk")
    @ValueSource(strings = {"bulk", "BULK", " Bulk "})
    @DisplayName("bulk, sin importar mayúsculas ni espacios, elige bulk")
    void shouldReadBulk(String value) {
        assertThat(ImportMode.from(value)).isEqualTo(ImportMode.BULK);
    }

    @ParameterizedTest(name = "«{0}» es append")
    @NullAndEmptySource
    @ValueSource(strings = {"append", "APPEND", "bulkk", "reemplazar", "1"})
    @DisplayName("append, vacío, ausente u otro valor eligen append")
    void shouldFallBackToAppend(String value) {
        assertThat(ImportMode.from(value)).isEqualTo(ImportMode.APPEND);
    }

    @Test
    @DisplayName("el nombre en el contrato es append o bulk")
    void shouldUseTheContractNames() {
        assertThat(ImportMode.APPEND.label()).isEqualTo("append");
        assertThat(ImportMode.BULK.label()).isEqualTo("bulk");
    }
}
