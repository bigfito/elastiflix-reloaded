package com.bigfito.elastiflix.ingest.job;

import java.time.Clock;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.catalog.CatalogName;
import com.bigfito.elastiflix.core.catalog.IndexNames;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ElasticsearchDiagnostics;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Crea el catálogo cuando todavía no existe, para el registro individual.
 *
 * <p>Crea {@code <alias>-<AAAAMMDD-HHMMSS>} con el mapping del repo y le pone el alias con {@code _aliases} solo con
 * {@code add}. Si la publicación no confirma, {@code describe} decide, como en la importación: alias en el índice
 * nuevo → listo; rechazo 4xx con el alias todavía ausente → se borra el índice nuevo; corte, 5xx o {@code describe}
 * ilegible → {@code EFX-IMP-006} y el índice se deja.</p>
 */
@Component
public final class FirstCatalog {

    private static final Logger LOG = LoggerFactory.getLogger(FirstCatalog.class);

    private final CatalogIndex catalog;
    private final IndexNames names;
    private final Clock clock;

    /**
     * Crea el ayudante con las puertas al catálogo.
     *
     * @param catalog operaciones del catálogo
     * @param names   nombre del índice nuevo
     * @param clock   reloj UTC de ese nombre
     */
    public FirstCatalog(CatalogIndex catalog, IndexNames names, Clock clock) {
        this.catalog = catalog;
        this.names = names;
        this.clock = clock;
    }

    /**
     * Si el alias no existe, crea el índice con el mapping y le pone el alias. Si ya existe, no hace nada.
     *
     * @param alias alias del catálogo
     */
    public void ensure(String alias) {
        if (CatalogAliases.currentIndex(catalog, alias) != null) {
            return;
        }
        String index = names.timestamped(alias, clock);
        catalog.createWithMapping(index);
        try {
            catalog.publish(alias, index, null);
        } catch (ElasticsearchAccessException failure) {
            settle(alias, index, failure);
        }
        LOG.info("Se creó el catálogo {} en el índice {}", alias, index);
    }

    private void settle(String alias, String index, ElasticsearchAccessException failure) {
        CatalogName described = describeAfter(alias);
        if (described instanceof CatalogName.Alias found && index.equals(found.concreteIndex())) {
            return;
        }
        if (ElasticsearchDiagnostics.isClientRejection(failure) && described instanceof CatalogName.Missing) {
            deleteQuietly(index);
            throw new ElasticsearchAccessException(ErrorCode.ALIAS_PUBLISH_FAILED, failure);
        }
        throw new ImportException(ErrorCode.PUBLISH_UNCERTAIN, failure);
    }

    private CatalogName describeAfter(String alias) {
        try {
            return catalog.describe(alias);
        } catch (ElastiflixException exception) {
            throw new ImportException(ErrorCode.PUBLISH_UNCERTAIN, exception);
        }
    }

    private void deleteQuietly(String index) {
        try {
            catalog.deleteIndex(index);
        } catch (ElasticsearchAccessException exception) {
            LOG.warn("No se pudo borrar el índice {} del catálogo que no se publicó", index);
        }
    }
}
