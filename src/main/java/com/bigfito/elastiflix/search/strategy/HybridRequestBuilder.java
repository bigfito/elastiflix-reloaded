package com.bigfito.elastiflix.search.strategy;

import java.util.List;
import java.util.UUID;

import com.bigfito.elastiflix.search.filter.FilterQueryBuilder;
import com.bigfito.elastiflix.search.query.SearchQuery;

import co.elastic.clients.elasticsearch._types.RRFRetriever;
import co.elastic.clients.elasticsearch._types.RRFRetrieverEntry;
import co.elastic.clients.elasticsearch._types.Retriever;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.msearch.RequestItem;

import org.springframework.stereotype.Component;

/**
 * Arma el {@code _msearch} de la híbrida: RRF, el componente léxico y el k-NN.
 */
@Component
public class HybridRequestBuilder {

    private final LexicalQueryBuilder lexical;
    private final FilterQueryBuilder filters;
    private final VectorRequestBuilder vectors;

    /**
     * Crea el armador de las tres búsquedas.
     *
     * @param lexical consulta de palabras
     * @param filters cláusulas que van en el RRF y en los componentes
     * @param vectors petición k-NN del componente vectorial
     */
    public HybridRequestBuilder(LexicalQueryBuilder lexical, FilterQueryBuilder filters,
            VectorRequestBuilder vectors) {
        this.lexical = lexical;
        this.filters = filters;
        this.vectors = vectors;
    }

    /**
     * Tres búsquedas. {@code rank_window_size} sigue siendo {@code k}.
     *
     * <p>Las tres comparten un {@code preference} nuevo ({@code elastiflix-<uuid>}), para que lean las mismas
     * copias del índice. La confirmación de un vacío usa {@code size} 1.</p>
     *
     * @param alias  nombre configurado del catálogo
     * @param query  consulta ya validada
     * @param vector vector de consulta, ya recortado
     * @param size   {@code size} de las tres búsquedas
     * @return la petición {@code _msearch}
     */
    public MsearchRequest build(String alias, SearchQuery query, float[] vector, int size) {
        String chosen = "elastiflix-" + UUID.randomUUID();
        SearchRequest fused = fused(alias, query, vector, chosen, size);
        SearchRequest words = words(alias, query, chosen, size);
        SearchRequest nearest = vectors.ids(alias, query, vector, chosen, size);
        return MsearchRequest.of(request -> request.searches(
                item(alias, chosen, fused),
                item(alias, chosen, words),
                item(alias, chosen, nearest)));
    }

    private SearchRequest fused(String alias, SearchQuery query, float[] vector, String preference, int size) {
        int k = query.knn().k();
        List<Query> clauses = filters.clauses(query.filters());
        Retriever fusion = Retriever.of(root -> root.rrf(rrf -> rrfOf(rrf, query, vector, clauses, k)));
        return SearchRequest.of(request -> request
                .index(alias)
                .preference(preference)
                .size(size)
                .source(SearchSource.withoutVectors())
                .retriever(fusion));
    }

    private RRFRetriever.Builder rrfOf(RRFRetriever.Builder rrf, SearchQuery query, float[] vector,
            List<Query> clauses, int k) {
        rrf.retrievers(entry(standard(query)), entry(nearest(query, vector)))
                .rankConstant(Rrf.RANK_CONSTANT)
                .rankWindowSize(k);
        if (!clauses.isEmpty()) {
            rrf.filter(clauses);
        }
        return rrf;
    }

    private static RRFRetrieverEntry entry(Retriever retriever) {
        return RRFRetrieverEntry.of(candidate -> candidate.retriever(retriever));
    }

    private Retriever standard(SearchQuery query) {
        return Retriever.of(root -> root.standard(standard -> standard.query(lexical.text(query))));
    }

    private static Retriever nearest(SearchQuery query, float[] vector) {
        return Retriever.of(root -> root.knn(knn -> knn
                .field(query.knn().field().fieldName())
                .queryVector(QueryVector.boxed(vector))
                .k(query.knn().k())
                .numCandidates(query.knn().numCandidates())));
    }

    private SearchRequest words(String alias, SearchQuery query, String preference, int size) {
        return SearchRequest.of(request -> request
                .index(alias)
                .preference(preference)
                .size(size)
                .source(SearchSource.idsOnly())
                .query(lexical.withFilters(query)));
    }

    private static RequestItem item(String alias, String preference, SearchRequest body) {
        return RequestItem.of(item -> item
                .header(header -> header.index(alias).preference(preference))
                .body(body));
    }
}
