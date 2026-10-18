package com.bigfito.elastiflix.ingest.json;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.IngestValidationException;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.VectorFields;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Lee los objetos de un JSON sin convertirlos todavía en películas.
 *
 * <p>El validador ve los campos tal como llegaron, salvo los vectores: se quitan de cada registro apenas se
 * lee, y solo queda una advertencia por campo.</p>
 */
public final class JsonDocuments {

    private final JsonMapper mapper = MovieJsonMapper.create();

    /**
     * Lee exportación, arreglo o NDJSON.
     *
     * @param path archivo JSON
     * @return un nodo por película, en el orden del archivo y sin vectores, y las advertencias
     */
    public JsonRead read(Path path) {
        JsonShape shape = JsonShapes.detect(path, mapper);
        Set<String> vectors = new LinkedHashSet<>();
        try (JsonParser parser = mapper.createParser(path)) {
            List<JsonNode> documents = switch (shape) {
                case ARRAY -> readArray(parser, vectors);
                case EXPORT -> readExport(parser, vectors);
                case NDJSON -> readNdjson(parser, vectors);
            };
            return new JsonRead(documents, warnings(vectors));
        } catch (JacksonException exception) {
            throw new IngestValidationException(ErrorCode.UNREADABLE_JSON, exception);
        }
    }

    private List<JsonNode> readArray(JsonParser parser, Set<String> vectors) {
        require(parser.nextToken(), JsonToken.START_ARRAY);
        List<JsonNode> documents = readElements(parser, vectors);
        requireEnd(parser);
        return documents;
    }

    private List<JsonNode> readNdjson(JsonParser parser, Set<String> vectors) {
        List<JsonNode> documents = new ArrayList<>();
        while (parser.nextToken() != null) {
            documents.add(withoutVectors(parser.readValueAsTree(), vectors));
        }
        return documents;
    }

    private List<JsonNode> readExport(JsonParser parser, Set<String> vectors) {
        require(parser.nextToken(), JsonToken.START_OBJECT);
        ExportRead read = new ExportRead(vectors);
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            read.field(parser);
        }
        requireDocuments(read.sawDocuments);
        requireCount(read.declared, read.documents.size());
        requireEnd(parser);
        return read.documents;
    }

    private static List<JsonNode> readElements(JsonParser parser, Set<String> vectors) {
        if (parser.currentToken() != JsonToken.START_ARRAY) {
            throw new IngestValidationException(ErrorCode.UNREADABLE_JSON);
        }
        List<JsonNode> documents = new ArrayList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            documents.add(withoutVectors(parser.readValueAsTree(), vectors));
        }
        return documents;
    }

    /** Quita los vectores de un registro recién leído y anota cuáles venían. */
    private static JsonNode withoutVectors(JsonNode node, Set<String> vectors) {
        if (node instanceof ObjectNode object) {
            for (String name : VectorFields.NAMES) {
                if (object.remove(name) != null) {
                    vectors.add(name);
                }
            }
        }
        return node;
    }

    private static List<ValidationIssue> warnings(Set<String> vectors) {
        List<ValidationIssue> warnings = new ArrayList<>();
        for (String name : vectors) {
            warnings.add(VectorFields.ignored(name));
        }
        return List.copyOf(warnings);
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

    private static void requireEnd(JsonParser parser) {
        if (parser.nextToken() != null) {
            throw new IngestValidationException(ErrorCode.UNREADABLE_JSON);
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
        private final Set<String> vectors;
        private Long declared;
        private boolean sawDocuments;

        private ExportRead(Set<String> vectors) {
            this.vectors = vectors;
        }

        private void field(JsonParser parser) {
            String name = parser.currentName();
            parser.nextToken();
            if (JsonFields.DOCUMENTS.equals(name)) {
                sawDocuments = true;
                documents.addAll(readElements(parser, vectors));
            } else if (JsonFields.DOCUMENT_COUNT.equals(name)) {
                declared = count(parser);
            } else {
                parser.skipChildren();
            }
        }
    }
}
