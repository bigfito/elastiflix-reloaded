package com.bigfito.elastiflix.search.result;

import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.search.query.FullTextSettings;
import com.bigfito.elastiflix.search.query.KnnSettings;
import com.bigfito.elastiflix.search.query.MatchType;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;

/**
 * Etiquetas de ajuste, en el orden del mockup y solo las que aplican al modo.
 */
final class AdjustmentLabels {

    private AdjustmentLabels() {
    }

    /**
     * Arma las etiquetas de una consulta ya validada.
     *
     * @param query consulta
     * @return por ejemplo {@code match · title}, {@code OR}, {@code plot_1024}, {@code k 100}
     */
    static List<String> of(SearchQuery query) {
        List<String> labels = new ArrayList<>();
        addText(labels, query.fullText());
        addVector(labels, query.knn());
        if (query.mode() == SearchMode.HYBRID) {
            labels.add("RRF");
        }
        return List.copyOf(labels);
    }

    private static void addText(List<String> labels, FullTextSettings settings) {
        if (settings == null) {
            return;
        }
        labels.add(match(settings));
        labels.add(settings.operator().name());
    }

    private static String match(FullTextSettings settings) {
        if (settings.type() == MatchType.MULTI_MATCH) {
            return "multi-match · best_fields";
        }
        return "match · " + settings.field().fieldName();
    }

    private static void addVector(List<String> labels, KnnSettings knn) {
        if (knn == null) {
            return;
        }
        labels.add(knn.field().fieldName());
        labels.add("k " + knn.k());
        labels.add("num_candidates " + knn.numCandidates());
    }
}
