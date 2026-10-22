package com.bigfito.elastiflix.ingest.job;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.catalog.CatalogName;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;

/**
 * Lee el alias del catálogo antes de cargar, en los dos modos y en el registro individual.
 *
 * <p>Un alias en varios índices da {@code EFX-ES-005} y un índice concreto con ese nombre, {@code EFX-IMP-004}: bulk
 * no debe borrar con {@code remove_index} el único índice, y append solo escribe por un alias. Un alias que no existe
 * depende del camino: bulk y el registro individual crean el catálogo; append se bloquea.</p>
 */
final class CatalogAliases {

    private CatalogAliases() {
    }

    /**
     * Exige un alias de un solo índice y devuelve su índice.
     *
     * @param catalog     operaciones del catálogo
     * @param alias       alias configurado
     * @param missingCode código si el nombre no existe
     * @return el alias y su índice concreto
     */
    static CatalogName.Alias require(CatalogIndex catalog, String alias, ErrorCode missingCode) {
        CatalogName described = catalog.describe(alias);
        if (described instanceof CatalogName.Alias found) {
            return found;
        }
        if (described instanceof CatalogName.Missing) {
            throw new ImportException(missingCode);
        }
        throw rejection(described);
    }

    /**
     * El índice que está detrás del alias, o {@code null} si el catálogo todavía no existe.
     *
     * @param catalog operaciones del catálogo
     * @param alias   alias configurado
     * @return el índice vigente, o {@code null} si el nombre no existe
     */
    static String currentIndex(CatalogIndex catalog, String alias) {
        CatalogName described = catalog.describe(alias);
        if (described instanceof CatalogName.Alias found) {
            return found.concreteIndex();
        }
        if (described instanceof CatalogName.Missing) {
            return null;
        }
        throw rejection(described);
    }

    private static ElastiflixException rejection(CatalogName described) {
        if (described instanceof CatalogName.Ambiguous) {
            return new ElasticsearchAccessException(ErrorCode.ALIAS_AMBIGUOUS);
        }
        return new ImportException(ErrorCode.CATALOG_NOT_ALIAS);
    }
}
