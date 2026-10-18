package com.bigfito.elastiflix.core.error;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba el formato de los mensajes y que no haya dos constantes con el mismo código.
 */
class ErrorCodeTest {

    private static final Pattern USER_MESSAGE = Pattern.compile(
            "^EFX-[A-Z]+-\\d{3} · .+\\. Causa probable: .+\\. Acción sugerida: .+\\.$");

    @Test
    @DisplayName("cada código es único y el mensaje sigue el formato del proyecto")
    void shouldFormatEveryCodeWhenCatalogIsRead() {
        Set<String> codes = new HashSet<>();

        for (ErrorCode errorCode : ErrorCode.values()) {
            assertThat(codes.add(errorCode.code()))
                    .as("código duplicado: %s", errorCode.code())
                    .isTrue();
            assertThat(errorCode.userMessage()).matches(USER_MESSAGE);
        }
    }

    @Test
    @DisplayName("la URL ausente pide revisar ELASTICSEARCH_URL")
    void shouldNameTheUrlVariableWhenUrlIsMissing() {
        String message = ErrorCode.MISSING_ELASTICSEARCH_URL.userMessage();

        assertThat(message).startsWith("EFX-CFG-001 · ");
        assertThat(message).contains("Revisa ELASTICSEARCH_URL");
    }

    @Test
    @DisplayName("una excepción de configuración conserva el código y la causa")
    void shouldKeepTheCauseWhenConfigurationFails() {
        // given
        IllegalStateException cause = new IllegalStateException("origen");

        // when
        ConfigurationException thrown = new ConfigurationException(ErrorCode.MISSING_API_KEY, cause);

        // then
        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.MISSING_API_KEY);
        assertThat(thrown).hasMessage(ErrorCode.MISSING_API_KEY.userMessage());
        assertThat(thrown).hasCause(cause);
    }
}
