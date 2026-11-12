package com.bigfito.elastiflix.eval;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Los mínimos de {@code eval/thresholds.json}: por configuración, el valor más bajo aceptado de cada métrica.
 *
 * <p>Formato: {@code {"knn_plot_1024": {"ndcg@10": 0.60, "mrr@10": 0.70}}}. Una configuración o métrica que no
 * aparece no se revisa.</p>
 *
 * @param minimums mínimo por configuración y métrica
 */
public record Thresholds(JsonNode minimums) {

    /** Ruta del archivo, relativa a la raíz del repositorio. */
    public static final Path DEFAULT = Path.of("eval", "thresholds.json");

    /**
     * Lee el archivo.
     *
     * @param path archivo JSON
     * @return los mínimos
     * @throws IllegalStateException si el archivo no existe
     */
    public static Thresholds read(Path path) {
        if (!Files.exists(path)) {
            throw new IllegalStateException("Falta " + path + ": sin mínimos no hay gate");
        }
        return new Thresholds(JsonMapper.builder().build().readTree(path.toFile()));
    }

    /**
     * Las métricas que quedaron por debajo de su mínimo.
     *
     * @param results resultados de la corrida
     * @return una línea por incumplimiento; vacía si todo pasa
     */
    public List<String> violations(List<ConfigResult> results) {
        List<String> violations = new ArrayList<>();
        for (ConfigResult result : results) {
            JsonNode config = minimums.path(result.config().key());
            for (Map.Entry<String, JsonNode> entry : config.properties()) {
                Metric metric = metric(entry.getKey());
                double measured = result.score(metric);
                double minimum = entry.getValue().asDouble();
                if (measured < minimum) {
                    violations.add(String.format(Locale.ROOT, "%s %s = %.3f, mínimo %.3f",
                            result.config().key(), metric.key(), measured, minimum));
                }
            }
        }
        return violations;
    }

    private static Metric metric(String key) {
        for (Metric metric : Metric.values()) {
            if (metric.key().equals(key)) {
                return metric;
            }
        }
        throw new IllegalStateException("Métrica desconocida en thresholds.json: " + key);
    }
}
