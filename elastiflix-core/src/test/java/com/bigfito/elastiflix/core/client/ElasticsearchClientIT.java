package com.bigfito.elastiflix.core.client;

import com.bigfito.elastiflix.core.TestApplication;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba que el cliente llega al proyecto Serverless.
 *
 * <p>No registra la respuesta completa: solo exige que {@code info()} traiga una versión.</p>
 */
@SpringBootTest(classes = TestApplication.class)
class ElasticsearchClientIT {

    private final ElasticsearchClient client;

    /**
     * Spring no inyecta el constructor de una prueba si no lleva {@code @Autowired}.
     *
     * @param client cliente creado por la autoconfiguración de Boot
     */
    @Autowired
    ElasticsearchClientIT(ElasticsearchClient client) {
        this.client = client;
    }

    @Test
    @DisplayName("info() responde sin error")
    void shouldReadServerInfoWhenClientConnects() throws Exception {
        // when
        String version = client.info().version().number();

        // then
        assertThat(version).isNotBlank();
    }
}
