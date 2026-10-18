package com.bigfito.elastiflix.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los grupos que no vienen en el entorno usan los valores del plan.
 */
class ElastiflixPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesOnly.class)
            .withPropertyValues(
                    "elastiflix.elasticsearch.url=http://127.0.0.1:9",
                    "elastiflix.elasticsearch.api-key=test-key");

    @Test
    @DisplayName("sin el resto de propiedades quedan los valores por defecto, embeddings incluidos")
    void shouldUsePlanDefaultsWhenOptionalGroupsAreMissing() {
        runner.run(context -> {
            ElastiflixProperties properties = context.getBean(ElastiflixProperties.class);
            assertThat(properties.index().alias()).isEqualTo("elastiflix-reloaded");
            assertThat(properties.inference().id()).isEqualTo(".jina-embeddings-v5-omni-small");
            assertThat(properties.ingest().batchSize()).isEqualTo(100);
            assertThat(properties.ingest().maxFileSize().toMegabytes()).isEqualTo(150);
            assertThat(properties.poster().baseUrl()).isEqualTo("https://image.tmdb.org/t/p/w342");
            assertThat(properties.bootstrap().enabled()).isFalse();
            assertThat(properties.embedding().documentInputType()).isEqualTo("ingest");
            assertThat(properties.embedding().documentPrefix()).isEqualTo("Document: ");
        });
    }

    /**
     * Solo enlaza las propiedades. No arranca el cliente de Elasticsearch.
     */
    @Configuration
    @EnableConfigurationProperties(ElastiflixProperties.class)
    static class PropertiesOnly {
    }
}
