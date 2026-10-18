package com.bigfito.elastiflix.core.catalog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ElasticsearchDiagnostics;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.core.model.Movie;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Traduce {@link CatalogIndex} a las llamadas del cliente Java de Elasticsearch.
 */
@Component
public class ElasticsearchCatalogIndex implements CatalogIndex {

    private static final int NOT_FOUND = 404;
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchCatalogIndex.class);

    private final ElasticsearchClient client;
    private final IndexMapping mapping;
    private final IndexNames names;
    private final Clock clock;

    /**
     * Crea la puerta de acceso al catálogo.
     *
     * @param client  cliente de Elasticsearch
     * @param mapping mapping empaquetado
     * @param names   patrón de los índices de esta demo
     * @param clock   reloj del plazo de la copia
     */
    public ElasticsearchCatalogIndex(
            ElasticsearchClient client, IndexMapping mapping, IndexNames names, Clock clock) {
        this.client = client;
        this.mapping = mapping;
        this.names = names;
        this.clock = clock;
    }

    @Override
    public CatalogName describe(String name) {
        if (existsAlias(name)) {
            return fromAlias(name);
        }
        if (existsIndex(name)) {
            return new CatalogName.ConcreteIndex(name);
        }
        return new CatalogName.Missing(name);
    }

    @Override
    public void createWithMapping(String indexName) {
        byte[] body = mapping.mappings().getBytes(StandardCharsets.UTF_8);
        call(indexName, () -> {
            client.indices().create(request -> request
                    .index(indexName)
                    .mappings(builder -> builder.withJson(new ByteArrayInputStream(body))));
            return null;
        });
    }

    @Override
    public void reindex(String source, String target, BooleanSupplier stop) {
        call(source, () -> {
            new ReindexTasks(client, clock).copy(source, target, stop);
            return null;
        });
    }

    @Override
    public void refresh(String name) {
        call(name, () -> {
            client.indices().refresh(request -> request.index(name));
            return null;
        });
    }

    @Override
    public long count(String name) {
        return call(name, () -> client.count(request -> request.index(name)).count());
    }

    @Override
    public void publish(String alias, String newIndex, String previousIndex) {
        call(alias, () -> {
            client.indices().updateAliases(request -> request.actions(
                    CatalogRequests.add(alias, newIndex), CatalogRequests.remove(previousIndex)));
            return null;
        });
    }

    @Override
    public void deleteIndex(String indexName) {
        call(indexName, () -> {
            client.indices().delete(request -> request.index(indexName));
            return null;
        });
    }

    @Override
    public List<String> listOwnedIndexes(String alias) {
        try {
            String pattern = alias + "-*";
            return client.indices().get(request -> request.index(pattern)).indices().keySet().stream()
                    .filter(index -> names.isOwnedBy(alias, index))
                    .sorted()
                    .toList();
        } catch (ElasticsearchException exception) {
            if (exception.status() == NOT_FOUND) {
                return List.of();
            }
            throw failure(alias, exception);
        } catch (IOException exception) {
            throw failure(alias, exception);
        }
    }

    @Override
    public List<Movie> readWithVectors(String name, List<String> ids) {
        return search(name, ids.size(), query -> query.ids(idsQuery -> idsQuery.values(ids)));
    }

    @Override
    public List<Movie> readAllWithVectors(String name, int limit) {
        return search(name, limit, query -> query.matchAll(match -> match));
    }

    @Override
    public Set<String> existingIds(String name, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return call(name, () -> CatalogRequests.existingIds(client, name, ids));
    }

    @Override
    public void writeAll(String indexName, List<Movie> movies) {
        if (movies.isEmpty()) {
            return;
        }
        BulkResponse response = call(indexName, () -> client.bulk(request -> request
                .index(indexName)
                .operations(CatalogRequests.operations(movies))));
        CatalogRequests.requireAccepted(indexName, response);
    }

    @Override
    public void indexOne(String name, Movie movie) {
        call(name, () -> {
            client.index(request -> request
                    .index(name)
                    .id(movie.id())
                    .document(movie)
                    .refresh(Refresh.WaitFor));
            return null;
        });
    }

    private CatalogName fromAlias(String name) {
        List<String> targets = call(name, () -> List.copyOf(
                client.indices().getAlias(request -> request.name(name)).aliases().keySet()));
        if (targets.size() > 1) {
            return new CatalogName.Ambiguous(name);
        }
        if (targets.isEmpty()) {
            return new CatalogName.Missing(name);
        }
        return new CatalogName.Alias(name, targets.getFirst());
    }

    private boolean existsAlias(String name) {
        return call(name, () -> client.indices().existsAlias(request -> request.name(name)).value());
    }

    private boolean existsIndex(String name) {
        return call(name, () -> client.indices().exists(request -> request.index(name)).value());
    }

    private List<Movie> search(String name, int size,
            Function<Query.Builder, ObjectBuilder<Query>> query) {
        SearchResponse<Movie> response = call(name, () -> client.search(request -> request
                .index(name)
                .size(size)
                .query(query)
                .source(CatalogRequests.withVectors()), Movie.class));
        return sources(response);
    }

    private static List<Movie> sources(SearchResponse<Movie> response) {
        List<Movie> movies = new ArrayList<>();
        for (Hit<Movie> hit : response.hits().hits()) {
            if (hit.source() != null) {
                movies.add(hit.source());
            }
        }
        return movies;
    }

    private <T> T call(String target, IoSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IOException | ElasticsearchException exception) {
            throw failure(target, exception);
        }
    }

    private ElasticsearchAccessException failure(String target, Exception cause) {
        int status = cause instanceof ElasticsearchException elastic ? elastic.status() : 0;
        ErrorCode code = ElasticsearchDiagnostics.codeFor(status);
        LOG.error("{} en {} tipo={}", code.code(), target, ElasticsearchDiagnostics.typeOf(cause));
        return new ElasticsearchAccessException(code, cause);
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }
}
