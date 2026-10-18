package com.bigfito.elastiflix.search.result;

import com.bigfito.elastiflix.search.query.FullTextSettings;
import com.bigfito.elastiflix.search.query.KnnSettings;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registro de una búsqueda. No escribe el vector ni el JSON de la petición.
 */
final class SearchLog {

    private static final Logger LOG = LoggerFactory.getLogger(SearchLog.class);

    private SearchLog() {
    }

    /**
     * Anota la consulta antes de ejecutarla. El texto solo sale en {@code DEBUG}.
     *
     * @param query consulta ya validada
     */
    static void debug(SearchQuery query) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "Consulta texto={} modo={} campo={} operador={} vector={} k={} num_candidates={}"
                            + " página={} tamaño={}",
                    query.text(),
                    query.mode(),
                    textField(query.fullText()),
                    operator(query.fullText()),
                    vectorField(query.knn()),
                    k(query.knn()),
                    candidates(query.knn()),
                    query.page(),
                    query.pageSize());
        }
    }

    /**
     * Anota el cierre en {@code INFO}: modo, total y milisegundos.
     *
     * @param mode   modo ejecutado
     * @param total  películas que cumplen la búsqueda
     * @param millis tiempo medido por la fachada
     */
    static void finished(SearchMode mode, long total, long millis) {
        LOG.info("Búsqueda {} total {} en {} ms", mode, total, millis);
    }

    private static String textField(FullTextSettings settings) {
        if (settings == null || settings.field() == null) {
            return "";
        }
        return settings.field().fieldName();
    }

    private static String operator(FullTextSettings settings) {
        if (settings == null || settings.operator() == null) {
            return "";
        }
        return settings.operator().name();
    }

    private static String vectorField(KnnSettings knn) {
        if (knn == null) {
            return "";
        }
        return knn.field().fieldName();
    }

    private static String k(KnnSettings knn) {
        if (knn == null) {
            return "";
        }
        return Integer.toString(knn.k());
    }

    private static String candidates(KnnSettings knn) {
        if (knn == null) {
            return "";
        }
        return Integer.toString(knn.numCandidates());
    }
}
