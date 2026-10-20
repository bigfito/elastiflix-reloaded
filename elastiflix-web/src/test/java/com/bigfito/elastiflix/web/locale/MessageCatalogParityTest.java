package com.bigfito.elastiflix.web.locale;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import com.bigfito.elastiflix.core.error.ErrorCode;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida la paridad completa entre los catálogos de mensajes en español e inglés.
 */
class MessageCatalogParityTest {

    private static Properties esProps;
    private static Properties enProps;

    @BeforeAll
    static void loadProperties() throws IOException {
        esProps = load("/messages_es.properties");
        enProps = load("/messages_en.properties");
    }

    private static Properties load(String path) throws IOException {
        Properties props = new Properties();
        try (InputStream in = MessageCatalogParityTest.class.getResourceAsStream(path)) {
            assertThat(in).as("El archivo %s debe existir en el classpath", path).isNotNull();
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return props;
    }

    @Test
    @DisplayName("ambos catálogos tienen exactamente el mismo conjunto de claves")
    void shouldHaveIdenticalKeySets() {
        Set<String> esKeys = new TreeSet<>(esProps.stringPropertyNames());
        Set<String> enKeys = new TreeSet<>(enProps.stringPropertyNames());

        Set<String> onlyInEs = new TreeSet<>(esKeys);
        onlyInEs.removeAll(enKeys);

        Set<String> onlyInEn = new TreeSet<>(enKeys);
        onlyInEn.removeAll(esKeys);

        assertThat(onlyInEs).as("Claves presentes solo en messages_es.properties").isEmpty();
        assertThat(onlyInEn).as("Claves presentes solo en messages_en.properties").isEmpty();
        assertThat(esKeys).isEqualTo(enKeys);
    }

    @Test
    @DisplayName("ningún valor de los catálogos está vacío o en blanco")
    void shouldNotContainBlankValues() {
        for (String key : esProps.stringPropertyNames()) {
            String value = esProps.getProperty(key);
            assertThat(value).as("Valor para la clave %s en messages_es.properties", key)
                    .isNotNull()
                    .isNotBlank();
        }

        for (String key : enProps.stringPropertyNames()) {
            String value = enProps.getProperty(key);
            assertThat(value).as("Valor para la clave %s en messages_en.properties", key)
                    .isNotNull()
                    .isNotBlank();
        }
    }

    @Test
    @DisplayName("todos los códigos de error tienen summary, cause y action definidos en ambos idiomas")
    void shouldDefineAllErrorCodes() {
        for (ErrorCode code : ErrorCode.values()) {
            String prefix = "error." + code.code();
            String summaryKey = prefix + ".summary";
            String causeKey = prefix + ".cause";
            String actionKey = prefix + ".action";

            assertThat(esProps.getProperty(summaryKey))
                    .as("messages_es.properties debe tener %s", summaryKey)
                    .isNotBlank();
            assertThat(esProps.getProperty(causeKey))
                    .as("messages_es.properties debe tener %s", causeKey)
                    .isNotBlank();
            assertThat(esProps.getProperty(actionKey))
                    .as("messages_es.properties debe tener %s", actionKey)
                    .isNotBlank();

            assertThat(enProps.getProperty(summaryKey))
                    .as("messages_en.properties debe tener %s", summaryKey)
                    .isNotBlank();
            assertThat(enProps.getProperty(causeKey))
                    .as("messages_en.properties debe tener %s", causeKey)
                    .isNotBlank();
            assertThat(enProps.getProperty(actionKey))
                    .as("messages_en.properties debe tener %s", actionKey)
                    .isNotBlank();
        }
    }
}
