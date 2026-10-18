package com.bigfito.elastiflix.ingest.csv;

import java.io.StringReader;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.IngestValidationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Un CSV mal formado tiene su propio código, distinto del JSON.
 */
class CsvRowsTest {

    @Test
    @DisplayName("una comilla sin cerrar es EFX-ING-015")
    void shouldRejectAnUnclosedQuoteAsCsv() {
        CsvRows rows = new CsvRows(new StringReader("\"sin cierre"), ',');

        assertThatThrownBy(rows::nextRow)
                .isInstanceOf(IngestValidationException.class)
                .extracting(thrown -> ((IngestValidationException) thrown).errorCode())
                .isEqualTo(ErrorCode.UNREADABLE_CSV);
    }
}
