package com.bigfito.elastiflix.ingest.job;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.catalog.IndexNames;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Borra índices {@code <alias>-<AAAAMMDD-HHMMSS>} que no son el catálogo vigente ni tienen otro alias.
 *
 * <p>La limpieza es una ayuda, no un paso de la importación: si un borrado falla, queda un {@code WARN} y la
 * importación sigue. El huérfano se vuelve a intentar en la siguiente.</p>
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
     * Borra huérfanos de ese alias. No toca el índice vigente, otro prefijo ni un índice con otro alias. Un borrado
     * que falla no detiene a los demás ni a la importación.
     *
     * @param catalog   operaciones del catálogo
     * @param alias     alias de esta importación
     * @param liveIndex índice que está detrás del alias, o {@code null} si el catálogo todavía no existe
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
        try {
            catalog.deleteIndex(index);
        } catch (ElasticsearchAccessException exception) {
            LOG.warn("{}: no se pudo borrar el índice huérfano {}; se reintentará en la próxima importación",
                    exception.errorCode().code(), index);
        }
    }
}
