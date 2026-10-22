package com.bigfito.elastiflix.ingest.job;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;

import com.bigfito.elastiflix.core.catalog.AppendResult;
import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.catalog.CatalogName;
import com.bigfito.elastiflix.core.model.Movie;

/**
 * Avisa para que la prueba cancele en el punto que la carga revisa a continuación.
 *
 * <p>En bulk, al crear el temporal (antes del primer lote); en append, al volver del primer lote escrito por el
 * alias (antes del segundo). El resto de las llamadas siguen al catálogo de verdad.</p>
 */
final class CancelGateCatalog implements CatalogIndex {

    private final CatalogIndex delegate;
    private final CancelGate gate;

    CancelGateCatalog(CatalogIndex delegate, CancelGate gate) {
        this.delegate = delegate;
        this.gate = gate;
    }

    @Override
    public CatalogName describe(String name) {
        return delegate.describe(name);
    }

    @Override
    public void createWithMapping(String indexName) {
        delegate.createWithMapping(indexName);
        gate.fire();
    }

    @Override
    public void reindex(String source, String target, BooleanSupplier stop) {
        delegate.reindex(source, target, stop);
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
    public List<String> listOwnedIndexes(String alias) {
        return delegate.listOwnedIndexes(alias);
    }

    @Override
    public List<Movie> readWithVectors(String name, List<String> ids) {
        return delegate.readWithVectors(name, ids);
    }

    @Override
    public List<Movie> readAllWithVectors(String name, int limit) {
        return delegate.readAllWithVectors(name, limit);
    }

    @Override
    public Set<String> existingIds(String name, List<String> ids) {
        return delegate.existingIds(name, ids);
    }

    @Override
    public void writeAll(String indexName, List<Movie> movies) {
        delegate.writeAll(indexName, movies);
    }

    @Override
    public AppendResult appendAll(String alias, List<Movie> movies) {
        AppendResult result = delegate.appendAll(alias, movies);
        gate.fire();
        return result;
    }

    @Override
    public void indexOne(String name, Movie movie) {
        delegate.indexOne(name, movie);
    }

    @Override
    public Optional<Movie> findById(String name, String id) {
        return delegate.findById(name, id);
    }
}
