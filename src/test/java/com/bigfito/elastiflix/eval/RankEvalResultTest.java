package com.bigfito.elastiflix.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class RankEvalResultTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    @DisplayName("lee el promedio, la métrica de cada consulta y los resultados con o sin calificación")
    void shouldReadScoresAndRatedHits() {
        RankEvalResult result = RankEvalResult.parse(json("""
                {"metric_score": 0.75,
                 "details": {"q1": {"metric_score": 0.5, "unrated_docs": [{"_index": "i", "_id": "9"}],
                   "hits": [{"hit": {"_index": "i", "_id": "9", "_score": 2.0}, "rating": null},
                            {"hit": {"_index": "i", "_id": "597", "_score": 1.0}, "rating": 3}]}},
                 "failures": {}}
                """));

        assertThat(result.score()).isEqualTo(0.75);
        RankEvalResult.QueryScore query = result.queries().get("q1");
        assertThat(query.score()).isEqualTo(0.5);
        assertThat(query.hits()).containsExactly(
                new RankEvalResult.RatedHit("9", null), new RankEvalResult.RatedHit("597", 3));
        assertThat(query.rankOf("597")).isEqualTo(2);
        assertThat(query.rankOf("1")).isZero();
    }

    @Test
    @DisplayName("falla si alguna consulta devolvió error")
    void shouldFailOnFailures() {
        JsonNode response = json("""
                {"metric_score": "NaN", "details": {}, "failures": {"q1": {"error": {"reason": "malo"}}}}
                """);

        assertThatThrownBy(() -> RankEvalResult.parse(response)).hasMessageContaining("malo");
    }

    @Test
    @DisplayName("el subconjunto promedia solo sus consultas y la posición sale del detalle de nDCG")
    void shouldAverageSubsetsAndFindTargets() {
        Judgment spanish = judgment("es-plot-01", "es", "597");
        Judgment english = judgment("en-plot-01", "en", "27205");
        RankEvalResult ndcg = new RankEvalResult(0.5, Map.of(
                "es-plot-01", score(1.0, "597"),
                "en-plot-01", score(0.0, "1", "2")));
        ConfigResult result = new ConfigResult(EvalConfig.KNN_1024, Map.of(
                Metric.NDCG, ndcg, Metric.MRR, ndcg, Metric.PRECISION, ndcg, Metric.RECALL, ndcg));

        assertThat(result.score(Metric.NDCG, List.of(spanish, english), j -> j.lang().equals("es")))
                .isEqualTo(1.0);
        assertThat(result.targetRank(spanish)).isEqualTo(1);
        assertThat(result.targetRank(english)).isZero();
    }

    private static Judgment judgment(String id, String lang, String target) {
        return new Judgment(id, lang, "plot", "texto", target, Map.of(target, 3));
    }

    private static RankEvalResult.QueryScore score(double value, String... ids) {
        return new RankEvalResult.QueryScore(value, Arrays.stream(ids)
                .map(id -> new RankEvalResult.RatedHit(id, null)).toList());
    }

    private static JsonNode json(String text) {
        return JSON.readTree(text);
    }
}
