package com.bigfito.elastiflix.core.catalog;

import java.io.IOException;

import com.bigfito.elastiflix.core.error.ConfigurationException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * La migración no cierra el cliente compartido si Tomcat sigue en pie.
 */
class CatalogBootstrapRunnerTest {

    @Test
    @DisplayName("con servidor web responde EFX-CFG-004 y no cierra el cliente")
    void shouldRejectBootstrapWhenTheWebServerIsUp() throws IOException {
        // given: el contexto de una aplicación con servidor web
        CatalogBootstrap bootstrap = mock(CatalogBootstrap.class);
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        ApplicationContext context = mock(WebServerApplicationContext.class);
        CatalogBootstrapRunner runner = new CatalogBootstrapRunner(bootstrap, client, context);

        // when / then
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
        // given: un contexto sin servidor web, como el de --spring.main.web-application-type=none
        CatalogBootstrap bootstrap = mock(CatalogBootstrap.class);
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        ApplicationContext context = mock(ApplicationContext.class);
        CatalogBootstrapRunner runner = new CatalogBootstrapRunner(bootstrap, client, context);

        // when
        runner.run(null);

        // then
        verify(bootstrap).migrate();
        verify(client).close();
    }
}
