package com.bigfito.elastiflix.ingest.movie;

import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.IngestValidationException;
import com.bigfito.elastiflix.core.model.Movie;

import tools.jackson.core.JacksonException;
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
     * Convierte un objeto ya validado en película. Sirve igual para JSON y para CSV.
     *
     * @param node valor de una película
     * @return película sin vectores
     * @throws IngestValidationException {@code EFX-ING-012} si el valor no es un objeto o no encaja en el modelo
     */
    public static Movie fromNode(JsonNode node) {
        if (!(node instanceof ObjectNode object)) {
            throw new IngestValidationException(ErrorCode.TYPE_MISMATCH);
        }
        dropBlankText(object);
        object.remove(IGNORED);
        try {
            return MAPPER.treeToValue(object, Movie.class);
        } catch (JacksonException exception) {
            throw new IngestValidationException(ErrorCode.TYPE_MISMATCH, exception);
        }
    }
}
