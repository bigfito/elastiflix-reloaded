package com.bigfito.elastiflix.search.filter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.search.query.SearchFilters;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.DateRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import org.springframework.stereotype.Component;

/**
 * Traduce los filtros de la consulta a cláusulas que no cambian la puntuación.
 *
 * <p>Entre filtros distintos hay un AND, porque cada cláusula va en {@code bool.filter}.
 * Dentro de un filtro basta un valor: se usa {@code terms}. Un filtro vacío no genera cláusula.</p>
 */
@Component
public class FilterQueryBuilder {

    /**
     * Arma las cláusulas en el orden adulto, géneros, idiomas, clasificación y fecha.
     *
     * @param filters filtros de la consulta; una lista vacía no aporta cláusula
     * @return cláusulas listas para {@code bool.filter} o para el {@code filter} de un retriever
     */
    public List<Query> clauses(SearchFilters filters) {
        List<Query> clauses = new ArrayList<>();
        addAdult(clauses, filters.adult());
        addTerms(clauses, CatalogFields.GENRES, filters.genres());
        addTerms(clauses, CatalogFields.SPOKEN_LANGUAGES, filters.spokenLanguages());
        addTerms(clauses, CatalogFields.RATING, filters.ratings());
        addReleaseDate(clauses, filters.from(), filters.to());
        return List.copyOf(clauses);
    }

    private static void addAdult(List<Query> clauses, List<Boolean> adult) {
        if (adult.isEmpty()) {
            return;
        }
        if (adult.size() == 1) {
            clauses.add(term(adult.getFirst()));
            return;
        }
        clauses.add(terms(CatalogFields.ADULT, fieldValues(adult)));
    }

    private static void addTerms(List<Query> clauses, String field, List<String> values) {
        List<FieldValue> present = texts(values);
        if (present.isEmpty()) {
            return;
        }
        clauses.add(terms(field, present));
    }

    private static void addReleaseDate(List<Query> clauses, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return;
        }
        clauses.add(Query.of(query -> query.range(range -> range.date(date -> dateRange(date, from, to)))));
    }

    private static DateRangeQuery.Builder dateRange(DateRangeQuery.Builder date, LocalDate from, LocalDate to) {
        date.field(CatalogFields.RELEASE_DATE).format(CatalogFields.DATE_FORMAT);
        if (from != null) {
            date.gte(from.toString());
        }
        if (to != null) {
            date.lte(to.toString());
        }
        return date;
    }

    private static Query term(boolean adult) {
        return Query.of(query -> query.term(match -> match.field(CatalogFields.ADULT).value(adult)));
    }

    private static Query terms(String field, List<FieldValue> values) {
        return Query.of(query -> query.terms(terms -> terms.field(field).terms(bucket -> bucket.value(values))));
    }

    private static List<FieldValue> fieldValues(List<Boolean> adult) {
        List<FieldValue> values = new ArrayList<>();
        for (Boolean value : adult) {
            values.add(FieldValue.of(value));
        }
        return values;
    }

    private static List<FieldValue> texts(List<String> values) {
        List<FieldValue> present = new ArrayList<>();
        for (String value : values) {
            if (!value.isBlank()) {
                present.add(FieldValue.of(value));
            }
        }
        return present;
    }
}
