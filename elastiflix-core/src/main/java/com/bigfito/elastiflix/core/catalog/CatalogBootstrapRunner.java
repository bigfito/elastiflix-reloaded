package com.bigfito.elastiflix.core.catalog;

import java.io.IOException;

import com.bigfito.elastiflix.core.error.ConfigurationException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Ejecuta la migración solo cuando {@code elastiflix.bootstrap.enabled} es {@code true}.
 *
 * <p>Al terminar cierra el cliente, salvo que el servidor web siga activo: en ese caso
 * responde {@code EFX-CFG-004} y no lo cierra.</p>
 */
@Component
@ConditionalOnProperty(prefix = "elastiflix.bootstrap", name = "enabled", havingValue = "true")
public class CatalogBootstrapRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogBootstrapRunner.class);

    private final CatalogBootstrap bootstrap;
    private final ElasticsearchClient client;
    private final ApplicationContext context;

    /**
     * Crea el runner.
     *
     * @param bootstrap migración del catálogo
     * @param client    cliente que hay que cerrar al terminar, solo sin servidor web
     * @param context   sirve para saber si Tomcat sigue en pie
     */
    public CatalogBootstrapRunner(
            CatalogBootstrap bootstrap, ElasticsearchClient client, ApplicationContext context) {
        this.bootstrap = bootstrap;
        this.client = client;
        this.context = context;
    }

    /**
     * Migra y deja que Boot cierre el proceso. Con servidor web no toca el cliente.
     *
     * @param args argumentos de la línea de comandos; no se usan
     */
    @Override
    public void run(ApplicationArguments args) {
        if (webServerStaysUp()) {
            throw new ConfigurationException(ErrorCode.BOOTSTRAP_WITH_WEB);
        }
        try {
            bootstrap.migrate();
        } finally {
            closeClient();
        }
    }

    private boolean webServerStaysUp() {
        String type = context.getEnvironment().getProperty("spring.main.web-application-type", "servlet");
        return !"none".equalsIgnoreCase(type);
    }

    private void closeClient() {
        try {
            client.close();
        } catch (IOException exception) {
            LOG.warn("No se pudo cerrar el cliente de Elasticsearch");
        }
    }
}
