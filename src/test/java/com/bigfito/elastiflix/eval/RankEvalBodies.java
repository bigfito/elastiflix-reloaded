package com.bigfito.elastiflix.eval;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.JsonpSerializable;
import co.elastic.clients.json.JsonpUtils;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.strategy.FullTextRequestBuilder;
import com.bigfito.elastiflix.search.strategy.HybridRequestBuilder;
import com.bigfito.elastiflix.search.strategy.QueryVector;
import com.bigfito.elastiflix.search.strategy.VectorRequestBuilder;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Arma el {@code request} de cada consulta con los mismos armadores que usa la aplicación.
 *
 * <p>Así el harness mide lo que la aplicación envía. De la petición solo se conservan {@code query}, {@code knn}
 * y {@code retriever}: {@code _rank_eval} fija el tamaño con el {@code k} de la métrica, y las agregaciones,
 * la página y {@code _source} no influyen en el orden.</p>
 */
public final class RankEvalBodies {

    private static final List<String> KEPT = List.of("query", "knn", "retriever");

    private final FullTextRequestBuilder fullText;
    private final VectorRequestBuilder vectors;
    private final HybridRequestBuilder hybrid;
    private final JsonpMapper mapper;
    private final JsonMapper json = JsonMapper.builder().build();

    /**
     * Crea el armador con los componentes de la aplicación.
     *
     * @param fullText petición full-text
     * @param vectors  petición k-NN
     * @param hybrid   petición híbrida; se usa su búsqueda RRF
     * @param mapper   el mismo mapper JSON del cliente de Elasticsearch
     */
    public RankEvalBodies(FullTextRequestBuilder fullText, VectorRequestBuilder vectors,
            HybridRequestBuilder hybrid, JsonpMapper mapper) {
        this.fullText = fullText;
        this.vectors = vectors;
        this.hybrid = hybrid;
        this.mapper = mapper;
    }

    /**
     * El cuerpo de búsqueda de una consulta en una configuración.
     *
     * @param config configuración evaluada
     * @param alias  alias del catálogo
     * @param text   texto de la consulta
     * @param model  vector de 1024 de la consulta; se recorta al campo de la configuración
     * @return objeto con {@code query}, {@code knn} o {@code retriever}
     */
    public ObjectNode body(EvalConfig config, String alias, String text, float[] model) {
        SearchQuery query = config.query(text);
        JsonpSerializable request = switch (config.mode()) {
            case FULL_TEXT -> fullText.build(alias, query);
            case VECTOR -> vectors.build(alias, query, vector(config, model), EvalConfig.K);
            case HYBRID -> hybrid.build(alias, query, vector(config, model), EvalConfig.K)
                    .searches().getFirst().body();
        };
        return kept(json.readTree(JsonpUtils.toJsonString(request, mapper)));
    }

    private static float[] vector(EvalConfig config, float[] model) {
        return QueryVector.forField(model, config.vectorField());
    }

    private ObjectNode kept(JsonNode all) {
        ObjectNode body = json.createObjectNode();
        for (String name : KEPT) {
            if (all.has(name)) {
                body.set(name, all.get(name));
            }
        }
        return body;
    }
}
