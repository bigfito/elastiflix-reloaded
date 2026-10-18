package com.bigfito.elastiflix.ingest.json;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.IngestValidationException;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.movie.MovieDocuments;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lee películas de un JSON en streaming: exportación, arreglo o NDJSON.
 *
 * <p>No guarda el archivo entero. Cada película se construye y se suelta. Si el objeto de
 * exportación trae {@code document_count}, tiene que coincidir con {@code documents}.</p>
 */
public final class JsonMovieReader {

    private final JsonMapper mapper = MovieJsonMapper.create();

    /**
     * Lee todas las películas del archivo. Los vectores que vengan se descartan.
     *
     * @param path archivo JSON
     * @return películas, en el orden del archivo
     */
    public List<Movie> read(Path path) {
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

    private List<Movie> readArray(JsonParser parser) {
        require(parser.nextToken(), JsonToken.START_ARRAY);
        return readElements(parser);
    }

    private List<Movie> readNdjson(JsonParser parser) {
        List<Movie> movies = new ArrayList<>();
        while (parser.nextToken() != null) {
            movies.add(MovieDocuments.read(parser));
        }
        return movies;
    }

    private List<Movie> readExport(JsonParser parser) {
        require(parser.nextToken(), JsonToken.START_OBJECT);
        List<Movie> movies = new ArrayList<>();
        Long declared = null;
        boolean sawDocuments = false;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String name = parser.currentName();
            parser.nextToken();
            if (JsonFields.DOCUMENTS.equals(name)) {
                sawDocuments = true;
                movies.addAll(readElements(parser));
            } else if (JsonFields.DOCUMENT_COUNT.equals(name)) {
                declared = count(parser);
            } else {
                parser.skipChildren();
            }
        }
        requireDocuments(sawDocuments);
        requireCount(declared, movies.size());
        return movies;
    }

    private static List<Movie> readElements(JsonParser parser) {
        if (parser.currentToken() != JsonToken.START_ARRAY) {
            throw new IngestValidationException(ErrorCode.UNREADABLE_JSON);
        }
        List<Movie> movies = new ArrayList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            movies.add(MovieDocuments.read(parser));
        }
        return movies;
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
}
