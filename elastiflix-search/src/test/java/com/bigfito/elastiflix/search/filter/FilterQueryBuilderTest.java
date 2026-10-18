package com.bigfito.elastiflix.search.filter;

import java.time.LocalDate;
import java.util.List;

import com.bigfito.elastiflix.search.JsonDocuments;
import com.bigfito.elastiflix.search.query.SearchFilters;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El JSON de cada filtro y el de una combinación.
 */
class FilterQueryBuilderTest {

    private final FilterQueryBuilder builder = new FilterQueryBuilder();

    @Test
    @DisplayName("un solo valor de adulto es un term")
    void shouldWriteATermWhenAdultHasOneValue() {
        assertThat(json(filters(List.of(false), List.of(), List.of(), List.of(), null, null)))
                .isEqualTo("{\"term\":{\"adult\":{\"value\":false}}}");
    }

    @Test
    @DisplayName("los dos valores de adulto son un terms")
    void shouldWriteTermsWhenAdultHasBothValues() {
        assertThat(json(filters(List.of(false, true), List.of(), List.of(), List.of(), null, null)))
                .isEqualTo("{\"terms\":{\"adult\":[false,true]}}");
    }

    @Test
    @DisplayName("los géneros van en terms y se ignoran los vacíos")
    void shouldWriteGenresWhenTheListHasText() {
        assertThat(json(filters(List.of(), List.of("Drama", "  "), List.of(), List.of(), null, null)))
                .isEqualTo("{\"terms\":{\"genres\":[\"Drama\"]}}");
    }

    @Test
    @DisplayName("los idiomas van en terms")
    void shouldWriteSpokenLanguagesWhenTheListHasText() {
        assertThat(json(filters(List.of(), List.of(), List.of("en", "es"), List.of(), null, null)))
                .isEqualTo("{\"terms\":{\"spoken_languages\":[\"en\",\"es\"]}}");
    }

    @Test
    @DisplayName("las clasificaciones van en terms")
    void shouldWriteRatingsWhenTheListHasText() {
        assertThat(json(filters(List.of(), List.of(), List.of(), List.of("PG-13"), null, null)))
                .isEqualTo("{\"terms\":{\"rating\":[\"PG-13\"]}}");
    }

    @Test
    @DisplayName("una sola fecha escribe solo ese límite")
    void shouldWriteOneBoundWhenOnlyOneDateIsPresent() {
        assertThat(json(filters(List.of(), List.of(), List.of(), List.of(),
                LocalDate.parse("1997-01-01"), null)))
                .isEqualTo("{\"range\":{\"release_date\":{\"gte\":\"1997-01-01\",\"format\":\"yyyy-MM-dd\"}}}");
        assertThat(json(filters(List.of(), List.of(), List.of(), List.of(),
                null, LocalDate.parse("1998-12-31"))))
                .isEqualTo("{\"range\":{\"release_date\":{\"lte\":\"1998-12-31\",\"format\":\"yyyy-MM-dd\"}}}");
    }

    @Test
    @DisplayName("el rango de estreno usa gte y lte")
    void shouldWriteBothBoundsWhenTheDateRangeIsComplete() {
        assertThat(json(filters(List.of(), List.of(), List.of(), List.of(),
                LocalDate.parse("1997-01-01"), LocalDate.parse("1998-12-31"))))
                .isEqualTo("{\"range\":{\"release_date\":{\"gte\":\"1997-01-01\",\"lte\":\"1998-12-31\","
                        + "\"format\":\"yyyy-MM-dd\"}}}");
    }

    @Test
    @DisplayName("sin filtros no hay cláusulas")
    void shouldWriteNothingWhenThereAreNoFilters() {
        assertThat(builder.clauses(SearchFilters.none())).isEmpty();
    }

    @Test
    @DisplayName("una combinación conserva el orden de la tabla")
    void shouldKeepTheTableOrderWhenSeveralFiltersAreSet() {
        SearchFilters filters = filters(
                List.of(false),
                List.of("Drama"),
                List.of("en"),
                List.of(),
                LocalDate.parse("1990-01-01"),
                LocalDate.parse("2000-12-31"));
        Query combined = Query.of(query -> query.bool(bool -> bool.filter(builder.clauses(filters))));
        assertThat(JsonDocuments.of(combined)).isEqualTo("{\"bool\":{\"filter\":["
                + "{\"term\":{\"adult\":{\"value\":false}}},"
                + "{\"terms\":{\"genres\":[\"Drama\"]}},"
                + "{\"terms\":{\"spoken_languages\":[\"en\"]}},"
                + "{\"range\":{\"release_date\":{\"gte\":\"1990-01-01\",\"lte\":\"2000-12-31\","
                + "\"format\":\"yyyy-MM-dd\"}}}]}}");
    }

    private String json(SearchFilters filters) {
        List<Query> clauses = builder.clauses(filters);
        assertThat(clauses).hasSize(1);
        return JsonDocuments.of(clauses.getFirst());
    }

    private static SearchFilters filters(List<Boolean> adult, List<String> genres, List<String> languages,
            List<String> ratings, LocalDate from, LocalDate to) {
        return new SearchFilters(adult, genres, languages, ratings, from, to);
    }
}
