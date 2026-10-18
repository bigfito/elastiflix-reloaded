package com.bigfito.elastiflix.core.error;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Un 4xx no se confunde con la falta de conexión.
 */
class ElasticsearchDiagnosticsTest {

    @Test
    @DisplayName("413 es EFX-ES-007 y un corte de red es EFX-ES-001")
    void shouldSeparateAClientErrorFromANetworkFailure() {
        assertThat(ElasticsearchDiagnostics.codeFor(413)).isEqualTo(ErrorCode.ELASTICSEARCH_REJECTED);
        assertThat(ElasticsearchDiagnostics.codeFor(0)).isEqualTo(ErrorCode.ELASTICSEARCH_UNAVAILABLE);
    }

    @Test
    @DisplayName("401 y 403 son una URL o API key no válidas: EFX-ES-001, como en el pie")
    void shouldTreatBadCredentialsAsUnavailable() {
        assertThat(ElasticsearchDiagnostics.codeFor(401)).isEqualTo(ErrorCode.ELASTICSEARCH_UNAVAILABLE);
        assertThat(ElasticsearchDiagnostics.codeFor(403)).isEqualTo(ErrorCode.ELASTICSEARCH_UNAVAILABLE);
        assertThat(ElasticsearchDiagnostics.codeFor(500)).isEqualTo(ErrorCode.ELASTICSEARCH_UNAVAILABLE);
        assertThat(ElasticsearchDiagnostics.codeFor(400)).isEqualTo(ErrorCode.ELASTICSEARCH_REJECTED);
        assertThat(ElasticsearchDiagnostics.typeOf(new IOException("red"))).isEqualTo("IOException");
    }
}
