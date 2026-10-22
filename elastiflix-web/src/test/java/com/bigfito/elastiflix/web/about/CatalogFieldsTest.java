package com.bigfito.elastiflix.web.about;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * «El catálogo» describe exactamente las propiedades del mapping, con su tipo, y todos sus textos existen.
 */
class CatalogFieldsTest {

    private static final Path MAPPING = Path.of("..", "elasticsearch", "elastiflix-reloaded-mapping.json");

    @Test
    @DisplayName("cada propiedad y subpropiedad del mapping tiene una fila con su tipo, y no sobra ninguna")
    void shouldDescribeEveryMappingPropertyWithItsType() throws IOException {
        Map<String, String> rows = new LinkedHashMap<>();
        for (CatalogGroup group : CatalogFields.GROUPS) {
            for (CatalogField field : group.fields()) {
                rows.put(field.name(), field.type());
            }
        }

        assertThat(rows).containsExactlyInAnyOrderEntriesOf(mappingTypes());
    }

    @Test
    @DisplayName("los subcampos van justo después de su objeto anidado")
    void shouldPlaceEverySubfieldUnderItsParent() {
        for (CatalogGroup group : CatalogFields.GROUPS) {
            String parent = "";
            for (CatalogField field : group.fields()) {
                if (field.subfield()) {
                    assertThat(field.name()).startsWith(parent + ".");
                } else {
                    parent = field.name();
                }
            }
        }
    }

    @ParameterizedTest(name = "messages_{0}")
    @ValueSource(strings = {"es", "en"})
    @DisplayName("el título de cada grupo y los textos de cada celda existen en los dos idiomas")
    void shouldHaveEveryTextInBothLanguages(String lang) throws IOException {
        Properties messages = new Properties();
        try (Reader reader = Files.newBufferedReader(
                Path.of("src", "main", "resources", "messages_" + lang + ".properties"), StandardCharsets.UTF_8)) {
            messages.load(reader);
        }
        for (CatalogGroup group : CatalogFields.GROUPS) {
            assertThat(messages).containsKey(group.titleKey());
            for (CatalogField field : group.fields()) {
                assertThat(messages).containsKey(field.contentKey()).containsKey(field.useKey())
                        .containsKey(field.jsonKey()).containsKey(field.csvKey());
            }
        }
    }

    private static Map<String, String> mappingTypes() throws IOException {
        JsonNode properties = JsonMapper.builder().build()
                .readTree(Files.readString(MAPPING)).path("mappings").path("properties");
        Map<String, String> types = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> property : properties.properties()) {
            types.put(property.getKey(), property.getValue().path("type").asString());
            for (Map.Entry<String, JsonNode> sub : property.getValue().path("properties").properties()) {
                types.put(property.getKey() + "." + sub.getKey(), sub.getValue().path("type").asString());
            }
        }
        return types;
    }
}
