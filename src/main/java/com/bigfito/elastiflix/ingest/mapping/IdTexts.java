package com.bigfito.elastiflix.ingest.mapping;

import tools.jackson.databind.JsonNode;

/**
 * Lee un {@code id} que puede llegar como número o como texto.
 *
 * <p>La búsqueda de ids ya existentes y el validador usan este mismo texto. No se recorta:
 * un espacio forma parte del id.</p>
 */
public final class IdTexts {

    private IdTexts() {
    }

    /**
     * Texto del id. Un número entero se escribe sin decimales.
     *
     * @param value nodo del id, o {@code null}
     * @return el texto, o {@code null} si no hay un id usable
     */
    public static String of(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        return text(value);
    }

    private static String text(JsonNode value) {
        if (value.isIntegralNumber()) {
            // bigIntegerValue: un id de 25 dígitos no cabe en un long, pero como keyword es un texto válido.
            return value.bigIntegerValue().toString();
        }
        if (value.isString() && !value.asString().isBlank()) {
            return value.asString();
        }
        return null;
    }
}
