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
        return build(alias, query, vector, null, query.knn().k());
    }

    /**
     * Igual que {@link #build(String, SearchQuery, float[])}, con la misma {@code preference}
     * que las otras partes de un {@code _msearch}.
     *
     * @param alias      nombre configurado del catálogo
     * @param query      consulta ya validada
     * @param vector     vector de consulta, ya recortado al campo
     * @param preference valor compartido, o {@code null} si la búsqueda es sola
     * @return petición de {@code _search}
     */
    public SearchRequest build(String alias, SearchQuery query, float[] vector, String preference) {
        return build(alias, query, vector, preference, query.knn().k());
    }

    /**
     * Igual que {@link #build(String, SearchQuery, float[], String)}, con otro {@code size}.
     *
     * <p>La confirmación de un resultado vacío pide un solo documento. {@code k} no cambia.</p>
     *
     * @param alias      nombre configurado del catálogo
     * @param query      consulta ya validada
     * @param vector     vector de consulta, ya recortado al campo
     * @param preference valor compartido, o {@code null} si la búsqueda es sola
     * @param size       cuántos documentos devolver
     * @return petición de {@code _search}
     */
    public SearchRequest build(String alias, SearchQuery query, float[] vector, String preference, int size) {
        KnnSettings knn = query.knn();
        Retriever retriever = retriever(knn, vector, filters.clauses(query.filters()));
        return SearchRequest.of(request -> withPreference(base(request, alias, size, retriever), preference));
    }

    private static SearchRequest.Builder base(SearchRequest.Builder request, String alias, int size,
            Retriever retriever) {
        return request.index(alias).size(size).source(SearchSource.withoutVectors()).retriever(retriever);
    }

    private static SearchRequest.Builder withPreference(SearchRequest.Builder request, String preference) {
        if (preference != null) {
            request.preference(preference);
        }
        return request;
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
