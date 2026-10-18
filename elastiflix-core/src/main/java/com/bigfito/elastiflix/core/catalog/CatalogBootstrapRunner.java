package com.bigfito.elastiflix.core.catalog;

import java.io.IOException;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Ejecuta la migración solo cuando {@code elastiflix.bootstrap.enabled} es {@code true}.
 *
 * <p>Al terminar cierra el cliente. Si no, sus hilos impiden que el proceso salga.</p>
 */
@Component
@ConditionalOnProperty(prefix = "elastiflix.bootstrap", name = "enabled", havingValue = "true")
public class CatalogBootstrapRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogBootstrapRunner.class);

    private final CatalogBootstrap bootstrap;
    private final ElasticsearchClient client;

    /**
     * Crea el runner.
     *
     * @param bootstrap migración del catálogo
     * @param client    cliente que hay que cerrar al terminar
     */
    public CatalogBootstrapRunner(CatalogBootstrap bootstrap, ElasticsearchClient client) {
        this.bootstrap = bootstrap;
        this.client = client;
    }

    /**
     * Migra y deja que Boot cierre el proceso.
     *
     * @param args argumentos de la línea de comandos; no se usan
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            bootstrap.migrate();
        } finally {
            closeClient();
        }
    }

    private void closeClient() {
        try {
            client.close();
        } catch (IOException exception) {
            LOG.warn("No se pudo cerrar el cliente de Elasticsearch");
        }
    }
}
