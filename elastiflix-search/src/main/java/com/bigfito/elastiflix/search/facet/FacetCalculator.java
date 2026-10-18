package com.bigfito.elastiflix.search.facet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.search.filter.CatalogFields;
import com.bigfito.elastiflix.search.query.SearchFilters;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;

import org.springframework.stereotype.Component;

/**
 * Cuenta géneros, idiomas, clasificación y adulto, y el rango de fechas de estreno.
 */
@Component
public class FacetCalculator {

    private static final String TRUE = "true";
    private static final String FALSE = "false";

    /**
     * Cuenta las películas devueltas. En vectorial e híbrida son las {@code k}, no la página.
     *
     * @param movies   películas del resultado
     * @param selected valores que el usuario marcó; se quedan aunque cuenten 0
     * @return facetas ordenadas
     */
    public Facets fromMovies(List<Movie> movies, SearchFilters selected) {
        DateSpan dates = span(movies);
        return new Facets(
                facet(count(movies, Movie::genres), selected.genres()),
                facet(count(movies, Movie::spokenLanguages), selected.spokenLanguages()),
                facet(count(movies, FacetCalculator::rating), selected.ratings()),
                facet(countAdult(movies), labels(selected.adult())),
                dates.earliest(),
                dates.latest());
    }

    /**
     * Lee las agregaciones de full-text. Un valor marcado que no vino en la agregación cuenta 0.
     *
     * @param aggregations agregaciones por nombre; puede ser un mapa vacío
     * @param selected     valores que el usuario marcó
     * @return facetas ordenadas
     */
    public Facets fromAggregations(Map<String, Aggregate> aggregations, SearchFilters selected) {
        Map<String, Aggregate> source = aggregations == null ? Map.of() : aggregations;
        return new Facets(
                facet(AggregationFacets.terms(source.get(CatalogFields.GENRES)), selected.genres()),
                facet(AggregationFacets.terms(source.get(CatalogFields.SPOKEN_LANGUAGES)),
                        selected.spokenLanguages()),
                facet(AggregationFacets.terms(source.get(CatalogFields.RATING)), selected.ratings()),
                facet(AggregationFacets.adult(source.get(CatalogFields.ADULT)), labels(selected.adult())),
                AggregationFacets.date(source.get(CatalogFields.RELEASE_DATE_MIN)),
                AggregationFacets.date(source.get(CatalogFields.RELEASE_DATE_MAX)));
    }

    private static Map<String, Long> count(List<Movie> movies, Function<Movie, List<String>> values) {
        Map<String, Long> counts = new HashMap<>();
        for (Movie movie : movies) {
            addAll(counts, values.apply(movie));
        }
        return counts;
    }

    private static List<String> rating(Movie movie) {
        if (movie.rating() == null) {
            return List.of();
        }
        return List.of(movie.rating());
    }

    private static Map<String, Long> countAdult(List<Movie> movies) {
        Map<String, Long> counts = new HashMap<>();
        for (Movie movie : movies) {
            if (movie.adult() != null) {
                put(counts, movie.adult() ? TRUE : FALSE, 1L);
            }
        }
        return counts;
    }

    private static void addAll(Map<String, Long> counts, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            put(counts, value, 1L);
        }
    }

    private static void put(Map<String, Long> counts, String label, long count) {
        if (label != null && !label.isBlank()) {
            counts.merge(label, count, Long::sum);
        }
    }

    private static List<String> labels(List<Boolean> adult) {
        List<String> labels = new ArrayList<>();
        for (Boolean value : adult) {
            if (value != null) {
                labels.add(value ? TRUE : FALSE);
            }
        }
        return labels;
    }

    private static List<FacetValue> facet(Map<String, Long> counts, List<String> selected) {
        Map<String, Long> merged = new HashMap<>(counts);
        for (String label : selected) {
            if (label != null && !label.isBlank()) {
                merged.putIfAbsent(label, 0L);
            }
        }
        List<FacetValue> values = new ArrayList<>();
        for (Map.Entry<String, Long> entry : merged.entrySet()) {
            values.add(new FacetValue(entry.getKey(), entry.getValue()));
        }
        values.sort(Comparator.comparingLong(FacetValue::count).reversed().thenComparing(FacetValue::label));
        return List.copyOf(values);
    }

    private static DateSpan span(List<Movie> movies) {
        LocalDate earliest = null;
        LocalDate latest = null;
        for (Movie movie : movies) {
            LocalDate date = movie.releaseDate();
            earliest = earlier(earliest, date);
            latest = later(latest, date);
        }
        return new DateSpan(earliest, latest);
    }

    private static LocalDate earlier(LocalDate current, LocalDate date) {
        if (date != null && (current == null || date.isBefore(current))) {
            return date;
        }
        return current;
    }

    private static LocalDate later(LocalDate current, LocalDate date) {
        if (date != null && (current == null || date.isAfter(current))) {
            return date;
        }
        return current;
    }

    private record DateSpan(LocalDate earliest, LocalDate latest) {
    }
}
