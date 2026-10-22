package com.bigfito.elastiflix.core.config;

import com.bigfito.elastiflix.core.error.ConfigurationException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Impide arrancar si faltan la URL o la API key de Elasticsearch.
 *
 * <p>Corre antes de crear beans, así que el cliente no llega a conectarse.</p>
 */
public final class ElasticsearchCredentialsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    /** Propiedad de la URL. */
    static final String URL_PROPERTY = "elastiflix.elasticsearch.url";

    /** Propiedad de la API key. */
    static final String API_KEY_PROPERTY = "elastiflix.elasticsearch.api-key";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!hasText(environment.getProperty(URL_PROPERTY))) {
            throw new ConfigurationException(ErrorCode.MISSING_ELASTICSEARCH_URL);
        }
        if (!hasText(environment.getProperty(API_KEY_PROPERTY))) {
            throw new ConfigurationException(ErrorCode.MISSING_API_KEY);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
