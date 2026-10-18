package com.bigfito.elastiflix.search.strategy;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.transport.SearchTransport;

import co.elastic.clients.elasticsearch.core.SearchResponse;

import org.springframework.stereotype.Component;

/**
 * Busca por palabras con BM25, página de Elasticsearch y agregaciones para las facetas.
 */
@Component
public class FullTextSearchStrategy implements SearchStrategy {

    private final FullTextRequestBuilder requests;
    private final SearchTransport transport;
    private final ElastiflixProperties properties;

    /**
     * Crea la estrategia full-text.
     *
     * @param requests   petición de {@code _search}
     * @param transport  quien llama a Elasticsearch
     * @param properties alias configurado; esta estrategia solo lo lee
     */
    public FullTextSearchStrategy(FullTextRequestBuilder requests, SearchTransport transport,
            ElastiflixProperties properties) {
        this.requests = requests;
        this.transport = transport;
        this.properties = properties;
    }

    @Override
    public SearchMode mode() {
        return SearchMode.FULL_TEXT;
    }

    @Override
    public StrategyResult search(SearchQuery query) {
        SearchResponse<Movie> response = transport.search(
                requests.build(properties.index().alias(), query), Movie.class);
        return new StrategyResult(HitReader.hits(response), HitReader.total(response),
                HitReader.aggregations(response));
    }

    @Override
    public long foundWithoutFilters(SearchQuery query) {
        return transport.count(requests.count(properties.index().alias(), query.withoutFilters()));
    }
}
