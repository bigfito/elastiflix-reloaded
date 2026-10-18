package com.bigfito.elastiflix.ingest.job;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.catalog.IndexNames;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Borra índices {@code <alias>-<AAAAMMDD-HHMMSS>} que no son el catálogo vigente.
 */
@Component
public final class OrphanCleaner {

    private static final Logger LOG = LoggerFactory.getLogger(OrphanCleaner.class);

    private final IndexNames names;

    /**
     * Crea el limpiador con el patrón de nombres de esta demo.
     *
     * @param names reconoce {@code <alias>-<AAAAMMDD-HHMMSS>}
     */
    public OrphanCleaner(IndexNames names) {
        this.names = names;
    }

    /**
     * Borra huérfanos de ese alias. No toca el índice vigente ni otro prefijo.
     *
     * @param catalog   operaciones del catálogo
     * @param alias     alias de esta importación
     * @param liveIndex índice que está detrás del alias
     */
    public void clean(CatalogIndex catalog, String alias, String liveIndex) {
        for (String index : catalog.listOwnedIndexes(alias)) {
            deleteIfOrphan(catalog, alias, liveIndex, index);
        }
    }

    private void deleteIfOrphan(CatalogIndex catalog, String alias, String liveIndex, String index) {
        if (!names.isOwnedBy(alias, index) || index.equals(liveIndex)) {
            return;
        }
        LOG.warn("Se borra el índice huérfano {}", index);
        catalog.deleteIndex(index);
    }
}
