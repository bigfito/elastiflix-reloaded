package com.bigfito.elastiflix.core.client;

import com.bigfito.elastiflix.core.TestApplication;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code ElasticsearchJsonConfig}: la autoconfiguración de Spring Boot entrega un mapper de Jackson 3.
 */
class ElasticsearchJsonConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestApplication.class)
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
}
