package com.bigfito.elastiflix.core.config;

import com.bigfito.elastiflix.core.error.ConfigurationException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Una falla de Elastiflix cierra el proceso con código 1.
 */
class ElastiflixExitCodeMapperTest {

    private final ElastiflixExitCodeMapper mapper = new ElastiflixExitCodeMapper();

    @Test
    @DisplayName("la excepción, o una causa suya, vale 1")
    void shouldReturnOneWhenTheFailureIsOurs() {
        ConfigurationException failure = new ConfigurationException(ErrorCode.CATALOG_NOT_FOUND);

        assertThat(mapper.getExitCode(failure)).isEqualTo(1);
        assertThat(mapper.getExitCode(new IllegalStateException(failure))).isEqualTo(1);
        assertThat(mapper.getExitCode(new IllegalStateException("ajena"))).isZero();
    }
}
