package com.bigfito.elastiflix.core.client;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code ElasticsearchJsonConfig}: la autoconfiguración de Spring Boot entrega un mapper de Jackson 3.
 */
class ElasticsearchJsonConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CoreOnly.class)
            .withPropertyValues(
                    "spring.elasticsearch.uris=http://127.0.0.1:9",
                    "spring.elasticsearch.api-key=test-key",
                    "management.health.elasticsearch.enabled=false");

    @Test
    @DisplayName("el mapper del cliente es Jackson3JsonpMapper")
    void shouldUseJackson3MapperWhenContextStarts() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(JsonpMapper.class)).isInstanceOf(Jackson3JsonpMapper.class);
        });
    }

    /** Solo el núcleo y la autoconfiguración de Spring Boot, sin las páginas web. */
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @ConfigurationPropertiesScan("com.bigfito.elastiflix.core")
    @ComponentScan("com.bigfito.elastiflix.core")
    static class CoreOnly {
    }
}
