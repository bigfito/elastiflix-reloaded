package com.bigfito.elastiflix.search.strategy;

import java.util.List;
import java.util.Map;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.embedding.EmbeddingService;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.transport.SearchTransport;

import co.elastic.clients.elasticsearch.core.SearchResponse;

import org.springframework.stereotype.Component;

/**
 * Busca por cercanía de la sinopsis. Pide {@code k} películas y no pagina en Elasticsearch.
 */
@Component
public class VectorSearchStrategy implements SearchStrategy {

    private final EmbeddingService embeddings;
    private final VectorRequestBuilder requests;
    private final SearchTransport transport;
    private final ElastiflixProperties properties;

    /**
     * Crea la estrategia vectorial.
     *
     * @param embeddings servicio de inferencia de la consulta
     * @param requests   petición k-NN
     * @param transport  quien llama a Elasticsearch
     * @param properties alias configurado; esta estrategia solo lo lee
     */
    public VectorSearchStrategy(EmbeddingService embeddings, VectorRequestBuilder requests,
            SearchTransport transport, ElastiflixProperties properties) {
        this.embeddings = embeddings;
        this.requests = requests;
        this.transport = transport;
        this.properties = properties;
    }

    @Override
    public SearchMode mode() {
        return SearchMode.VECTOR;
    }

    @Override
    public StrategyResult search(SearchQuery query) {
        List<RankedHit> hits = HitReader.hits(execute(query, query.knn().k()));
        return new StrategyResult(hits, hits.size(), Map.of());
    }

    @Override
    public long foundWithoutFilters(SearchQuery query) {
        SearchQuery open = query.withoutFilters();
        return HitReader.hits(execute(open, 1)).size();
    }

    private SearchResponse<Movie> execute(SearchQuery query, int size) {
        float[] vector = QueryVector.forField(embeddings.embedQuery(query.text()), query.knn().field());
        return transport.search(
                requests.build(properties.index().alias(), query, vector, null, size), Movie.class);
    }
}
