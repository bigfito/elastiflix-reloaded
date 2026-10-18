package com.bigfito.elastiflix.search.transport;

import java.io.IOException;
import java.util.List;

import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.MsearchResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Traduce {@link SearchTransport} a las llamadas del cliente Java de Elasticsearch.
 */
@Component
public class ElasticsearchSearchTransport implements SearchTransport {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchSearchTransport.class);

    private final ElasticsearchClient client;

    /**
     * Crea la puerta de acceso a las búsquedas.
     *
     * @param client cliente que ya configuró Spring
     */
    public ElasticsearchSearchTransport(ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public <T> SearchResponse<T> search(SearchRequest request, Class<T> documentType) {
        return call(request.index(), () -> client.search(request, documentType));
    }

    @Override
    public <T> MsearchResponse<T> msearch(MsearchRequest request, Class<T> documentType) {
        return call(request.index(), () -> client.msearch(request, documentType));
    }

    @Override
    public long count(CountRequest request) {
        return call(request.index(), () -> client.count(request).count());
    }

    private <T> T call(List<String> index, IoCall<T> call) {
        try {
            return call.get();
        } catch (IOException | ElasticsearchException exception) {
            throw unavailable(index, exception);
        }
    }

    private static ElasticsearchAccessException unavailable(List<String> index, Exception cause) {
        LOG.error("EFX-ES-001 en {}", alias(index));
        return new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE, cause);
    }

    private static String alias(List<String> index) {
        if (index == null || index.isEmpty()) {
            return "";
        }
        return index.getFirst();
    }

    @FunctionalInterface
    private interface IoCall<T> {
        T get() throws IOException;
    }
}
