package com.bigfito.elastiflix.search.strategy;

import java.util.List;
import java.util.Map;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.embedding.EmbeddingService;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.transport.SearchTransport;

import co.elastic.clients.elasticsearch.core.MsearchResponse;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchItem;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fusiona la lista léxica y la k-NN con RRF, y guarda el puesto de cada película en las dos.
 */
@Component
public class HybridSearchStrategy implements SearchStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(HybridSearchStrategy.class);
    private static final int FUSED = 0;
    private static final int LEXICAL = 1;
    private static final int VECTOR = 2;
    private static final int PARTS = 3;

    private final EmbeddingService embeddings;
    private final HybridRequestBuilder requests;
    private final ScoreJoiner joiner;
    private final SearchTransport transport;
    private final ElastiflixProperties properties;

    /**
     * Crea la estrategia híbrida.
     *
     * @param embeddings servicio de inferencia de la consulta
     * @param requests   las tres partes del {@code _msearch}
     * @param joiner     quién copia BM25, similitud y posiciones
     * @param transport  quien llama a Elasticsearch
     * @param properties alias configurado; esta estrategia solo lo lee
     */
    public HybridSearchStrategy(EmbeddingService embeddings, HybridRequestBuilder requests, ScoreJoiner joiner,
            SearchTransport transport, ElastiflixProperties properties) {
        this.embeddings = embeddings;
        this.requests = requests;
        this.joiner = joiner;
        this.transport = transport;
        this.properties = properties;
    }

    @Override
    public SearchMode mode() {
        return SearchMode.HYBRID;
    }

    @Override
    public StrategyResult search(SearchQuery query) {
        float[] vector = QueryVector.forField(embeddings.embedQuery(query.text()), query.knn().field());
        MsearchResponse<Movie> response = transport.msearch(
                requests.build(properties.index().alias(), query, vector), Movie.class);
        List<RankedHit> joined = join(query, response);
        return new StrategyResult(joined, joined.size(), Map.of());
    }

    private List<RankedHit> join(SearchQuery query, MsearchResponse<Movie> response) {
        List<MultiSearchResponseItem<Movie>> parts = response.responses();
        if (parts.size() != PARTS) {
            throw unavailable();
        }
        List<RankedHit> fused = HitReader.hits(part(parts.get(FUSED)));
        List<RankedHit> lexical = HitReader.hits(part(parts.get(LEXICAL)));
        List<RankedHit> vector = HitReader.hits(part(parts.get(VECTOR)));
        return joiner.join(fused, lexical, vector, query.knn().k());
    }

    private MultiSearchItem<Movie> part(MultiSearchResponseItem<Movie> item) {
        if (item.isFailure()) {
            throw unavailable();
        }
        return item.result();
    }

    private ElasticsearchAccessException unavailable() {
        LOG.error("EFX-ES-001 en {}", properties.index().alias());
        return new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE);
    }
}
