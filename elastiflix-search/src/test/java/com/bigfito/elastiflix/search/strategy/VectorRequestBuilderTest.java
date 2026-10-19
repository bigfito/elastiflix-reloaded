package com.bigfito.elastiflix.search.strategy;

import java.util.List;

import com.bigfito.elastiflix.search.JsonDocuments;
import com.bigfito.elastiflix.search.filter.FilterQueryBuilder;
import com.bigfito.elastiflix.search.query.KnnSettings;
import com.bigfito.elastiflix.search.query.SearchFilters;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.query.VectorField;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El JSON del retriever {@code knn}. El vector de la prueba es corto y no se registra.
 */
class VectorRequestBuilderTest {

    private static final float[] VECTOR = {0.5f, 0.25f};
    private static final String SOURCE = "\"_source\":{\"excludes\":[\"plot_256\",\"plot_512\",\"plot_1024\"]}";
    private static final String KNN = "\"field\":\"plot_1024\",\"query_vector\":[0.5,0.25],\"k\":100,"
            + "\"num_candidates\":200";
    private static final String WITHOUT_FILTERS = "{" + SOURCE + ",\"retriever\":{\"knn\":{" + KNN + "}},\"size\":100}";
    private static final String WITH_FILTERS = "{" + SOURCE + ",\"retriever\":{\"knn\":{\"filter\":["
            + "{\"term\":{\"adult\":{\"value\":false}}},{\"terms\":{\"genres\":[\"Drama\"]}}]," + KNN
            + "}},\"size\":100}";

    private final VectorRequestBuilder builder = new VectorRequestBuilder(new FilterQueryBuilder());

    @Test
    @DisplayName("knn pide el campo, k, los candidatos y size igual a k")
    void shouldBuildKnnWhenThereAreNoFilters() {
        assertThat(json(SearchFilters.none())).isEqualTo(WITHOUT_FILTERS);
    }

    @Test
    @DisplayName("el filtro va dentro del retriever knn")
    void shouldPutTheFilterInsideKnnWhenOneIsPresent() {
        SearchFilters filters = new SearchFilters(List.of(false), List.of("Drama"), List.of(), List.of(), null, null);
        assertThat(json(filters)).isEqualTo(WITH_FILTERS);
    }

    @Test
    @DisplayName("la confirmación de un vacío pide un documento y deja k en 100")
    void shouldAskForOneHitWhenConfirmingThereAreResults() {
        SearchQuery query = new SearchQuery(
                "un barco",
                SearchMode.VECTOR,
                null,
                new KnnSettings(VectorField.PLOT_1024, 100, 200),
                SearchFilters.none(),
                1,
                SearchQuery.PAGE_25);
        String body = JsonDocuments.of(builder.build("catalogo", query, VECTOR, 1));
        assertThat(body).contains("\"size\":1");
        assertThat(body).contains("\"k\":100");
        assertThat(body).doesNotContain("\"size\":100");
    }

    private String json(SearchFilters filters) {
        SearchQuery query = new SearchQuery(
                "un barco",
                SearchMode.VECTOR,
                null,
                new KnnSettings(VectorField.PLOT_1024, 100, 200),
                filters,
                1,
                SearchQuery.PAGE_25);
        return JsonDocuments.of(builder.build("catalogo", query, VECTOR, query.knn().k()));
    }
}
