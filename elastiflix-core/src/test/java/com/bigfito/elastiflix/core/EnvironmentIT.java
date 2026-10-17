package com.bigfito.elastiflix.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba que las pruebas de integración reciben la URL y la API key de Elasticsearch.
 *
 * <p>No imprime esos valores: si faltan, el mensaje solo dice qué propiedad está vacía.</p>
 */
@SpringBootTest(classes = TestApplication.class)
class EnvironmentIT {

    private final Environment environment;

    /**
     * Spring no inyecta el constructor de una prueba si no lleva {@code @Autowired}.
     *
     * @param environment entorno ya cargado con {@code .env}
     */
    @Autowired
    EnvironmentIT(Environment environment) {
        this.environment = environment;
    }

    @Test
    @DisplayName("la URL y la API key de Elasticsearch llegan configuradas")
    void shouldExposeElasticsearchSettingsWhenContextStarts() {
        // when
        String url = environment.getProperty("elastiflix.elasticsearch.url");
        String apiKey = environment.getProperty("elastiflix.elasticsearch.api-key");

        // then
        assertThat(hasText(url))
                .as("elastiflix.elasticsearch.url no está vacía (revisa ELASTICSEARCH_URL en .env)")
                .isTrue();
        assertThat(hasText(apiKey))
                .as("elastiflix.elasticsearch.api-key no está vacía (revisa ELASTICSEARCH_API_KEY en .env)")
                .isTrue();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
