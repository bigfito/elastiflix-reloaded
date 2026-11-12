package com.bigfito.elastiflix.ingest.mapping;

import tools.jackson.databind.JsonNode;

/**
 * Texto corto de un valor, para la columna «recibido» del informe.
 */
final class IssueTexts {

    private static final int LIMIT = 80;

    private IssueTexts() {
    }

    static String show(JsonNode value) {
        return clip(value.toString());
    }

    static String clip(String text) {
        if (text.length() <= LIMIT) {
            return text;
        }
        return text.substring(0, LIMIT);
    }
}
