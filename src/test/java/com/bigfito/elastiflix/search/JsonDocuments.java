package com.bigfito.elastiflix.search;

import java.io.StringWriter;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.json.JsonpSerializable;

import jakarta.json.stream.JsonGenerator;

/**
 * Serializa una petición del cliente para compararla en las pruebas.
 */
public final class JsonDocuments {

    private static final JsonpMapper MAPPER = new Jackson3JsonpMapper();

    private JsonDocuments() {
    }

    /**
     * JSON compacto, tal como lo escribe el cliente.
     *
     * @param value petición o cláusula
     * @return el JSON
     */
    public static String of(JsonpSerializable value) {
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = MAPPER.jsonProvider().createGenerator(writer)) {
            value.serialize(generator, MAPPER);
        }
        return writer.toString();
    }
}
