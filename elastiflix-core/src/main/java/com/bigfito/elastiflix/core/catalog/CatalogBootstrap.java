package com.bigfito.elastiflix.core.catalog;

import java.time.Clock;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Pasa el catálogo de un índice concreto a un alias, sin tocar nada si ya es alias.
 */
@Component
public class CatalogBootstrap {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogBootstrap.class);

    private final CatalogIndex catalog;
    private final ElastiflixProperties properties;
    private final IndexNames names;
    private final Clock clock;

    /**
     * Crea la migración.
     *
     * @param catalog    operaciones del catálogo
     * @param properties alias configurado
     * @param names      nombres con fecha UTC
     * @param clock      reloj de esa fecha
     */
    public CatalogBootstrap(CatalogIndex catalog, ElastiflixProperties properties, IndexNames names, Clock clock) {
        this.catalog = catalog;
        this.properties = properties;
        this.names = names;
        this.clock = clock;
    }

    /**
     * Migra el índice concreto, o no hace nada si el alias ya existe.
     */
    public void migrate() {
        String alias = properties.index().alias();
        CatalogName described = catalog.describe(alias);
        if (described instanceof CatalogName.Alias found) {
            logExisting(alias, found.concreteIndex());
            return;
        }
        requireConcrete(described);
        copyAndPublish(alias);
    }

    private void requireConcrete(CatalogName described) {
        if (described instanceof CatalogName.Missing) {
            throw new ElasticsearchAccessException(ErrorCode.CATALOG_NOT_FOUND);
        }
        if (described instanceof CatalogName.Ambiguous) {
            throw new ElasticsearchAccessException(ErrorCode.ALIAS_AMBIGUOUS);
        }
    }

    private void copyAndPublish(String alias) {
        String target = names.timestamped(alias, clock);
        catalog.createWithMapping(target);
        try {
            reindexAndCheck(alias, target);
        } catch (ElasticsearchAccessException exception) {
            deleteNewIfOriginalRemains(alias, target);
            throw exception;
        }
        publish(alias, target);
    }

    private void reindexAndCheck(String alias, String target) {
        catalog.reindex(alias, target);
        catalog.refresh(target);
        if (catalog.count(alias) != catalog.count(target)) {
            throw new ElasticsearchAccessException(ErrorCode.REINDEX_COUNT_MISMATCH);
        }
    }

    private void publish(String alias, String target) {
        try {
            catalog.publish(alias, target, alias);
        } catch (ElasticsearchAccessException exception) {
            deleteNewIfOriginalRemains(alias, target);
            throw new ElasticsearchAccessException(ErrorCode.ALIAS_PUBLISH_FAILED, exception);
        }
        LOG.info("El alias {} quedó en {} con {} documentos", alias, target, catalog.count(alias));
    }

    private void logExisting(String alias, String concreteIndex) {
        LOG.info("El alias {} ya apunta a {} con {} documentos", alias, concreteIndex, catalog.count(alias));
    }

    private void deleteNewIfOriginalRemains(String alias, String target) {
        try {
            if (catalog.describe(alias) instanceof CatalogName.ConcreteIndex) {
                catalog.deleteIndex(target);
            }
        } catch (ElasticsearchAccessException exception) {
            LOG.warn("No se pudo borrar el índice nuevo {}", target);
        }
    }
}
