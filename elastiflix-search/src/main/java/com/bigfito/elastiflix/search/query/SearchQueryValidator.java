package com.bigfito.elastiflix.search.query;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.SearchValidationException;

/**
 * Rechaza una consulta que no cumple las reglas de la demo.
 *
 * <p>Se detiene en la primera falla. El orden es el del mockup: texto, {@code k},
 * {@code num_candidates} y, al final, la página.</p>
 */
public class SearchQueryValidator {

    /** Tope de {@code num_candidates} y del primer resultado recorrible en full-text. */
    public static final int MAX_WINDOW = 10_000;

    /**
     * Lanza {@link SearchValidationException} si la consulta no se puede ejecutar.
     *
     * @param query consulta ya armada
     */
    public void validate(SearchQuery query) {
        requireText(query.text());
        requireMode(query);
        requirePageSize(query.pageSize());
        requireDates(query.filters());
        requirePage(query);
    }

    private static void requireText(String text) {
        if (text == null || text.isBlank()) {
            throw failure(ErrorCode.EMPTY_SEARCH_TEXT);
        }
    }

    private static void requireMode(SearchQuery query) {
        if (query.mode() == SearchMode.FULL_TEXT) {
            requireFullText(query.fullText());
            return;
        }
        requireKnn(query.knn());
        if (query.mode() == SearchMode.HYBRID) {
            requireFullText(query.fullText());
        }
    }

    private static void requireKnn(KnnSettings knn) {
        if (knn == null) {
            throw failure(ErrorCode.MISSING_MODE_SETTINGS);
        }
        if (knn.k() < 1) {
            throw failure(ErrorCode.K_TOO_SMALL);
        }
        if (knn.numCandidates() > MAX_WINDOW) {
            throw failure(ErrorCode.CANDIDATES_TOO_LARGE);
        }
        if (knn.numCandidates() < knn.k()) {
            throw failure(ErrorCode.CANDIDATES_BELOW_K);
        }
    }

    private static void requireFullText(FullTextSettings settings) {
        if (settings == null || settings.type() == null || settings.operator() == null) {
            throw failure(ErrorCode.MISSING_MODE_SETTINGS);
        }
        requireField(settings);
    }

    private static void requireField(FullTextSettings settings) {
        if (settings.type() == MatchType.MULTI_MATCH && settings.field() != null) {
            throw failure(ErrorCode.FIELD_WITH_MULTI_MATCH);
        }
        if (settings.type() == MatchType.MATCH && settings.field() == null) {
            throw failure(ErrorCode.MISSING_MODE_SETTINGS);
        }
    }

    private static void requirePageSize(int pageSize) {
        if (pageSize != SearchQuery.PAGE_25 && pageSize != SearchQuery.PAGE_50
                && pageSize != SearchQuery.PAGE_100) {
            throw failure(ErrorCode.PAGE_SIZE_NOT_ALLOWED);
        }
    }

    private static void requireDates(SearchFilters filters) {
        if (filters.from() != null && filters.to() != null && filters.from().isAfter(filters.to())) {
            throw failure(ErrorCode.DATE_RANGE_REVERSED);
        }
    }

    private static void requirePage(SearchQuery query) {
        if (query.page() < 1) {
            throw failure(ErrorCode.PAGE_TOO_SMALL);
        }
        long windowStart = (long) (query.page() - 1) * query.pageSize();
        if (windowStart >= windowOf(query)) {
            throw failure(windowCode(query.mode()));
        }
    }

    private static int windowOf(SearchQuery query) {
        if (query.mode() == SearchMode.FULL_TEXT) {
            return MAX_WINDOW;
        }
        return query.knn().k();
    }

    private static ErrorCode windowCode(SearchMode mode) {
        if (mode == SearchMode.FULL_TEXT) {
            return ErrorCode.FULL_TEXT_WINDOW_EXCEEDED;
        }
        return ErrorCode.KNN_WINDOW_EXCEEDED;
    }

    private static SearchValidationException failure(ErrorCode code) {
        return new SearchValidationException(code);
    }
}
