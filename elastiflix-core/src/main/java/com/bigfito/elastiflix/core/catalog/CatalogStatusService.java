package com.bigfito.elastiflix.core.catalog;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Calcula el estado del catálogo, sirva el nombre de alias o de índice concreto.
 */
@Service
public class CatalogStatusService {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogStatusService.class);

    private final CatalogIndex catalog;
    private final ElastiflixProperties properties;

    /**
     * Crea el servicio con el catálogo y el alias configurado.
     *
     * @param catalog    operaciones del catálogo
     * @param properties configuración; el alias sale de aquí
     */
    public CatalogStatusService(CatalogIndex catalog, ElastiflixProperties properties) {
        this.catalog = catalog;
        this.properties = properties;
    }

    /**
     * Lee el alias configurado y dice si está conectado.
     *
     * @return estado, con el conteo si se pudo leer
     */
    public CatalogStatus status() {
        String name = properties.index().alias();
        try {
            CatalogStatus status = connectedOrNot(name, catalog.describe(name));
            logStatus(status);
            return status;
        } catch (ElasticsearchAccessException exception) {
            LOG.error("{} en {}", exception.errorCode().code(), name);
            return failed(name, exception.errorCode());
        }
    }

    private CatalogStatus connectedOrNot(String name, CatalogName described) {
        if (described instanceof CatalogName.Alias found) {
            return counted(name, found.concreteIndex());
        }
        if (described instanceof CatalogName.ConcreteIndex found) {
            return counted(name, found.name());
        }
        return failed(name, errorOf(described));
    }

    private CatalogStatus counted(String name, String concreteIndex) {
        long documents = catalog.count(name);
        return new CatalogStatus(name, concreteIndex, documents, true, null);
    }

    private static CatalogStatus failed(String name, ErrorCode code) {
        return new CatalogStatus(name, null, null, false, code.code());
    }

    private static ErrorCode errorOf(CatalogName described) {
        if (described instanceof CatalogName.Ambiguous) {
            return ErrorCode.ALIAS_AMBIGUOUS;
        }
        return ErrorCode.CATALOG_NOT_FOUND;
    }

    private static void logStatus(CatalogStatus status) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Estado del catálogo: conectado={}", status.connected());
        }
    }
}
