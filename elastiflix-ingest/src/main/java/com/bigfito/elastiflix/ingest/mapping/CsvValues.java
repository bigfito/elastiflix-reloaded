package com.bigfito.elastiflix.ingest.mapping;

import java.math.BigDecimal;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Convierte una celda de CSV en el valor que el validador debe ver.
 *
 * <p>El texto y el keyword se quedan como texto, aunque parezcan un número o un JSON. Las listas y
 * los objetos se leen como JSON; si no se pueden leer, queda el texto para que el informe señale
 * la fila y el campo.</p>
 */
public final class CsvValues {

    private static final JsonMapper MAPPER = MovieJsonMapper.create();

    private CsvValues() {
    }

    /**
     * Objeto vacío para ir poniendo las celdas de una fila.
     *
     * @return nodo de película
     */
    public static ObjectNode newObject() {
        return MAPPER.createObjectNode();
    }

    /**
     * Convierte la celda según la forma del campo en el mapping.
     *
     * @param column nombre de la columna
     * @param cell   texto de la celda, sin las comillas del CSV
     * @return valor JSON, o {@code null} si la columna es un vector y no se guarda
     */
    public static JsonNode read(String column, String cell) {
        FieldKind kind = FieldRules.kind(column);
        String text = cell.trim();
        if (kind == FieldKind.BOOLEAN) {
            return bool(text);
        }
        if (kind == FieldKind.INTEGER || kind == FieldKind.DECIMAL) {
            return number(text);
        }
        return textOrStructure(kind, text);
    }

    private static JsonNode textOrStructure(FieldKind kind, String text) {
        if (structured(kind)) {
            return jsonOrText(text);
        }
        if (kind == FieldKind.VECTOR) {
            return null;
        }
        return MAPPER.valueToTree(text);
    }

    private static boolean structured(FieldKind kind) {
        return kind == FieldKind.TEXT_LIST || kind == FieldKind.COLLECTION || kind == FieldKind.KEYWORDS;
    }

    private static JsonNode bool(String text) {
        if ("true".equals(text) || "false".equals(text)) {
            return MAPPER.valueToTree(Boolean.parseBoolean(text));
        }
        return MAPPER.valueToTree(text);
    }

    private static JsonNode number(String text) {
        try {
            return MAPPER.getNodeFactory().numberNode(new BigDecimal(text));
        } catch (NumberFormatException exception) {
            return MAPPER.valueToTree(text);
        }
    }

    private static JsonNode jsonOrText(String text) {
        try {
            return MAPPER.readTree(text);
        } catch (JacksonException exception) {
            return MAPPER.valueToTree(text);
        }
    }
}
