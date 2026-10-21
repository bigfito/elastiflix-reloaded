package com.bigfito.elastiflix.ingest.job;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.catalog.CatalogName;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;

/**
 * Exige que el catálogo sea un alias de un solo índice antes de cargar, en los dos modos.
 */
final class CatalogAliases {

    private CatalogAliases() {
    }

    /**
     * Lee el alias y devuelve su índice.
     *
     * <p>Si el nombre no existe, {@code EFX-ES-002}; si apunta a varios índices, {@code EFX-ES-005}; si es un índice
     * concreto, {@code EFX-IMP-004}: bulk no debe borrar con {@code remove_index} el único índice, y append solo
     * escribe por un alias.</p>
     *
     * @param catalog operaciones del catálogo
     * @param alias   alias configurado
     * @return el alias y su índice concreto
     */
    static CatalogName.Alias require(CatalogIndex catalog, String alias) {
        CatalogName described = catalog.describe(alias);
        if (described instanceof CatalogName.Alias found) {
            return found;
        }
        throw rejection(described);
    }

    private static ElastiflixException rejection(CatalogName described) {
        if (described instanceof CatalogName.Missing) {
            return new ElasticsearchAccessException(ErrorCode.CATALOG_NOT_FOUND);
        }
        if (described instanceof CatalogName.Ambiguous) {
            return new ElasticsearchAccessException(ErrorCode.ALIAS_AMBIGUOUS);
        }
        return new ImportException(ErrorCode.CATALOG_NOT_ALIAS);
    }
}
