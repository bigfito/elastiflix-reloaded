package com.bigfito.elastiflix.search.strategy;

import com.bigfito.elastiflix.search.query.VectorField;

import co.elastic.clients.elasticsearch.core.search.SourceConfig;

/**
 * Pide la película sin los vectores. Esos campos no se muestran y no deben viajar.
 */
public final class SearchSource {

    private SearchSource() {
    }

    /**
     * Excluye {@code plot_256}, {@code plot_512} y {@code plot_1024}.
     *
     * @return la configuración de {@code _source}
     */
    public static SourceConfig withoutVectors() {
        return SourceConfig.of(source -> source.filter(filter -> filter.excludes(
                VectorField.PLOT_256.fieldName(),
                VectorField.PLOT_512.fieldName(),
                VectorField.PLOT_1024.fieldName())));
    }
}
