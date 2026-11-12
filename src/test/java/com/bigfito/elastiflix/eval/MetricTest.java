package com.bigfito.elastiflix.eval;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MetricTest {

    @Test
    @DisplayName("cada métrica pide k = 10 y las binarias usan el umbral 2")
    void shouldBuildTheRankEvalMetricObjects() {
        assertThat(Metric.MRR.spec().toString())
                .isEqualTo("{\"mean_reciprocal_rank\":{\"k\":10,\"relevant_rating_threshold\":2}}");
        assertThat(Metric.PRECISION.spec().toString()).isEqualTo(
                "{\"precision\":{\"k\":10,\"relevant_rating_threshold\":2,\"ignore_unlabeled\":false}}");
        assertThat(Metric.RECALL.spec().toString())
                .isEqualTo("{\"recall\":{\"k\":10,\"relevant_rating_threshold\":2}}");
        assertThat(Metric.NDCG.spec().toString()).isEqualTo("{\"dcg\":{\"k\":10,\"normalize\":true}}");
    }

    @Test
    @DisplayName("la híbrida y la vectorial usan los valores por defecto de la aplicación")
    void shouldUseTheAppDefaults() {
        assertThat(EvalConfig.RRF_256.query("x").knn().k()).isEqualTo(100);
        assertThat(EvalConfig.RRF_256.query("x").knn().numCandidates()).isEqualTo(200);
        assertThat(EvalConfig.KNN_512.query("x").knn().field().fieldName()).isEqualTo("plot_512");
    }
}
