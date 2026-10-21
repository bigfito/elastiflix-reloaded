package com.bigfito.elastiflix.core.catalog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
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
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonpMappingException;
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

    /**
     * Tope de {@code _aliases}, por debajo del socket-timeout de 30 s del cliente. Solo {@code timeout}:
     * Serverless rechaza {@code master_timeout} («not permitted when running in serverless mode»).
     */
    private static final String PUBLISH_LIMIT = "10s";
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
            client.indices().updateAliases(request -> request
                    .timeout(limit -> limit.time(PUBLISH_LIMIT))
                    .actions(CatalogRequests.add(alias, newIndex), CatalogRequests.remove(previousIndex)));
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
                    .refresh(Refresh.WaitFor)
                    .requireAlias(true));
            return null;
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Un documento inexistente llega como {@code found=false} y da vacío. Cualquier error de
     * Elasticsearch, incluido un 404 por un alias o índice inexistente ({@code index_not_found_exception}),
     * es una falla del catálogo: la web muestra la página de error EFX, no «película no encontrada».</p>
     */
    @Override
    public Optional<Movie> findById(String name, String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        GetResponse<Movie> response = call(name, () -> client.get(request -> request
                .index(name)
                .id(id)
                .sourceExcludes("plot_256", "plot_512", "plot_1024"),
                Movie.class));
        return response.found() ? Optional.ofNullable(response.source()) : Optional.empty();
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
        return CatalogRequests.sources(response);
    }

    private <T> T call(String target, IoSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IOException | ElasticsearchException | JsonpMappingException exception) {
            // JsonpMappingException: llegó una respuesta que no se pudo leer. No es un rechazo 4xx.
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
