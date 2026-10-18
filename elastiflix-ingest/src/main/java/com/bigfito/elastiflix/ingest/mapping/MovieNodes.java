package com.bigfito.elastiflix.ingest.mapping;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;

import tools.jackson.databind.json.JsonMapper;

/**
 * Mapper compartido para armar el objeto limpio de una película.
 */
final class MovieNodes {

    static final JsonMapper MAPPER = MovieJsonMapper.create();

    private MovieNodes() {
    }
}
