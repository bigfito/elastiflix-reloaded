package com.bigfito.elastiflix.ingest.csv;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.IngestValidationException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Convierte el texto de una celda en un valor JSON.
 */
final class CsvCells {

    private static final JsonMapper MAPPER = MovieJsonMapper.create();
    private static final char DECIMAL_POINT = '.';

    private CsvCells() {
    }

    /**
     * Texto, número, booleano o JSON de una lista u objeto.
     *
     * @param cell contenido de la celda, sin las comillas del CSV
     * @return nodo listo para armar la película
     */
    static JsonNode toNode(String cell) {
        String text = cell.trim();
        if (startsJson(text)) {
            return readJson(text);
        }
        JsonNode value = scalar(text);
        if (value == null) {
            return MAPPER.valueToTree(text);
        }
        return value;
    }

    private static JsonNode scalar(String text) {
        if ("true".equals(text) || "false".equals(text)) {
            return MAPPER.valueToTree(Boolean.parseBoolean(text));
        }
        return number(text);
    }

    private static JsonNode number(String text) {
        if (isInteger(text)) {
            return MAPPER.valueToTree(Long.parseLong(text));
        }
        if (isDecimal(text)) {
            return MAPPER.valueToTree(Double.parseDouble(text));
        }
        return null;
    }

    static JsonMapper mapper() {
        return MAPPER;
    }

    private static boolean startsJson(String text) {
        return text.startsWith("[") || text.startsWith("{");
    }

    private static JsonNode readJson(String text) {
        try {
            return MAPPER.readTree(text);
        } catch (JacksonException exception) {
            throw new IngestValidationException(ErrorCode.UNREADABLE_JSON, exception);
        }
    }

    private static boolean isInteger(String text) {
        return digits(text, false);
    }

    private static boolean isDecimal(String text) {
        return digits(text, true);
    }

    private static boolean digits(String text, boolean decimal) {
        int start = text.startsWith("-") ? 1 : 0;
        boolean point = false;
        boolean digit = false;
        for (int index = start; index < text.length(); index++) {
            char ch = text.charAt(index);
            if (ch == DECIMAL_POINT && decimal && !point) {
                point = true;
            } else if (Character.isDigit(ch)) {
                digit = true;
            } else {
                return false;
            }
        }
        return digit && (!decimal || point);
    }
}
