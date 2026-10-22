package com.bigfito.elastiflix.core.client;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publica el mapper de películas para que el cliente de Elasticsearch lo use.
 *
 * <p>Boot no crea el suyo si ya hay un {@link JsonpMapper}. El cliente HTTP no se personaliza.</p>
 */
@Configuration
public class ElasticsearchJsonConfig {

    /**
     * Mapper Jackson 3 con los nombres del índice.
     *
     * @return mapper del cliente
     */
    @Bean
    public JsonpMapper elasticsearchJsonpMapper() {
        return new Jackson3JsonpMapper(MovieJsonMapper.create());
    }
}
