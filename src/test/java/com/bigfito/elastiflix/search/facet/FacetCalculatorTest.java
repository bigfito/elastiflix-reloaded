package com.bigfito.elastiflix.search.facet;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.search.filter.CatalogFields;
import com.bigfito.elastiflix.search.query.SearchFilters;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Conteos a mano: orden, un valor marcado en cero y el rango de fechas.
 */
class FacetCalculatorTest {

    private static final LocalDate TITANIC = LocalDate.parse("1997-12-19");
    private static final LocalDate LIFE_OF_PI = LocalDate.parse("2012-11-21");
    /** 2012-01-01 a las 00:00 UTC, por si la agregación no trae texto. */
    private static final double EPOCH_2012_01_01 = 1_325_376_000_000d;

    private final FacetCalculator calculator = new FacetCalculator();

    @Test
    @DisplayName("cuenta cada género, ordena y conserva el valor marcado aunque sea cero")
    void shouldOrderCountsAndKeepASelectedValueWhenMoviesAreBuiltByHand() {
        SearchFilters selected = new SearchFilters(
                List.of(false), List.of("Comedia", " "), List.of("fr"), List.of(), null, null);
        Facets facets = calculator.fromMovies(List.of(
                movie("1", TITANIC, false, "PG-13", List.of("Drama", "Action", " "), List.of("en", "es")),
                movie("2", LIFE_OF_PI, true, "PG-13", List.of("Drama"), List.of("en")),
                movie("3", null, null, " ", null, List.of(""))), selected);

        assertThat(facets.genres()).containsExactly(
                new FacetValue("Drama", 2),
                new FacetValue("Action", 1),
                new FacetValue("Comedia", 0));
        assertThat(facets.spokenLanguages()).containsExactly(
                new FacetValue("en", 2),
                new FacetValue("es", 1),
                new FacetValue("fr", 0));
        assertThat(facets.ratings()).containsExactly(new FacetValue("PG-13", 2));
        assertThat(facets.adult()).containsExactly(
                new FacetValue("false", 1),
                new FacetValue("true", 1));
        assertThat(facets.earliestRelease()).isEqualTo(TITANIC);
        assertThat(facets.latestRelease()).isEqualTo(LIFE_OF_PI);
    }

    @Test
    @DisplayName("sin fechas el rango queda vacío")
    void shouldLeaveTheDateSpanEmptyWhenNoMovieHasAReleaseDate() {
        Facets facets = calculator.fromMovies(
                List.of(movie("1", null, false, null, List.of(), List.of())), SearchFilters.none());

        assertThat(facets.earliestRelease()).isNull();
        assertThat(facets.latestRelease()).isNull();
        assertThat(facets.adult()).containsExactly(new FacetValue("false", 1));
    }

    @Test
    @DisplayName("las agregaciones traen los mismos conteos y el rango de fechas")
    void shouldReadTermsAndDatesWhenTheResponseBringsAggregations() {
        SearchFilters selected = new SearchFilters(
                List.of(), List.of("Musical"), List.of(), List.of("G"), null, null);
        Facets facets = calculator.fromAggregations(Map.of(
                CatalogFields.GENRES, terms("Drama", 2, "Action", 2),
                CatalogFields.RATING, terms("R", 1),
                CatalogFields.ADULT, longTerms(1L, 4, 0L, 1),
                CatalogFields.RELEASE_DATE_MIN, Aggregate.of(agg -> agg.min(min -> min
                        .value(0d)
                        .valueAsString("1997-12-19"))),
                CatalogFields.RELEASE_DATE_MAX, Aggregate.of(agg -> agg.max(max -> max
                        .value(EPOCH_2012_01_01)))),
                selected);

        assertThat(facets.genres()).containsExactly(
                new FacetValue("Action", 2),
                new FacetValue("Drama", 2),
                new FacetValue("Musical", 0));
        assertThat(facets.ratings()).containsExactly(
                new FacetValue("R", 1),
                new FacetValue("G", 0));
        assertThat(facets.adult()).containsExactly(
                new FacetValue("true", 4),
                new FacetValue("false", 1));
        assertThat(facets.spokenLanguages()).isEmpty();
        assertThat(facets.earliestRelease()).isEqualTo(TITANIC);
        assertThat(facets.latestRelease()).isEqualTo(LocalDate.parse("2012-01-01"));
    }

    private static Aggregate terms(String first, long firstCount, String second, long secondCount) {
        return Aggregate.of(agg -> agg.sterms(terms -> terms.buckets(buckets -> buckets.array(List.of(
                StringTermsBucket.of(bucket -> bucket.key(FieldValue.of(first)).docCount(firstCount)),
                StringTermsBucket.of(bucket -> bucket.key(FieldValue.of(second)).docCount(secondCount)))))));
    }

    private static Aggregate terms(String label, long count) {
        return Aggregate.of(agg -> agg.sterms(terms -> terms.buckets(buckets -> buckets.array(List.of(
                StringTermsBucket.of(bucket -> bucket.key(FieldValue.of(label)).docCount(count)))))));
    }

    private static Aggregate longTerms(long first, long firstCount, long second, long secondCount) {
        return Aggregate.of(agg -> agg.lterms(terms -> terms.buckets(buckets -> buckets.array(List.of(
                LongTermsBucket.of(bucket -> bucket.key(first).docCount(firstCount)),
                LongTermsBucket.of(bucket -> bucket.key(second).docCount(secondCount)))))));
    }

    private static Movie movie(String id, LocalDate date, Boolean adult, String rating, List<String> genres,
            List<String> languages) {
        return new Movie(id, null, id, null, null, date, null, null, adult, null,
                null, null, null, genres, null, null, null, null, null, null,
                null, null, rating, null, null, null, null, null, languages, null, null, null, null, null);
    }
}
