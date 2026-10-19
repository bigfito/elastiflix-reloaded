package com.bigfito.elastiflix.search.strategy;

import java.util.List;

import com.bigfito.elastiflix.search.filter.FilterQueryBuilder;
import com.bigfito.elastiflix.search.query.KnnSettings;
import com.bigfito.elastiflix.search.query.SearchQuery;

import co.elastic.clients.elasticsearch._types.KnnRetriever;
import co.elastic.clients.elasticsearch._types.Retriever;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;

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
     * Petición contra el alias, sin {@code from} ni agregaciones. {@code size} es {@code k} en la búsqueda y 1
     * al confirmar el estado vacío.
     *
     * <p>La confirmación de un resultado vacío pide un solo documento. {@code k} no cambia.</p>
     *
     * @param alias  nombre configurado del catálogo
     * @param query  consulta ya validada
     * @param vector vector de consulta, ya recortado al campo
     * @param size   cuántos documentos devolver
     * @return petición de {@code _search}
     */
    public SearchRequest build(String alias, SearchQuery query, float[] vector, int size) {
        return search(alias, query, vector, null, new Shape(size, false));
    }

    /**
     * La lista de apoyo de la híbrida: mismo k-NN, sin el documento.
     *
     * @param alias      nombre configurado del catálogo
     * @param query      consulta ya validada
     * @param vector     vector de consulta, ya recortado al campo
     * @param preference valor compartido por las tres búsquedas
     * @param size       cuántos ids devolver
     * @return petición de {@code _search} con {@code _source: false}
     */
    public SearchRequest ids(String alias, SearchQuery query, float[] vector, String preference, int size) {
        return search(alias, query, vector, preference, new Shape(size, true));
    }

    private SearchRequest search(String alias, SearchQuery query, float[] vector, String preference, Shape shape) {
        KnnSettings knn = query.knn();
        Retriever retriever = retriever(knn, vector, filters.clauses(query.filters()));
        return SearchRequest.of(request -> withPreference(
                base(request, alias, shape, retriever), preference));
    }

    private record Shape(int size, boolean idsOnly) {
    }

    private static SearchRequest.Builder base(SearchRequest.Builder request, String alias, Shape shape,
            Retriever retriever) {
        SourceConfig source = shape.idsOnly() ? SearchSource.idsOnly() : SearchSource.withoutVectors();
        return request.index(alias).size(shape.size()).source(source).retriever(retriever);
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
