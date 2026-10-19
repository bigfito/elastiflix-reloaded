package com.bigfito.elastiflix.core;

import java.util.Map;

import com.bigfito.elastiflix.core.config.ElasticsearchCredentialsEnvironmentPostProcessor;
import com.bigfito.elastiflix.core.error.ConfigurationException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprueba el post-procesador de credenciales sobre el entorno real de las pruebas de integración.
 *
 * <p>Si el contexto arrancó, el post-procesador ya aceptó la URL y la API key de {@code .env}. Esta prueba
 * va más allá: sobre una copia de ese mismo entorno, sin la URL, el post-procesador tiene que detenerse con
 * {@code EFX-CFG-001}. No imprime los valores.</p>
 */
@SpringBootTest(classes = TestApplication.class)
class EnvironmentIT {

    private static final String URL_PROPERTY = "elastiflix.elasticsearch.url";

    private final ConfigurableEnvironment environment;
    private final ElasticsearchCredentialsEnvironmentPostProcessor postProcessor =
            new ElasticsearchCredentialsEnvironmentPostProcessor();

    /**
     * Spring no inyecta el constructor de una prueba si no lleva {@code @Autowired}.
     *
     * @param environment entorno ya cargado con {@code .env}
     */
    @Autowired
    EnvironmentIT(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Test
    @DisplayName("el entorno de .env pasa el post-procesador, y sin la URL se detiene con EFX-CFG-001")
    void shouldStopWithoutTheUrlWhenTheRealEnvironmentLosesIt() {
        // given
        ConfigurableEnvironment withoutUrl = copyOf(environment);
        withoutUrl.getPropertySources().addFirst(new MapPropertySource("sin-url", Map.of(URL_PROPERTY, "")));
        SpringApplication application = new SpringApplication(TestApplication.class);

        // when / then
        assertThatCode(() -> postProcessor.postProcessEnvironment(environment, application))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(withoutUrl, application))
                .isInstanceOf(ConfigurationException.class)
                .extracting(thrown -> ((ConfigurationException) thrown).errorCode())
                .isEqualTo(ErrorCode.MISSING_ELASTICSEARCH_URL);
    }

    private static ConfigurableEnvironment copyOf(ConfigurableEnvironment source) {
        StandardEnvironment copy = new StandardEnvironment();
        copy.getPropertySources().forEach(existing -> copy.getPropertySources().remove(existing.getName()));
        for (PropertySource<?> propertySource : source.getPropertySources()) {
            copy.getPropertySources().addLast(propertySource);
        }
        return copy;
    }
}
