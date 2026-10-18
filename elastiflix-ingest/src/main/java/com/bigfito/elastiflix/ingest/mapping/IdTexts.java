package com.bigfito.elastiflix.ingest.mapping;

import tools.jackson.databind.JsonNode;

/**
 * Lee un {@code id} que puede llegar como número o como texto.
 */
final class IdTexts {

    private IdTexts() {
    }

    static String of(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        return text(value);
    }

    private static String text(JsonNode value) {
        if (value.isIntegralNumber()) {
            return Long.toString(value.longValue());
        }
        if (value.isString() && !value.asString().isBlank()) {
            return value.asString();
        }
        return null;
    }
}
