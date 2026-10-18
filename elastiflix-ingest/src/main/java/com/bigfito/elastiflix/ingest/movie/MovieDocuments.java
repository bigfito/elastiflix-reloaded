package com.bigfito.elastiflix.ingest.movie;

import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.IngestValidationException;
import com.bigfito.elastiflix.core.model.Movie;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Construye una película y deja fuera los vectores que pudiera traer el archivo.
 *
 * <p>Esos vectores se generan después, con el servicio de inferencia. Aquí no se validan.</p>
 */
public final class MovieDocuments {

    private static final JsonMapper MAPPER = MovieJsonMapper.create();
    private static final List<String> IGNORED = List.of(
            "_id", "plot_256", "plot_512", "plot_1024");

    private MovieDocuments() {
    }

    private static void dropBlankText(ObjectNode object) {
        List<String> blank = new ArrayList<>();
        object.forEachEntry((name, value) -> {
            if (value.isString() && value.asString().isBlank()) {
                blank.add(name);
            }
        });
        object.remove(blank);
    }

    /**
     * Lee el valor actual del parser como película.
     *
     * @param parser posicionado al inicio del objeto
     * @return película sin vectores
     */
    public static Movie read(JsonParser parser) {
        return fromNode((JsonNode) parser.readValueAsTree());
    }

    /**
     * Convierte un objeto JSON. Un valor que no es objeto no se puede leer.
     *
     * @param node valor de una película
     * @return película sin vectores
     */
    public static Movie fromNode(JsonNode node) {
        if (!(node instanceof ObjectNode object)) {
            throw new IngestValidationException(ErrorCode.UNREADABLE_JSON);
        }
        dropBlankText(object);
        object.remove(IGNORED);
        try {
            return MAPPER.treeToValue(object, Movie.class);
        } catch (JacksonException exception) {
            throw new IngestValidationException(ErrorCode.UNREADABLE_JSON, exception);
        }
    }
}
