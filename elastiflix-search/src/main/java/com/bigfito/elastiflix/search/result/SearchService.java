package com.bigfito.elastiflix.search.result;

import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.SearchValidationException;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.search.facet.FacetCalculator;
import com.bigfito.elastiflix.search.facet.Facets;
import com.bigfito.elastiflix.search.page.Pagination;
import com.bigfito.elastiflix.search.page.Paginator;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.query.SearchQueryValidator;
import com.bigfito.elastiflix.search.strategy.RankedHit;
import com.bigfito.elastiflix.search.strategy.SearchStrategy;
import com.bigfito.elastiflix.search.strategy.StrategyResult;

import org.springframework.stereotype.Service;

/**
 * Fachada de la búsqueda: valida, elige el modo, pagina y distingue los dos estados vacíos.
 */
@Service
public class SearchService {

    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final List<SearchStrategy> strategies;
    private final SearchQueryValidator validator;
    private final FacetCalculator facets;
    private final Paginator paginator;

    /**
     * Crea la fachada.
     *
     * @param strategies una estrategia por modo
     * @param facets     conteos de full-text o de las películas devueltas
     * @param paginator  rango y recorte de la página
     */
    public SearchService(List<SearchStrategy> strategies, FacetCalculator facets, Paginator paginator) {
        this.strategies = List.copyOf(strategies);
        this.validator = new SearchQueryValidator();
        this.facets = facets;
        this.paginator = paginator;
    }

    /**
     * Ejecuta una consulta.
     *
     * @param query texto, modo, filtros y página
     * @return la página, el total, las facetas y, si aplica, el estado vacío
     */
    public SearchResult search(SearchQuery query) {
        validator.validate(query);
        SearchLog.debug(query);
        long started = System.nanoTime();
        SearchStrategy strategy = strategy(query.mode());
        StrategyResult found = strategy.search(query);
        SearchLog.finished(query.mode(), found.total(), (System.nanoTime() - started) / NANOS_PER_MILLI);
        return assemble(query, strategy, found);
    }

    private SearchStrategy strategy(SearchMode mode) {
        for (SearchStrategy candidate : strategies) {
            if (candidate.mode() == mode) {
                return candidate;
            }
        }
        throw new SearchValidationException(ErrorCode.MISSING_MODE_SETTINGS);
    }

    private SearchResult assemble(SearchQuery query, SearchStrategy strategy, StrategyResult found) {
        List<RankedHit> page = page(query, found);
        return new SearchResult(
                hits(page),
                found.total(),
                facetValues(query, found),
                pagination(query, found.total()),
                AdjustmentLabels.of(query),
                empty(query, strategy, found.total()));
    }

    private List<RankedHit> page(SearchQuery query, StrategyResult found) {
        if (query.mode() == SearchMode.FULL_TEXT) {
            return found.hits();
        }
        return paginator.slice(found.hits(), query.page(), query.pageSize());
    }

    private Facets facetValues(SearchQuery query, StrategyResult found) {
        if (query.mode() == SearchMode.FULL_TEXT) {
            return facets.fromAggregations(found.aggregations(), query.filters());
        }
        return facets.fromMovies(movies(found.hits()), query.filters());
    }

    private Pagination pagination(SearchQuery query, long total) {
        return paginator.describe(total, query.page(), query.pageSize());
    }

    private EmptyState empty(SearchQuery query, SearchStrategy strategy, long total) {
        if (total > 0) {
            return null;
        }
        if (!query.filters().isEmpty() && strategy.foundWithoutFilters(query) > 0) {
            return EmptyState.filtered();
        }
        return EmptyState.none(query.text(), query.mode());
    }

    private static List<Movie> movies(List<RankedHit> hits) {
        List<Movie> movies = new ArrayList<>();
        for (RankedHit hit : hits) {
            movies.add(hit.movie());
        }
        return movies;
    }

    private static List<SearchHit> hits(List<RankedHit> page) {
        List<SearchHit> hits = new ArrayList<>();
        for (RankedHit hit : page) {
            hits.add(hit(hit));
        }
        return hits;
    }

    private static SearchHit hit(RankedHit ranked) {
        Movie movie = ranked.movie();
        return new SearchHit(
                movie.id(),
                movie.title(),
                movie.originalTitle(),
                movie.plot(),
                movie.posterPath(),
                movie.releaseDate(),
                movie.runtime(),
                movie.genres(),
                movie.voteAverage(),
                movie.voteCount(),
                movie.rating(),
                ranked.score(),
                ranked.lexical(),
                ranked.vector(),
                ranked.rrf());
    }
}
