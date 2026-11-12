package com.bigfito.elastiflix.eval;

import com.bigfito.elastiflix.search.query.FullTextSettings;
import com.bigfito.elastiflix.search.query.KnnSettings;
import com.bigfito.elastiflix.search.query.MatchType;
import com.bigfito.elastiflix.search.query.SearchFilters;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.query.TextField;
import com.bigfito.elastiflix.search.query.TextOperator;
import com.bigfito.elastiflix.search.query.VectorField;

/**
 * Las configuraciones que compara el harness. Cada una es una consulta de la aplicación con sus ajustes.
 *
 * <p>Usan los valores por defecto de la página de Inicio: operador {@code OR}, {@code k = 100} y
 * {@code num_candidates = 200}. La híbrida combina el {@code match} sobre {@code title} o {@code plot} con el
 * k-NN.</p>
 */
public enum EvalConfig {

    /** Full-text, {@code match} sobre {@code title}: lo que hace la aplicación por defecto. */
    BM25_TITLE("bm25_title", SearchMode.FULL_TEXT, MatchType.MATCH, TextField.TITLE, VectorField.PLOT_256),
    /** Full-text, {@code multi_match} sobre {@code title} y {@code original_title}. */
    BM25_MULTI_MATCH("bm25_multi_match", SearchMode.FULL_TEXT, MatchType.MULTI_MATCH, TextField.TITLE,
            VectorField.PLOT_256),
    /** Full-text, {@code match} sobre la sinopsis. */
    BM25_PLOT("bm25_plot", SearchMode.FULL_TEXT, MatchType.MATCH, TextField.PLOT, VectorField.PLOT_256),
    /** Vectorial con 256 dimensiones. */
    KNN_256("knn_plot_256", SearchMode.VECTOR, MatchType.MATCH, TextField.TITLE, VectorField.PLOT_256),
    /** Vectorial con 512 dimensiones. */
    KNN_512("knn_plot_512", SearchMode.VECTOR, MatchType.MATCH, TextField.TITLE, VectorField.PLOT_512),
    /** Vectorial con 1024 dimensiones. */
    KNN_1024("knn_plot_1024", SearchMode.VECTOR, MatchType.MATCH, TextField.TITLE, VectorField.PLOT_1024),
    /** Híbrida: BM25 sobre {@code title} y k-NN con 256 dimensiones. Es la combinación por defecto. */
    RRF_TITLE_256("rrf_title_knn256", SearchMode.HYBRID, MatchType.MATCH, TextField.TITLE, VectorField.PLOT_256),
    /** Híbrida: BM25 sobre {@code title} y k-NN con 1024 dimensiones. */
    RRF_TITLE_1024("rrf_title_knn1024", SearchMode.HYBRID, MatchType.MATCH, TextField.TITLE,
            VectorField.PLOT_1024),
    /** Híbrida: BM25 sobre {@code plot} y k-NN con 256 dimensiones. */
    RRF_PLOT_256("rrf_plot_knn256", SearchMode.HYBRID, MatchType.MATCH, TextField.PLOT, VectorField.PLOT_256),
    /** Híbrida: BM25 sobre {@code plot} y k-NN con 1024 dimensiones. */
    RRF_PLOT_1024("rrf_plot_knn1024", SearchMode.HYBRID, MatchType.MATCH, TextField.PLOT, VectorField.PLOT_1024);

    /** {@code k} de la búsqueda vectorial y {@code rank_window_size} de la híbrida. */
    public static final int K = 100;
    /** {@code num_candidates} de la búsqueda vectorial. */
    public static final int NUM_CANDIDATES = 200;

    private final String key;
    private final SearchMode mode;
    private final MatchType matchType;
    private final TextField textField;
    private final VectorField vectorField;

    EvalConfig(String key, SearchMode mode, MatchType matchType, TextField textField, VectorField vectorField) {
        this.key = key;
        this.mode = mode;
        this.matchType = matchType;
        this.textField = textField;
        this.vectorField = vectorField;
    }

    /**
     * Nombre de la configuración en el informe y en {@code eval/thresholds.json}.
     *
     * @return por ejemplo {@code knn_plot_1024} o {@code rrf_plot_knn256}
     */
    public String key() {
        return key;
    }

    /**
     * Modo de búsqueda.
     *
     * @return full-text, vectorial o híbrida
     */
    public SearchMode mode() {
        return mode;
    }

    /**
     * Campo vectorial; en full-text no se usa.
     *
     * @return {@code plot_256}, {@code plot_512} o {@code plot_1024}
     */
    public VectorField vectorField() {
        return vectorField;
    }

    /**
     * La consulta que recibiría la aplicación, sin filtros y en la primera página.
     *
     * @param text texto de la consulta
     * @return consulta lista para los armadores de la aplicación
     */
    public SearchQuery query(String text) {
        return new SearchQuery(
                text,
                mode,
                new FullTextSettings(matchType, textField, TextOperator.OR),
                new KnnSettings(vectorField, K, NUM_CANDIDATES),
                SearchFilters.none(),
                1,
                SearchQuery.PAGE_25);
    }
}
