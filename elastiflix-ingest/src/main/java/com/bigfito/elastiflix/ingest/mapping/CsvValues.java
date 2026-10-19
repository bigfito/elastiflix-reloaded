package com.bigfito.elastiflix.ingest.mapping;

import java.math.BigDecimal;
import java.math.BigInteger;

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
     * @return valor JSON; nunca {@code null}
     */
    public static JsonNode read(String column, String cell) {
        // Sin trim: el espacio es parte del valor (un id " 597" no es "597"). Quien llama ya descartó las
        // celdas en blanco.
        FieldKind kind = FieldRules.kind(column);
        if (kind == null) {
            // _id no es un campo del mapping: llega como texto.
            return MAPPER.valueToTree(cell);
        }
        return switch (kind) {
            case BOOLEAN -> bool(cell);
            case INTEGER -> integer(cell);
            case DECIMAL -> number(cell);
            default -> textOrStructure(kind, cell);
        };
    }

    /**
     * Dice si la columna es una lista de textos ({@code genres}, {@code cast}…), que en la celda va como JSON.
     *
     * @param column nombre de la columna
     * @return {@code true} para las listas de textos del mapping
     */
    public static boolean isTextList(String column) {
        return FieldRules.kind(column) == FieldKind.TEXT_LIST;
    }

    private static JsonNode textOrStructure(FieldKind kind, String text) {
        if (structured(kind)) {
            return jsonOrText(text);
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

    /** Solo dígitos: {@code 1e3} y {@code 12.0} quedan como texto y el validador los rechaza. */
    private static JsonNode integer(String text) {
        try {
            return MAPPER.getNodeFactory().numberNode(new BigInteger(text));
        } catch (NumberFormatException exception) {
            return MAPPER.valueToTree(text);
        }
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
