package com.bigfito.elastiflix.core.error;

import java.util.HashSet;
import java.util.Map;
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
    @DisplayName("los textos de limpieza dicen «se borra si…; si quedó, bórralo a mano», sin prometer un borrado")
    void shouldDescribeTheCleanupWithoutPromisingIt() {
        // given
        Map<ErrorCode, String> expected = Map.of(
                ErrorCode.REINDEX_COUNT_MISMATCH, "EFX-ES-003 · El índice nuevo no tiene los mismos documentos que "
                        + "el original. Causa probable: El reindex no copió todos los documentos. Acción sugerida: No "
                        + "se publicó el alias y el catálogo original sigue igual. El índice nuevo se borra si el "
                        + "original sigue en su lugar; si quedó, bórralo a mano. Vuelve a ejecutar la importación o "
                        + "la migración.",
                ErrorCode.REINDEX_FAILED, "EFX-ES-006 · No se pudo copiar el catálogo al índice nuevo. Causa "
                        + "probable: La tarea de reindex terminó con error o no se completó. Acción sugerida: No se "
                        + "publicó el alias y el catálogo original sigue igual. Se pide cancelar la tarea y el índice "
                        + "nuevo se borra si el original sigue en su lugar; si quedó, bórralo a mano. Vuelve a "
                        + "ejecutar la importación o la migración.",
                ErrorCode.ALIAS_PUBLISH_FAILED, "EFX-ES-004 · No se pudo publicar el alias. Causa probable: "
                        + "Elasticsearch rechazó la publicación y el alias sigue en el índice anterior. Acción "
                        + "sugerida: El catálogo anterior sigue igual. El índice nuevo se borra; si quedó, bórralo a "
                        + "mano. Revisa el catálogo y vuelve a intentarlo.",
                ErrorCode.BULK_REJECTED, "EFX-IMP-001 · Elasticsearch rechazó un documento del lote. Causa "
                        + "probable: Un ítem del lote trae error. Acción sugerida: El catálogo anterior sigue igual. "
                        + "El índice temporal se borra; si quedó, bórralo a mano. Revisa el archivo y vuelve a "
                        + "importar.",
                ErrorCode.IMPORT_COUNT_MISMATCH, "EFX-IMP-002 · El conteo del índice nuevo no es el esperado. Causa "
                        + "probable: Después de los lotes, el conteo no cuadra. Acción sugerida: No se publicó. El "
                        + "índice temporal se borra; si quedó, bórralo a mano. Vuelve a importar.",
                ErrorCode.IMPORT_CANCELLED, "EFX-IMP-003 · La importación se canceló. Causa probable: Alguien pidió "
                        + "cancelar. Acción sugerida: El catálogo anterior sigue igual. El índice temporal, si "
                        + "existía, se borra; si quedó, bórralo a mano.",
                ErrorCode.PUBLISH_UNCERTAIN, "EFX-IMP-006 · No se sabe si el alias se publicó. Causa probable: "
                        + "_aliases no confirmó el cambio y el alias no se pudo leer o no quedó en un estado que "
                        + "permita borrar el índice nuevo. Acción sugerida: Estado incierto: revisa el alias "
                        + "manualmente. El índice nuevo se dejó sin borrar.",
                ErrorCode.UNREADABLE_JSON, "EFX-ING-013 · El JSON no se puede leer. Causa probable: El archivo JSON "
                        + "está mal formado. Acción sugerida: Revisa el JSON y vuelve a subir el archivo.");

        // when / then
        expected.forEach((code, message) -> assertThat(code.userMessage()).as(code.code()).isEqualTo(message));
        for (ErrorCode errorCode : ErrorCode.values()) {
            assertThat(errorCode.userMessage())
                    .doesNotContain("Se borró el índice temporal")
                    .doesNotContain("lo limpia la próxima importación");
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
