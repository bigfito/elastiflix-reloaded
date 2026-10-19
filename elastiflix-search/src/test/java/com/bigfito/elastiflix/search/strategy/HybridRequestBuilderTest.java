package com.bigfito.elastiflix.search.strategy;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bigfito.elastiflix.search.JsonDocuments;
import com.bigfito.elastiflix.search.filter.FilterQueryBuilder;
import com.bigfito.elastiflix.search.query.FullTextSettings;
import com.bigfito.elastiflix.search.query.KnnSettings;
import com.bigfito.elastiflix.search.query.MatchType;
import com.bigfito.elastiflix.search.query.SearchFilters;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.query.TextField;
import com.bigfito.elastiflix.search.query.TextOperator;
import com.bigfito.elastiflix.search.query.VectorField;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El JSON de las tres partes del {@code _msearch}. El vector de la prueba es corto.
 */
class HybridRequestBuilderTest {

    private static final float[] VECTOR = {0.5f, 0.25f};
    private static final String PREFERENCE = "elastiflix-test";
    private static final Pattern GENERATED = Pattern.compile("elastiflix-[0-9a-f-]{36}");
    /** Una por cabecera del {@code _msearch}: las tres búsquedas comparten el mismo valor. */
    private static final int SHARED_PREFERENCES = 3;
    private static final String HEADER =
            "\"header\":{\"index\":[\"catalogo\"],\"preference\":\"elastiflix-test\"}";
    private static final String SOURCE = "\"_source\":{\"excludes\":[\"plot_256\",\"plot_512\",\"plot_1024\"]}";
    private static final String IDS = "\"_source\":false";
    private static final String MATCH = "{\"match\":{\"title\":{\"operator\":\"and\",\"query\":\"toy story\"}}}";
    private static final String KNN = "\"field\":\"plot_1024\",\"query_vector\":[0.5,0.25],\"k\":100,"
            + "\"num_candidates\":200";
    private static final String FILTERS =
            "{\"term\":{\"adult\":{\"value\":false}}},{\"terms\":{\"genres\":[\"Drama\"]}}";
    private static final String WITHOUT_FILTERS = "["
            + "{" + HEADER + ",\"body\":{\"retriever\":{\"rrf\":{\"retrievers\":["
            + "{\"standard\":{\"query\":" + MATCH + "}},{\"knn\":{" + KNN + "}}],"
            + "\"rank_constant\":60,\"rank_window_size\":100}},\"size\":100," + SOURCE + "}},"
            + "{" + HEADER + ",\"body\":{\"query\":{\"bool\":{\"must\":[" + MATCH + "]}},\"size\":100,"
            + IDS + "}},"
            + "{" + HEADER + ",\"body\":{\"retriever\":{\"knn\":{" + KNN + "}},\"size\":100," + IDS + "}}]";
    private static final String WITH_FILTERS = "["
            + "{" + HEADER + ",\"body\":{\"retriever\":{\"rrf\":{\"filter\":[" + FILTERS + "],\"retrievers\":["
            + "{\"standard\":{\"query\":" + MATCH + "}},{\"knn\":{" + KNN + "}}],"
            + "\"rank_constant\":60,\"rank_window_size\":100}},\"size\":100," + SOURCE + "}},"
            + "{" + HEADER + ",\"body\":{\"query\":{\"bool\":{\"filter\":[" + FILTERS + "],\"must\":["
            + MATCH + "]}},\"size\":100," + IDS + "}},"
            + "{" + HEADER + ",\"body\":{\"retriever\":{\"knn\":{\"filter\":[" + FILTERS + "],"
            + KNN + "}},\"size\":100," + IDS + "}}]";

    private final HybridRequestBuilder builder = new HybridRequestBuilder(
            new LexicalQueryBuilder(new FilterQueryBuilder()),
            new FilterQueryBuilder(),
            new VectorRequestBuilder(new FilterQueryBuilder()));

    @Test
    @DisplayName("las tres búsquedas comparten preference, rank_constant 60 y size igual a k")
    void shouldBuildThreeSearchesWhenThereAreNoFilters() {
        assertThat(json(SearchFilters.none())).isEqualTo(WITHOUT_FILTERS);
    }

    @Test
    @DisplayName("el filtro va en el rrf y en los componentes, no dentro de standard ni de knn del rrf")
    void shouldPutTheFilterOnRrfAndOnEachComponentWhenOneIsPresent() {
        SearchFilters filters = new SearchFilters(List.of(false), List.of("Drama"), List.of(), List.of(), null, null);
        assertThat(json(filters)).isEqualTo(WITH_FILTERS);
    }

    @Test
    @DisplayName("la confirmación de un vacío pide size 1 y conserva rank_window_size = k")
    void shouldAskForOneHitWhenConfirmingThereAreResults() {
        String body = json(SearchFilters.none(), 1);
        assertThat(body).contains("\"size\":1");
        assertThat(body).contains("\"rank_window_size\":100");
        assertThat(body).contains("\"k\":100");
        assertThat(body).doesNotContain("\"size\":100");
    }

    private String json(SearchFilters filters) {
        return json(filters, 100);
    }

    private String json(SearchFilters filters, int size) {
        SearchQuery query = new SearchQuery(
                "toy story",
                SearchMode.HYBRID,
                new FullTextSettings(MatchType.MATCH, TextField.TITLE, TextOperator.AND),
                new KnnSettings(VectorField.PLOT_1024, 100, 200),
                filters,
                1,
                SearchQuery.PAGE_25);
        String body = JsonDocuments.of(builder.build("catalogo", query, VECTOR, size));
        Matcher preference = GENERATED.matcher(body);
        assertThat(preference.find()).isTrue();
        String generated = preference.group();
        assertThat(body.split(Pattern.quote(generated), -1)).hasSize(SHARED_PREFERENCES + 1);
        return body.replace(generated, PREFERENCE);
    }
}
