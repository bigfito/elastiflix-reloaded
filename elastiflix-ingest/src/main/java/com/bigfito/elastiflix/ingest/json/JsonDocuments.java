package com.bigfito.elastiflix.ingest.json;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.IngestValidationException;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lee los objetos de un JSON sin convertirlos todavía en películas.
 *
 * <p>Así el validador ve los campos tal como llegaron, vectores incluidos.</p>
 */
public final class JsonDocuments {

    private final JsonMapper mapper = MovieJsonMapper.create();

    /**
     * Lee exportación, arreglo o NDJSON.
     *
     * @param path archivo JSON
     * @return un nodo por película, en el orden del archivo
     */
    public List<JsonNode> read(Path path) {
        JsonShape shape = JsonShapes.detect(path, mapper);
        try (JsonParser parser = mapper.createParser(path)) {
            return switch (shape) {
                case ARRAY -> readArray(parser);
                case EXPORT -> readExport(parser);
                case NDJSON -> readNdjson(parser);
            };
        } catch (JacksonException exception) {
            throw new IngestValidationException(ErrorCode.UNREADABLE_JSON, exception);
        }
    }

    private List<JsonNode> readArray(JsonParser parser) {
        require(parser.nextToken(), JsonToken.START_ARRAY);
        return readElements(parser);
    }

    private List<JsonNode> readNdjson(JsonParser parser) {
        List<JsonNode> documents = new ArrayList<>();
        while (parser.nextToken() != null) {
            documents.add(parser.readValueAsTree());
        }
        return documents;
    }

    private List<JsonNode> readExport(JsonParser parser) {
        require(parser.nextToken(), JsonToken.START_OBJECT);
        ExportRead read = new ExportRead();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            read.field(parser);
        }
        requireDocuments(read.sawDocuments);
        requireCount(read.declared, read.documents.size());
        return read.documents;
    }

    private static List<JsonNode> readElements(JsonParser parser) {
        if (parser.currentToken() != JsonToken.START_ARRAY) {
            throw new IngestValidationException(ErrorCode.UNREADABLE_JSON);
        }
        List<JsonNode> documents = new ArrayList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            documents.add(parser.readValueAsTree());
        }
        return documents;
    }

    private static Long count(JsonParser parser) {
        if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
            throw new IngestValidationException(ErrorCode.DOCUMENT_COUNT_MISMATCH);
        }
        return parser.getLongValue();
    }

    private static void requireDocuments(boolean sawDocuments) {
        if (!sawDocuments) {
            throw new IngestValidationException(ErrorCode.UNREADABLE_JSON);
        }
    }

    private static void requireCount(Long declared, int actual) {
        if (declared != null && declared != actual) {
            throw new IngestValidationException(ErrorCode.DOCUMENT_COUNT_MISMATCH);
        }
    }

    private static void require(JsonToken token, JsonToken expected) {
        if (token != expected) {
            throw new IngestValidationException(ErrorCode.UNREADABLE_JSON);
        }
    }

    /**
     * Acumula el arreglo {@code documents} y el {@code document_count} opcional.
     */
    private final class ExportRead {

        private final List<JsonNode> documents = new ArrayList<>();
        private Long declared;
        private boolean sawDocuments;

        private void field(JsonParser parser) {
            String name = parser.currentName();
            parser.nextToken();
            if (JsonFields.DOCUMENTS.equals(name)) {
                sawDocuments = true;
                documents.addAll(readElements(parser));
            } else if (JsonFields.DOCUMENT_COUNT.equals(name)) {
                declared = count(parser);
            } else {
                parser.skipChildren();
            }
        }
    }
}
