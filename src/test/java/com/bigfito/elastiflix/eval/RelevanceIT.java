package com.bigfito.elastiflix.eval;

import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.catalog.CatalogName;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.embedding.EmbeddingService;
import com.bigfito.elastiflix.search.strategy.FullTextRequestBuilder;
import com.bigfito.elastiflix.search.strategy.HybridRequestBuilder;
import com.bigfito.elastiflix.search.strategy.VectorRequestBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Harness de relevancia: mide con {@code _rank_eval} cada configuración sobre {@code eval/judgments.jsonl} y
 * falla si una métrica baja de su mínimo en {@code eval/thresholds.json}.
 *
 * <p>Solo lee. No crea ni borra índices. Los cuerpos de búsqueda salen de los armadores de la aplicación y el
 * vector de cada consulta, de su servicio de inferencia (una llamada por consulta). Escribe
 * {@code target/eval/report.md} y {@code target/eval/pending.jsonl}. Los mínimos dejan margen porque el k-NN es
 * aproximado: al reconstruir el índice, una película al borde puede moverse un lugar. Si el catálogo no tiene
 * 6 899 películas, la prueba se omite.</p>
 */
@SpringBootTest
class RelevanceIT {

    private static final Logger LOG = LoggerFactory.getLogger(RelevanceIT.class);
    private static final long CATALOG_COUNT = 6_899L;
    private static final Path OUTPUT = Path.of("target", "eval");

    private final EmbeddingService embeddings;
    private final RankEvalBodies bodies;
    private final RankEvalClient client;
    private final CatalogIndex catalog;
    private final ElastiflixProperties properties;

    /**
     * Spring no inyecta el constructor de una prueba si no lleva {@code @Autowired}.
     *
     * @param embeddings vector de cada consulta
     * @param fullText   petición full-text de la aplicación
     * @param vectors    petición k-NN de la aplicación
     * @param hybrid     petición híbrida de la aplicación
     * @param mapper     mapper JSON del cliente de Elasticsearch
     * @param rest       cliente de bajo nivel para {@code _rank_eval}
     * @param catalog    conteo y alias, solo lectura
     * @param properties alias configurado
     */
    @Autowired
    RelevanceIT(EmbeddingService embeddings, FullTextRequestBuilder fullText, VectorRequestBuilder vectors,
            HybridRequestBuilder hybrid, JsonpMapper mapper, Rest5Client rest, CatalogIndex catalog,
            ElastiflixProperties properties) {
        this.embeddings = embeddings;
        this.bodies = new RankEvalBodies(fullText, vectors, hybrid, mapper);
        this.client = new RankEvalClient(rest);
        this.catalog = catalog;
        this.properties = properties;
    }

    @Test
    @DisplayName("cada configuración alcanza sus mínimos de relevancia")
    void shouldMeetTheRelevanceThresholds() throws IOException {
        String alias = properties.index().alias();
        long counted = catalog.count(alias);
        Assumptions.assumeTrue(counted == CATALOG_COUNT, () ->
                "El catálogo cambió: se esperaban " + CATALOG_COUNT + " películas y hay " + counted);
        String index = concreteIndex(alias);

        List<Judgment> judgments = JudgmentFile.read(JudgmentFile.DEFAULT);
        Map<String, float[]> vectors = new LinkedHashMap<>();
        judgments.forEach(judgment -> vectors.put(judgment.id(), embeddings.embedQuery(judgment.text())));

        List<ConfigResult> results = new ArrayList<>();
        for (EvalConfig config : EvalConfig.values()) {
            results.add(evaluate(config, alias, index, judgments, vectors));
        }
        int pending = write(alias, index, judgments, results);

        List<String> violations = Thresholds.read(Thresholds.DEFAULT).violations(results);
        LOG.info("Relevancia: {} configuraciones, {} películas sin calificar, informe en {}",
                results.size(), pending, OUTPUT.resolve("report.md").toAbsolutePath());
        assertThat(violations).as("métricas por debajo de eval/thresholds.json").isEmpty();
    }

    private String concreteIndex(String alias) {
        if (catalog.describe(alias) instanceof CatalogName.Alias(String name, String concrete)) {
            return concrete;
        }
        throw new IllegalStateException(alias + " no es un alias: los juicios necesitan el índice concreto");
    }

    private ConfigResult evaluate(EvalConfig config, String alias, String index, List<Judgment> judgments,
            Map<String, float[]> vectors) {
        List<RankEvalClient.RatedRequest> requests = judgments.stream()
                .map(judgment -> new RankEvalClient.RatedRequest(judgment,
                        bodies.body(config, alias, judgment.text(), vectors.get(judgment.id()))))
                .toList();
        Map<Metric, RankEvalResult> metrics = new EnumMap<>(Metric.class);
        for (Metric metric : Metric.values()) {
            metrics.put(metric, client.evaluate(alias, index, requests, metric));
        }
        return new ConfigResult(config, metrics);
    }

    private int write(String alias, String index, List<Judgment> judgments, List<ConfigResult> results)
            throws IOException {
        List<String> unrated = RelevanceReport.unratedIds(judgments, results);
        List<String> pending = RelevanceReport.pendingLines(judgments, results,
                unrated.isEmpty() ? Map.of() : client.describe(alias, unrated));
        Files.createDirectories(OUTPUT);
        Files.writeString(OUTPUT.resolve("report.md"),
                RelevanceReport.markdown(index, judgments, results, pending.size()), StandardCharsets.UTF_8);
        Files.write(OUTPUT.resolve("pending.jsonl"), pending, StandardCharsets.UTF_8);
        return pending.size();
    }
}
