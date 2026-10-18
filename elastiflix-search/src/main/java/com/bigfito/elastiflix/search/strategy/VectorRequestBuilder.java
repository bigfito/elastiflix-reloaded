package com.bigfito.elastiflix.search.strategy;

import java.util.List;

import com.bigfito.elastiflix.search.filter.FilterQueryBuilder;
import com.bigfito.elastiflix.search.query.KnnSettings;
import com.bigfito.elastiflix.search.query.SearchQuery;

import co.elastic.clients.elasticsearch._types.KnnRetriever;
import co.elastic.clients.elasticsearch._types.Retriever;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;

import org.springframework.stereotype.Component;

/**
 * Arma el {@code _search} vectorial: retriever {@code knn} y {@code size = k}.
 */
@Component
public class VectorRequestBuilder {

    private final FilterQueryBuilder filters;

    /**
     * Crea el armador de la petición k-NN.
     *
     * @param filters cláusulas del retriever
     */
    public VectorRequestBuilder(FilterQueryBuilder filters) {
        this.filters = filters;
    }

    /**
     * Petición contra el alias. No manda {@code from} ni agregaciones.
     *
     * @param alias  nombre configurado del catálogo
     * @param query  consulta ya validada
     * @param vector vector de consulta, ya recortado al campo
     * @return petición de {@code _search}
     */
    public SearchRequest build(String alias, SearchQuery query, float[] vector) {
        KnnSettings knn = query.knn();
        List<Query> clauses = filters.clauses(query.filters());
        return SearchRequest.of(request -> request
                .index(alias)
                .size(knn.k())
                .source(SearchSource.withoutVectors())
                .retriever(retriever(knn, vector, clauses)));
    }

    private static Retriever retriever(KnnSettings knn, float[] vector, List<Query> clauses) {
        return Retriever.of(root -> root.knn(search -> knnOf(search, knn, vector, clauses)));
    }

    private static KnnRetriever.Builder knnOf(KnnRetriever.Builder search, KnnSettings knn,
            float[] vector, List<Query> clauses) {
        search.field(knn.field().fieldName())
                .queryVector(QueryVector.boxed(vector))
                .k(knn.k())
                .numCandidates(knn.numCandidates());
        if (!clauses.isEmpty()) {
            search.filter(clauses);
        }
        return search;
    }
}
