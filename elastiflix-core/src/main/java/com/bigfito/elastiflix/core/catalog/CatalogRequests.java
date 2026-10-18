package com.bigfito.elastiflix.core.catalog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.core.model.Movie;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Arma las operaciones de escritura que {@link ElasticsearchCatalogIndex} envía.
 */
final class CatalogRequests {

    private static final int ID_BATCH = 500;
    private static final Logger LOG = LoggerFactory.getLogger(CatalogRequests.class);

    private CatalogRequests() {
    }

    static Action add(String alias, String index) {
        return Action.of(action -> action.add(add -> add.alias(alias).index(index)));
    }

    static Action remove(String index) {
        return Action.of(action -> action.removeIndex(remove -> remove.index(index)));
    }

    static SourceConfig withVectors() {
        return SourceConfig.of(source -> source.filter(filter -> filter.excludeVectors(false)));
    }

    static List<BulkOperation> operations(List<Movie> movies) {
        List<BulkOperation> operations = new ArrayList<>();
        for (Movie movie : movies) {
            operations.add(operation(movie));
        }
        return operations;
    }

    static void requireAccepted(String indexName, BulkResponse response) {
        if (!response.errors()) {
            return;
        }
        BulkResponseItem failed = firstFailure(response);
        LOG.error("{} en {} id={} tipo={}", ErrorCode.BULK_REJECTED.code(), indexName,
                failed == null ? "" : failed.id(), errorType(failed));
        throw new ImportException(ErrorCode.BULK_REJECTED);
    }

    private static BulkResponseItem firstFailure(BulkResponse response) {
        for (BulkResponseItem item : response.items()) {
            if (item.error() != null) {
                return item;
            }
        }
        return null;
    }

    private static String errorType(BulkResponseItem item) {
        if (item == null || item.error() == null || item.error().type() == null) {
            return "";
        }
        return item.error().type();
    }

    static Set<String> existingIds(ElasticsearchClient client, String name, List<String> ids)
            throws IOException {
        Set<String> found = new HashSet<>();
        for (int start = 0; start < ids.size(); start += ID_BATCH) {
            int end = Math.min(start + ID_BATCH, ids.size());
            found.addAll(searchIds(client, name, ids.subList(start, end)));
        }
        return Set.copyOf(found);
    }

    private static BulkOperation operation(Movie movie) {
        return BulkOperation.of(operation -> operation.index(index -> index
                .id(movie.id())
                .document(movie)));
    }

    private static Set<String> searchIds(ElasticsearchClient client, String name, List<String> ids)
            throws IOException {
        SearchResponse<Void> response = client.search(request -> request
                .index(name)
                .size(ids.size())
                .source(source -> source.fetch(false))
                .query(query -> query.ids(values -> values.values(List.copyOf(ids)))), Void.class);
        return hitIds(response);
    }

    private static Set<String> hitIds(SearchResponse<Void> response) {
        Set<String> found = new HashSet<>();
        for (Hit<Void> hit : response.hits().hits()) {
            if (hit.id() != null) {
                found.add(hit.id());
            }
        }
        return found;
    }
}
