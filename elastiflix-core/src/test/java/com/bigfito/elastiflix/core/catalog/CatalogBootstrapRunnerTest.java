package com.bigfito.elastiflix.core.catalog;

import java.io.IOException;

import com.bigfito.elastiflix.core.error.ConfigurationException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La migración no cierra el cliente compartido si Tomcat sigue en pie.
 */
class CatalogBootstrapRunnerTest {

    @Test
    @DisplayName("con servidor web responde EFX-CFG-004 y no cierra el cliente")
    void shouldRejectBootstrapWhenTheWebServerIsUp() throws IOException {
        CatalogBootstrap bootstrap = mock(CatalogBootstrap.class);
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        CatalogBootstrapRunner runner = new CatalogBootstrapRunner(bootstrap, client, context("servlet"));

        assertThatThrownBy(() -> runner.run(null))
                .isInstanceOf(ConfigurationException.class)
                .extracting(thrown -> ((ConfigurationException) thrown).errorCode())
                .isEqualTo(ErrorCode.BOOTSTRAP_WITH_WEB);
        verify(bootstrap, never()).migrate();
        verify(client, never()).close();
    }

    @Test
    @DisplayName("sin servidor web migra y cierra el cliente")
    void shouldCloseTheClientWhenThereIsNoWebServer() throws IOException {
        CatalogBootstrap bootstrap = mock(CatalogBootstrap.class);
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        CatalogBootstrapRunner runner = new CatalogBootstrapRunner(bootstrap, client, context("none"));

        runner.run(null);

        verify(bootstrap).migrate();
        verify(client).close();
    }

    private static ApplicationContext context(String webType) {
        ApplicationContext context = mock(ApplicationContext.class);
        Environment environment = mock(Environment.class);
        when(context.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("spring.main.web-application-type", "servlet")).thenReturn(webType);
        return context;
    }
}
