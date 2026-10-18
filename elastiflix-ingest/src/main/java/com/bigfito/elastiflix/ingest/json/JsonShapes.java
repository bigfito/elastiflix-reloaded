package com.bigfito.elastiflix.ingest.json;

import java.nio.file.Path;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.IngestValidationException;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.json.JsonMapper;

/**
 * Distingue arreglo, exportación y NDJSON sin cargar las películas.
 *
 * <p>Si el objeto tiene {@code documents}, se detiene en ese nombre y no recorre la lista.</p>
 */
final class JsonShapes {

    private JsonShapes() {
    }

    /**
     * Mira el primer valor del archivo.
     *
     * @param path   archivo ya comprobado
     * @param mapper lector de JSON
     * @return la forma
     */
    static JsonShape detect(Path path, JsonMapper mapper) {
        try (JsonParser parser = mapper.createParser(path)) {
            JsonToken first = parser.nextToken();
            if (first == JsonToken.START_ARRAY) {
                return JsonShape.ARRAY;
            }
            if (first == JsonToken.START_OBJECT) {
                return objectShape(parser);
            }
            throw new IngestValidationException(ErrorCode.UNREADABLE_JSON);
        } catch (JacksonException exception) {
            throw new IngestValidationException(ErrorCode.UNREADABLE_JSON, exception);
        }
    }

    private static JsonShape objectShape(JsonParser parser) {
        int depth = 1;
        while (depth > 0) {
            JsonToken token = next(parser);
            if (isDocumentsField(token, depth, parser)) {
                return JsonShape.EXPORT;
            }
            depth = step(depth, token);
        }
        return JsonShape.NDJSON;
    }

    private static boolean isDocumentsField(JsonToken token, int depth, JsonParser parser) {
        return token == JsonToken.PROPERTY_NAME
                && depth == 1
                && JsonFields.DOCUMENTS.equals(parser.currentName());
    }

    private static int step(int depth, JsonToken token) {
        if (token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY) {
            return depth + 1;
        }
        if (token == JsonToken.END_OBJECT || token == JsonToken.END_ARRAY) {
            return depth - 1;
        }
        return depth;
    }

    private static JsonToken next(JsonParser parser) {
        JsonToken token = parser.nextToken();
        if (token == null) {
            throw new IngestValidationException(ErrorCode.UNREADABLE_JSON);
        }
        return token;
    }
}
