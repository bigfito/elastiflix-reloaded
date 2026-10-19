package com.bigfito.elastiflix.ingest.movie;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.IngestValidationException;
import com.bigfito.elastiflix.core.model.Movie;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Construye una película desde el objeto que armó el validador.
 *
 * <p>Ese objeto ya viene sin {@code _id}, sin vectores y sin textos en blanco: los vectores se
 * generan después, con el servicio de inferencia.</p>
 */
public final class MovieDocuments {

    private static final JsonMapper MAPPER = MovieJsonMapper.create();

    private MovieDocuments() {
    }

    /**
     * Convierte un objeto ya validado en película. Sirve igual para JSON y para CSV.
     *
     * @param node objeto de una película ya validado
     * @return película sin vectores
     * @throws IngestValidationException {@code EFX-ING-012} si el valor no es un objeto o no encaja en el modelo
     */
    public static Movie fromNode(JsonNode node) {
        if (!(node instanceof ObjectNode object)) {
            throw new IngestValidationException(ErrorCode.TYPE_MISMATCH);
        }
        try {
            return MAPPER.treeToValue(object, Movie.class);
        } catch (JacksonException exception) {
            throw new IngestValidationException(ErrorCode.TYPE_MISMATCH, exception);
        }
    }
}
