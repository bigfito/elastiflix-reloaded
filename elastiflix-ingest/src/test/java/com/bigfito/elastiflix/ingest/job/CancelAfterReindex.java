package com.bigfito.elastiflix.ingest.job;

import java.util.function.BooleanSupplier;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;

/**
 * Al volver de {@code reindex}, avisa para que la prueba cancele.
 *
 * <p>El resto de las llamadas siguen al catálogo de verdad.</p>
 */
final class CancelAfterReindex implements CatalogIndex {

    private final CatalogIndex delegate;
    private final CancelGate gate;

    CancelAfterReindex(CatalogIndex delegate, CancelGate gate) {
        this.delegate = delegate;
        this.gate = gate;
    }

    @Override
    public com.bigfito.elastiflix.core.catalog.CatalogName describe(String name) {
        return delegate.describe(name);
    }

    @Override
    public void createWithMapping(String indexName) {
        delegate.createWithMapping(indexName);
    }

    @Override
    public void reindex(String source, String target, BooleanSupplier stop) {
        delegate.reindex(source, target, stop);
        gate.fire();
    }

    @Override
    public void refresh(String name) {
        delegate.refresh(name);
    }

    @Override
    public long count(String name) {
        return delegate.count(name);
    }

    @Override
    public void publish(String alias, String newIndex, String previousIndex) {
        delegate.publish(alias, newIndex, previousIndex);
    }

    @Override
    public void deleteIndex(String indexName) {
        delegate.deleteIndex(indexName);
    }

    @Override
    public java.util.List<String> listOwnedIndexes(String alias) {
        return delegate.listOwnedIndexes(alias);
    }

    @Override
    public java.util.List<com.bigfito.elastiflix.core.model.Movie> readWithVectors(
            String name, java.util.List<String> ids) {
        return delegate.readWithVectors(name, ids);
    }

    @Override
    public java.util.List<com.bigfito.elastiflix.core.model.Movie> readAllWithVectors(String name, int limit) {
        return delegate.readAllWithVectors(name, limit);
    }

    @Override
    public java.util.Set<String> existingIds(String name, java.util.List<String> ids) {
        return delegate.existingIds(name, ids);
    }

    @Override
    public void writeAll(String indexName, java.util.List<com.bigfito.elastiflix.core.model.Movie> movies) {
        delegate.writeAll(indexName, movies);
    }

    @Override
    public void indexOne(String name, com.bigfito.elastiflix.core.model.Movie movie) {
        delegate.indexOne(name, movie);
    }
}
