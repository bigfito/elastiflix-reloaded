package com.bigfito.elastiflix.search.strategy;

import com.bigfito.elastiflix.search.filter.CatalogFields;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.query.SearchQueryValidator;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;

import org.springframework.stereotype.Component;

/**
 * Arma el {@code _search} de full-text: página, total exacto, filtros y agregaciones.
 */
@Component
public class FullTextRequestBuilder {

    private final LexicalQueryBuilder lexical;

    /**
     * Crea el armador de la petición full-text.
     *
     * @param lexical consulta de palabras y filtros
     */
    public FullTextRequestBuilder(LexicalQueryBuilder lexical) {
        this.lexical = lexical;
    }

    /**
     * Petición contra el alias. No incluye vectores.
     *
     * @param alias nombre configurado del catálogo
     * @param query consulta ya validada
     * @return petición de {@code _search}
     */
    public SearchRequest build(String alias, SearchQuery query) {
        return SearchRequest.of(request -> aggregations(base(request, alias, query)));
    }

    /**
     * Conteo de la misma consulta, sin página ni agregaciones.
     *
     * @param alias nombre configurado del catálogo
     * @param query consulta ya validada; quien confirma un vacío le quita los filtros
     * @return petición de {@code _count}
     */
    public CountRequest count(String alias, SearchQuery query) {
        return CountRequest.of(request -> request.index(alias).query(lexical.withFilters(query)));
    }

    private SearchRequest.Builder base(SearchRequest.Builder request, String alias, SearchQuery query) {
        return request
                .index(alias)
                .from(query.from())
                .size(requestedSize(query))
                .trackTotalHits(track -> track.enabled(true))
                .source(SearchSource.withoutVectors())
                .query(lexical.withFilters(query));
    }

    private static SearchRequest.Builder aggregations(SearchRequest.Builder request) {
        request.aggregations(CatalogFields.GENRES, terms(CatalogFields.GENRES));
        request.aggregations(CatalogFields.SPOKEN_LANGUAGES, terms(CatalogFields.SPOKEN_LANGUAGES));
        request.aggregations(CatalogFields.RATING, terms(CatalogFields.RATING));
        request.aggregations(CatalogFields.ADULT, terms(CatalogFields.ADULT));
        request.aggregations(CatalogFields.RELEASE_DATE_MIN, minimum());
        request.aggregations(CatalogFields.RELEASE_DATE_MAX, maximum());
        return request;
    }

    private static Aggregation terms(String field) {
        return Aggregation.of(aggregation -> aggregation.terms(terms -> terms
                .field(field)
                .size(CatalogFields.TERMS_AGGREGATION_SIZE)));
    }

    private static int requestedSize(SearchQuery query) {
        int remaining = SearchQueryValidator.MAX_WINDOW - query.from();
        if (remaining < query.pageSize()) {
            return Math.max(remaining, 0);
        }
        return query.pageSize();
    }

    private static Aggregation minimum() {
        return Aggregation.of(aggregation -> aggregation.min(metric -> metric.field(CatalogFields.RELEASE_DATE)));
    }

    private static Aggregation maximum() {
        return Aggregation.of(aggregation -> aggregation.max(metric -> metric.field(CatalogFields.RELEASE_DATE)));
    }
}
