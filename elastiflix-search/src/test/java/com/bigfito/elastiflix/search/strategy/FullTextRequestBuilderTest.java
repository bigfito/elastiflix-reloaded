package com.bigfito.elastiflix.search.strategy;

import java.time.LocalDate;
import java.util.List;

import com.bigfito.elastiflix.search.JsonDocuments;
import com.bigfito.elastiflix.search.filter.FilterQueryBuilder;
import com.bigfito.elastiflix.search.query.FullTextSettings;
import com.bigfito.elastiflix.search.query.MatchType;
import com.bigfito.elastiflix.search.query.SearchFilters;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.query.TextField;
import com.bigfito.elastiflix.search.query.TextOperator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El JSON de full-text: {@code match}, {@code multi_match}, página y agregaciones, sin vectores.
 */
class FullTextRequestBuilderTest {

    private static final String AGGREGATIONS = "\"aggregations\":{"
            + "\"spoken_languages\":{\"terms\":{\"field\":\"spoken_languages\",\"size\":100}},"
            + "\"genres\":{\"terms\":{\"field\":\"genres\",\"size\":100}},"
            + "\"rating\":{\"terms\":{\"field\":\"rating\",\"size\":100}},"
            + "\"release_date_max\":{\"max\":{\"field\":\"release_date\"}},"
            + "\"release_date_min\":{\"min\":{\"field\":\"release_date\"}},"
            + "\"adult\":{\"terms\":{\"field\":\"adult\",\"size\":100}}}";

    private static final String SOURCE = "\"_source\":{\"excludes\":[\"plot_256\",\"plot_512\",\"plot_1024\"]}";

    private static final String MATCH = "{" + SOURCE + "," + AGGREGATIONS
            + ",\"from\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"title\":"
            + "{\"operator\":\"or\",\"query\":\"dark knight\"}}}]}},\"size\":25,\"track_total_hits\":true}";

    private static final String MULTI_MATCH = "{" + SOURCE + "," + AGGREGATIONS
            + ",\"from\":25,\"query\":{\"bool\":{\"must\":[{\"multi_match\":{\"fields\":"
            + "[\"title\",\"original_title\"],\"operator\":\"and\",\"query\":\"heaven cielo\","
            + "\"type\":\"best_fields\"}}]}},\"size\":25,\"track_total_hits\":true}";

    private static final String WITH_FILTERS = "{" + SOURCE + "," + AGGREGATIONS
            + ",\"from\":0,\"query\":{\"bool\":{\"filter\":[{\"term\":{\"adult\":{\"value\":false}}},"
            + "{\"terms\":{\"genres\":[\"Drama\"]}},{\"range\":{\"release_date\":"
            + "{\"gte\":\"1997-01-01\",\"format\":\"yyyy-MM-dd\"}}}],\"must\":[{\"match\":{\"title\":"
            + "{\"operator\":\"and\",\"query\":\"love story\"}}}]}},\"size\":25,\"track_total_hits\":true}";

    private final FullTextRequestBuilder builder = new FullTextRequestBuilder(
            new LexicalQueryBuilder(new FilterQueryBuilder()));

    @Test
    @DisplayName("match sobre title pide la página, el total y las agregaciones")
    void shouldBuildMatchWhenTheFieldIsTitle() {
        SearchQuery query = query("dark knight", MatchType.MATCH, TextField.TITLE, TextOperator.OR, 1,
                SearchFilters.none());
        assertThat(JsonDocuments.of(builder.build("catalogo", query))).isEqualTo(MATCH);
    }

    @Test
    @DisplayName("multi_match best_fields busca en los dos títulos con AND")
    void shouldBuildMultiMatchWhenThereIsNoField() {
        SearchQuery query = query("heaven cielo", MatchType.MULTI_MATCH, null, TextOperator.AND, 2,
                SearchFilters.none());
        assertThat(JsonDocuments.of(builder.build("catalogo", query))).isEqualTo(MULTI_MATCH);
    }

    @Test
    @DisplayName("los filtros van en bool.filter y no en must")
    void shouldPutFiltersBesideTheTextWhenTheyArePresent() {
        SearchFilters filters = new SearchFilters(
                List.of(false), List.of("Drama"), List.of(), List.of(),
                LocalDate.parse("1997-01-01"), null);
        SearchQuery query = query("love story", MatchType.MATCH, TextField.TITLE, TextOperator.AND, 1, filters);
        assertThat(JsonDocuments.of(builder.build("catalogo", query))).isEqualTo(WITH_FILTERS);
    }

    private static SearchQuery query(String text, MatchType type, TextField field, TextOperator operator,
            int page, SearchFilters filters) {
        return new SearchQuery(text, SearchMode.FULL_TEXT, new FullTextSettings(type, field, operator),
                null, filters, page, SearchQuery.PAGE_25);
    }
}
