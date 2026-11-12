package com.bigfito.elastiflix.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ThresholdsTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    @DisplayName("informa solo las métricas por debajo de su mínimo")
    void shouldReportOnlyMetricsBelowTheirMinimum() {
        Thresholds thresholds = new Thresholds(JSON.readTree("""
                {"knn_plot_1024": {"ndcg@10": 0.80, "mrr@10": 0.50}}
                """));

        List<String> violations = thresholds.violations(List.of(result(EvalConfig.KNN_1024, 0.70)));

        assertThat(violations).containsExactly("knn_plot_1024 ndcg@10 = 0.700, mínimo 0.800");
    }

    @Test
    @DisplayName("una configuración sin mínimos no se revisa")
    void shouldSkipConfigsWithoutMinimums() {
        Thresholds thresholds = new Thresholds(JSON.readTree("{}"));

        assertThat(thresholds.violations(List.of(result(EvalConfig.BM25_TITLE, 0.0)))).isEmpty();
    }

    @Test
    @DisplayName("una métrica desconocida es un error del archivo")
    void shouldRejectUnknownMetrics() {
        Thresholds thresholds = new Thresholds(JSON.readTree("""
                {"knn_plot_1024": {"map@10": 0.5}}
                """));

        assertThatThrownBy(() -> thresholds.violations(List.of(result(EvalConfig.KNN_1024, 1.0))))
                .hasMessageContaining("map@10");
    }

    @Test
    @DisplayName("el archivo versionado define mínimos para cada configuración")
    void shouldDefineMinimumsForEveryConfig() {
        Thresholds versioned = Thresholds.read(Thresholds.DEFAULT);

        for (EvalConfig config : EvalConfig.values()) {
            assertThat(versioned.minimums().has(config.key())).as(config.key()).isTrue();
        }
    }

    private static ConfigResult result(EvalConfig config, double score) {
        Map<Metric, RankEvalResult> metrics = new EnumMap<>(Metric.class);
        for (Metric metric : Metric.values()) {
            metrics.put(metric, new RankEvalResult(score, Map.of()));
        }
        return new ConfigResult(config, metrics);
    }
}
