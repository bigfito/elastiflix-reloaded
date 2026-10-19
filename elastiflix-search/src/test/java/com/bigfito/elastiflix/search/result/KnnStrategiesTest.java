package com.bigfito.elastiflix.search.result;

import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.embedding.EmbeddingService;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.search.facet.FacetCalculator;
import com.bigfito.elastiflix.search.filter.FilterQueryBuilder;
import com.bigfito.elastiflix.search.page.Paginator;
import com.bigfito.elastiflix.search.query.KnnSettings;
import com.bigfito.elastiflix.search.query.MatchType;
import com.bigfito.elastiflix.search.query.FullTextSettings;
import com.bigfito.elastiflix.search.query.SearchFilters;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.query.TextField;
import com.bigfito.elastiflix.search.query.TextOperator;
import com.bigfito.elastiflix.search.query.VectorField;
import com.bigfito.elastiflix.search.strategy.HybridRequestBuilder;
import com.bigfito.elastiflix.search.strategy.HybridSearchStrategy;
import com.bigfito.elastiflix.search.strategy.LexicalQueryBuilder;
import com.bigfito.elastiflix.search.strategy.ScoreJoiner;
import com.bigfito.elastiflix.search.strategy.VectorRequestBuilder;
import com.bigfito.elastiflix.search.strategy.VectorSearchStrategy;
import com.bigfito.elastiflix.search.transport.SearchTransport;

import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.MsearchResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchItem;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Las estrategias vectorial e híbrida de verdad, con el transporte y la inferencia simulados.
 */
class KnnStrategiesTest {

    private final EmbeddingService embeddings = mock(EmbeddingService.class);
    private final SearchTransport transport = mock(SearchTransport.class);
    private final CatalogIndex catalog = mock(CatalogIndex.class);
    private final ElastiflixProperties properties = new ElastiflixProperties(null, null, null, null, null, null, null);
    private final VectorRequestBuilder vectors = new VectorRequestBuilder(new FilterQueryBuilder());
    private final SearchService service = new SearchService(List.of(
            new VectorSearchStrategy(embeddings, vectors, transport, properties),
            new HybridSearchStrategy(embeddings, new HybridRequestBuilder(
                    new LexicalQueryBuilder(new FilterQueryBuilder()), new FilterQueryBuilder(), vectors),
                    new ScoreJoiner(), transport, properties)),
            new FacetCalculator(), new Paginator(), catalog, properties);

    @Test
    @DisplayName("vectorial vacía con filtros confirma sin filtros y vectoriza la consulta una sola vez")
    void shouldEmbedTheVectorQueryOnceWhenConfirmingTheFilteredEmptyState() {
        when(embeddings.embedQuery(anyString())).thenReturn(unit());
        when(transport.search(any(SearchRequest.class), eq(Movie.class))).thenReturn(response(0), response(1));

        SearchResult result = service.search(query(SearchMode.VECTOR, drama()));

        assertThat(result.empty()).isNotNull();
        assertThat(result.empty().title()).isEqualTo("Ningún resultado cumple los filtros");
        verify(transport, times(2)).search(any(SearchRequest.class), eq(Movie.class));
        verify(embeddings, times(1)).embedQuery(anyString());
    }

    @Test
    @DisplayName("híbrida vacía con filtros confirma sin filtros y vectoriza la consulta una sola vez")
    void shouldEmbedTheHybridQueryOnceWhenConfirmingTheFilteredEmptyState() {
        when(embeddings.embedQuery(anyString())).thenReturn(unit());
        when(transport.msearch(any(MsearchRequest.class), eq(Movie.class)))
                .thenReturn(msearch(ok(0), ok(0), ok(0)), msearch(ok(1), ok(1), ok(1)));

        SearchResult result = service.search(query(SearchMode.HYBRID, drama()));

        assertThat(result.empty().title()).isEqualTo("Ningún resultado cumple los filtros");
        verify(transport, times(2)).msearch(any(MsearchRequest.class), eq(Movie.class));
        verify(embeddings, times(1)).embedQuery(anyString());
    }

    @Test
    @DisplayName("un ítem fallido del _msearch no se toma como lista vacía: lanza")
    void shouldFailWhenAPartOfTheMultiSearchFails() {
        when(embeddings.embedQuery(anyString())).thenReturn(unit());
        MultiSearchResponseItem<Movie> failed = failure(500, "search_phase_execution_exception");
        when(transport.msearch(any(MsearchRequest.class), eq(Movie.class))).thenReturn(msearch(ok(1), failed, ok(1)));

        assertThatThrownBy(() -> service.search(query(SearchMode.HYBRID, SearchFilters.none())))
                .isInstanceOf(ElasticsearchAccessException.class)
                .extracting(thrown -> ((ElasticsearchAccessException) thrown).errorCode())
                .isEqualTo(ErrorCode.ELASTICSEARCH_UNAVAILABLE);
    }

    @Test
    @DisplayName("un ítem del _msearch rechazado con 4xx da EFX-ES-007, no «sin conexión»")
    void shouldGiveTheRejectedCodeWhenAPartOfTheMultiSearchIsAClientError() {
        // given
        when(embeddings.embedQuery(anyString())).thenReturn(unit());
        MultiSearchResponseItem<Movie> rejected = failure(400, "illegal_argument_exception");
        when(transport.msearch(any(MsearchRequest.class), eq(Movie.class)))
                .thenReturn(msearch(ok(1), ok(1), rejected));

        // when
        var thrown = catchThrowableOfType(ElasticsearchAccessException.class,
                () -> service.search(query(SearchMode.HYBRID, SearchFilters.none())));

        // then
        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.ELASTICSEARCH_REJECTED);
    }

    @Test
    @DisplayName("un _msearch que no trae exactamente tres partes da EFX-ES-007, no «sin conexión»")
    void shouldFailWhenTheMultiSearchHasTheWrongNumberOfParts() {
        // given
        when(embeddings.embedQuery(anyString())).thenReturn(unit());
        when(transport.msearch(any(MsearchRequest.class), eq(Movie.class))).thenReturn(msearch(ok(1), ok(1)));

        // when
        var thrown = catchThrowableOfType(ElasticsearchAccessException.class,
                () -> service.search(query(SearchMode.HYBRID, SearchFilters.none())));

        // then
        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.ELASTICSEARCH_REJECTED);
    }

    private static SearchQuery query(SearchMode mode, SearchFilters filters) {
        FullTextSettings lexical = mode == SearchMode.HYBRID
                ? new FullTextSettings(MatchType.MATCH, TextField.TITLE, TextOperator.OR) : null;
        return new SearchQuery("un barco", mode, lexical, new KnnSettings(VectorField.PLOT_256, 100, 200), filters,
                1, SearchQuery.PAGE_25);
    }

    private static SearchFilters drama() {
        return new SearchFilters(List.of(), List.of("Drama"), List.of(), List.of(), null, null);
    }

    private static float[] unit() {
        float[] vector = new float[1024];
        vector[0] = 1f;
        return vector;
    }

    private static SearchResponse<Movie> response(int count) {
        return SearchResponse.of(response -> response.took(1).timedOut(false)
                .shards(shards -> shards.total(1).successful(1).failed(0))
                .hits(hits -> hits.total(total -> total.value(count).relation(TotalHitsRelation.Eq))
                        .hits(hits(count))));
    }

    private static MultiSearchResponseItem<Movie> failure(int status, String type) {
        return MultiSearchResponseItem.of(item -> item.failure(ErrorResponse.of(
                error -> error.status(status).error(cause -> cause.type(type).reason("x")))));
    }

    private static MultiSearchResponseItem<Movie> ok(int count) {
        return MultiSearchResponseItem.of(item -> item.result(MultiSearchItem.of(result -> result.took(1)
                .timedOut(false).status(200)
                .shards(shards -> shards.total(1).successful(1).failed(0))
                .hits(hits -> hits.total(total -> total.value(count).relation(TotalHitsRelation.Eq))
                        .hits(hits(count))))));
    }

    @SafeVarargs
    private static MsearchResponse<Movie> msearch(MultiSearchResponseItem<Movie>... parts) {
        return MsearchResponse.of(response -> response.took(1).responses(List.of(parts)));
    }

    private static List<Hit<Movie>> hits(int count) {
        List<Hit<Movie>> hits = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String id = Integer.toString(index + 1);
            Movie movie = MovieJsonMapper.create().readValue(
                    "{\"id\":\"" + id + "\",\"title\":\"T\",\"plot\":\"p\"}", Movie.class);
            hits.add(Hit.of(hit -> hit.index("i").id(id).score(0.9).source(movie)));
        }
        return hits;
    }
}
