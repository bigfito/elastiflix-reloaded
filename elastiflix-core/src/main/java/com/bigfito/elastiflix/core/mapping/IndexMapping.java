package com.bigfito.elastiflix.core.mapping;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bigfito.elastiflix.core.error.ConfigurationException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lee el mapping del catálogo empaquetado en el jar.
 *
 * <p>El archivo es {@code elasticsearch/elastiflix-reloaded-mapping.json}. Si no está, el arranque
 * se detiene con {@code EFX-CFG-003}.</p>
 */
@Component
public class IndexMapping {

    /** Ruta del mapping dentro del classpath. */
    static final String RESOURCE = "elasticsearch/elastiflix-reloaded-mapping.json";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final String document;
    private final Map<String, FieldMapping> fields;

    /**
     * Carga el mapping del jar.
     */
    public IndexMapping() {
        document = read(packagedStream());
        fields = readFields(document);
    }

    /**
     * JSON del archivo, sin cambios.
     *
     * @return el documento completo
     */
    public String document() {
        return document;
    }

    /**
     * Objeto {@code mappings} listo para crear un índice.
     *
     * @return JSON de {@code mappings}
     */
    public String mappings() {
        return JSON.writeValueAsString(JSON.readTree(document).path("mappings"));
    }

    /**
     * Campos de primer nivel, en el orden del archivo.
     *
     * @return los campos
     */
    public List<FieldMapping> fields() {
        return List.copyOf(fields.values());
    }

    /**
     * Busca un campo de primer nivel.
     *
     * @param name nombre en el índice
     * @return el campo
     */
    public FieldMapping field(String name) {
        FieldMapping found = fields.get(name);
        if (found == null) {
            throw new IllegalArgumentException("El mapping no tiene el campo " + name);
        }
        return found;
    }

    /**
     * Lee el flujo y, si no hay flujo, avisa que el mapping no se empaquetó.
     *
     * @param source flujo del recurso, o {@code null} si el jar no lo trae
     * @return texto del mapping
     */
    static String read(InputStream source) {
        if (source == null) {
            throw new ConfigurationException(ErrorCode.MAPPING_NOT_PACKAGED);
        }
        try (source) {
            return new String(source.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ConfigurationException(ErrorCode.MAPPING_NOT_PACKAGED, exception);
        }
    }

    private static InputStream packagedStream() {
        return IndexMapping.class.getClassLoader().getResourceAsStream(RESOURCE);
    }

    private static Map<String, FieldMapping> readFields(String mapping) {
        try {
            return fieldsOf(JSON.readTree(mapping));
        } catch (JacksonException exception) {
            throw new ConfigurationException(ErrorCode.MAPPING_NOT_PACKAGED, exception);
        }
    }

    private static Map<String, FieldMapping> fieldsOf(JsonNode root) {
        JsonNode properties = root.path("mappings").path("properties");
        Map<String, FieldMapping> parsed = new LinkedHashMap<>();
        for (String name : properties.propertyNames()) {
            parsed.put(name, toField(name, properties.get(name)));
        }
        return Collections.unmodifiableMap(parsed);
    }

    private static FieldMapping toField(String name, JsonNode spec) {
        return new FieldMapping(name, text(spec, "type"), text(spec, "format"),
                integer(spec, "dims"), nestedNames(spec));
    }

    private static String text(JsonNode spec, String property) {
        JsonNode value = spec.get(property);
        if (value == null || !value.isString()) {
            return null;
        }
        return value.asString();
    }

    private static Integer integer(JsonNode spec, String property) {
        JsonNode value = spec.get(property);
        if (value == null || !value.isNumber()) {
            return null;
        }
        return value.intValue();
    }

    private static List<String> nestedNames(JsonNode spec) {
        JsonNode nested = spec.get("properties");
        if (nested == null) {
            return List.of();
        }
        return List.copyOf(new ArrayList<>(nested.propertyNames()));
    }
}
