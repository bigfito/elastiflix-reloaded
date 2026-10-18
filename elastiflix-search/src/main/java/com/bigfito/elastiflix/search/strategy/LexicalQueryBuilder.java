package com.bigfito.elastiflix.search.strategy;

import java.util.List;

import com.bigfito.elastiflix.search.filter.FilterQueryBuilder;
import com.bigfito.elastiflix.search.query.FullTextSettings;
import com.bigfito.elastiflix.search.query.MatchType;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.query.TextField;
import com.bigfito.elastiflix.search.query.TextOperator;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;

import org.springframework.stereotype.Component;

/**
 * Arma la consulta de palabras: {@code match} de un título o {@code multi_match} de los dos.
 */
@Component
public class LexicalQueryBuilder {

    private final FilterQueryBuilder filters;

    /**
     * Crea el armador de la consulta léxica.
     *
     * @param filters cláusulas que no deben cambiar la puntuación
     */
    public LexicalQueryBuilder(FilterQueryBuilder filters) {
        this.filters = filters;
    }

    /**
     * La consulta de palabras, sin filtros. La híbrida la mete dentro del retriever.
     *
     * @param query consulta ya validada
     * @return {@code match} o {@code multi_match}
     */
    public Query text(SearchQuery query) {
        FullTextSettings settings = query.fullText();
        if (settings.type() == MatchType.MULTI_MATCH) {
            return multiMatch(query.text(), settings.operator());
        }
        return match(query.text(), settings.field(), settings.operator());
    }

    /**
     * La misma consulta dentro de {@code bool}, con los filtros en {@code filter}.
     *
     * @param query consulta ya validada
     * @return {@code bool.must} y, si hay filtros, {@code bool.filter}
     */
    public Query withFilters(SearchQuery query) {
        List<Query> clauses = filters.clauses(query.filters());
        Query text = text(query);
        if (clauses.isEmpty()) {
            return Query.of(root -> root.bool(bool -> bool.must(text)));
        }
        return Query.of(root -> root.bool(bool -> bool.must(text).filter(clauses)));
    }

    private static Query match(String text, TextField field, TextOperator operator) {
        return Query.of(root -> root.match(match -> match
                .field(field.fieldName())
                .query(text)
                .operator(elasticsearch(operator))));
    }

    private static Query multiMatch(String text, TextOperator operator) {
        return Query.of(root -> root.multiMatch(match -> match
                .query(text)
                .fields(TextField.TITLE.fieldName(), TextField.ORIGINAL_TITLE.fieldName())
                .type(TextQueryType.BestFields)
                .operator(elasticsearch(operator))));
    }

    private static Operator elasticsearch(TextOperator operator) {
        if (operator == TextOperator.AND) {
            return Operator.And;
        }
        return Operator.Or;
    }
}
