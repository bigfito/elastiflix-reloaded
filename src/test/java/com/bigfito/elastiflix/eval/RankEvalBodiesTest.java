package com.bigfito.elastiflix.eval;

import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import com.bigfito.elastiflix.search.filter.FilterQueryBuilder;
import com.bigfito.elastiflix.search.strategy.FullTextRequestBuilder;
import com.bigfito.elastiflix.search.strategy.HybridRequestBuilder;
import com.bigfito.elastiflix.search.strategy.LexicalQueryBuilder;
import com.bigfito.elastiflix.search.strategy.VectorRequestBuilder;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

class RankEvalBodiesTest {

    private static final float[] MODEL = model();

    private final RankEvalBodies bodies = new RankEvalBodies(
            new FullTextRequestBuilder(new LexicalQueryBuilder(new FilterQueryBuilder())),
            new VectorRequestBuilder(new FilterQueryBuilder()),
            new HybridRequestBuilder(new LexicalQueryBuilder(new FilterQueryBuilder()), new FilterQueryBuilder(),
                    new VectorRequestBuilder(new FilterQueryBuilder())),
            new Jackson3JsonpMapper());

    @Test
    @DisplayName("full-text conserva solo la consulta: sin agregaciones, página ni _source")
    void shouldKeepOnlyTheQueryForFullText() {
        ObjectNode body = bodies.body(EvalConfig.BM25_TITLE, "alias", "titanic", MODEL);

        assertThat(body.propertyNames()).containsExactly("query");
        assertThat(body.at("/query/bool/must/0/match/title/query").asString()).isEqualTo("titanic");
    }

    @Test
    @DisplayName("la vectorial usa el retriever knn con el vector recortado y renormalizado")
    void shouldUseTheKnnRetrieverWithTheProjectedVector() {
        ObjectNode body = bodies.body(EvalConfig.KNN_256, "alias", "un barco", MODEL);

        assertThat(body.propertyNames()).containsExactly("retriever");
        assertThat(body.at("/retriever/knn/field").asString()).isEqualTo("plot_256");
        assertThat(body.at("/retriever/knn/k").asInt()).isEqualTo(100);
        assertThat(body.at("/retriever/knn/query_vector")).hasSize(256);
    }

    @Test
    @DisplayName("la híbrida usa la búsqueda RRF de la aplicación, no sus listas de apoyo")
    void shouldUseTheFusedRrfSearch() {
        ObjectNode body = bodies.body(EvalConfig.RRF_1024, "alias", "un barco", MODEL);

        assertThat(body.at("/retriever/rrf/rank_window_size").asInt()).isEqualTo(100);
        assertThat(body.at("/retriever/rrf/retrievers")).hasSize(2);
        assertThat(body.at("/retriever/rrf/retrievers/1/knn/query_vector")).hasSize(1024);
    }

    private static float[] model() {
        float[] vector = new float[1024];
        Arrays.fill(vector, 1f / 32f);
        return vector;
    }
}
