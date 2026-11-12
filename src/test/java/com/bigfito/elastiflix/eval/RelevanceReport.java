package com.bigfito.elastiflix.eval;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Escribe el informe en Markdown y la lista de películas por calificar.
 */
public final class RelevanceReport {

    private static final int OVERVIEW_CHARS = 400;
    private static final int TOP_FIVE = 5;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final Map<String, Predicate<Judgment>> SUBSETS = subsets();

    private RelevanceReport() {
    }

    /**
     * El informe completo.
     *
     * @param index     índice concreto evaluado
     * @param judgments consultas
     * @param results   una entrada por configuración
     * @param pending   cuántas películas del top 10 no tienen juicio
     * @return Markdown
     */
    public static String markdown(String index, List<Judgment> judgments, List<ConfigResult> results, int pending) {
        StringBuilder out = new StringBuilder();
        out.append("# Informe de relevancia\n\n")
                .append("Índice `").append(index).append("` · ").append(judgments.size()).append(" consultas · ")
                .append("relevante = calificación 2 o 3 · profundidad 10\n\n");
        averages(out, judgments, results);
        bySubset(out, Metric.NDCG, judgments, results);
        bySubset(out, Metric.MRR, judgments, results);
        targets(out, judgments, results);
        out.append("## Sin calificar\n\n").append(pending)
                .append(" películas aparecen en algún top 10 sin juicio. Están en `target/eval/pending.jsonl`; ")
                .append("mientras no se califiquen cuentan como no relevantes.\n");
        return out.toString();
    }

    /**
     * Películas del top 10 de alguna configuración que no tienen juicio, una por línea JSON.
     *
     * @param judgments   consultas
     * @param results     resultados
     * @param descriptions título, año y resumen de cada película, por id
     * @return líneas JSON, agrupadas por consulta
     */
    public static List<String> pendingLines(List<Judgment> judgments, List<ConfigResult> results,
            Map<String, JsonNode> descriptions) {
        List<String> lines = new ArrayList<>();
        for (Judgment judgment : judgments) {
            unrated(judgment, results).forEach((movie, seen) ->
                    lines.add(JSON.writeValueAsString(line(judgment, movie, seen, descriptions.get(movie)))));
        }
        return lines;
    }

    /**
     * Ids de todas las películas sin juicio, sin repetir.
     *
     * @param judgments consultas
     * @param results   resultados
     * @return ids en orden de aparición
     */
    public static List<String> unratedIds(List<Judgment> judgments, List<ConfigResult> results) {
        Set<String> ids = new LinkedHashSet<>();
        judgments.forEach(judgment -> ids.addAll(unrated(judgment, results).keySet()));
        return List.copyOf(ids);
    }

    private static Map<String, List<String>> unrated(Judgment judgment, List<ConfigResult> results) {
        Map<String, List<String>> seen = new LinkedHashMap<>();
        for (ConfigResult result : results) {
            List<RankEvalResult.RatedHit> hits = result.hits(judgment).hits();
            for (int i = 0; i < hits.size(); i++) {
                RankEvalResult.RatedHit hit = hits.get(i);
                if (!judgment.ratings().containsKey(hit.movieId())) {
                    seen.computeIfAbsent(hit.movieId(), id -> new ArrayList<>())
                            .add(result.config().key() + "#" + (i + 1));
                }
            }
        }
        return seen;
    }

    private static ObjectNode line(Judgment judgment, String movie, List<String> seen, JsonNode source) {
        ObjectNode line = JSON.createObjectNode();
        line.put("query", judgment.id()).put("text", judgment.text()).put("movie", movie);
        JsonNode described = source == null ? JSON.createObjectNode() : source;
        line.put("title", described.path("title").asString(""));
        line.put("year", described.path("release_date").asString("").replaceFirst("-.*", ""));
        String overview = described.path("overview").asString("");
        line.put("overview", overview.length() > OVERVIEW_CHARS ? overview.substring(0, OVERVIEW_CHARS) : overview);
        seen.forEach(line.putArray("seen_in")::add);
        return line;
    }

    private static void averages(StringBuilder out, List<Judgment> judgments, List<ConfigResult> results) {
        out.append("## Promedio de las ").append(judgments.size()).append(" consultas\n\n")
                .append("| Configuración | MRR@10 | P@10 | Recall@10 | nDCG@10 | Buscada en 1.º | Buscada en top 5 ")
                .append("| Buscada en top 10 |\n")
                .append("|---|---|---|---|---|---|---|---|\n");
        for (ConfigResult result : results) {
            long first = judgments.stream().filter(j -> result.targetRank(j) == 1).count();
            long five = judgments.stream().filter(j -> inTop(result.targetRank(j), TOP_FIVE)).count();
            long top = judgments.stream().filter(j -> result.targetRank(j) > 0).count();
            out.append("| `").append(result.config().key()).append("` | ")
                    .append(number(result.score(Metric.MRR))).append(" | ")
                    .append(number(result.score(Metric.PRECISION))).append(" | ")
                    .append(number(result.score(Metric.RECALL))).append(" | ")
                    .append(number(result.score(Metric.NDCG))).append(" | ")
                    .append(first).append(" de ").append(judgments.size()).append(" | ")
                    .append(five).append(" de ").append(judgments.size()).append(" | ")
                    .append(top).append(" de ").append(judgments.size()).append(" |\n");
        }
        out.append('\n');
    }

    private static boolean inTop(int rank, int depth) {
        return rank > 0 && rank <= depth;
    }

    private static void bySubset(StringBuilder out, Metric metric, List<Judgment> judgments,
            List<ConfigResult> results) {
        out.append("## ").append(metric.key()).append(" por tipo de consulta\n\n| Configuración |");
        SUBSETS.forEach((name, subset) -> out.append(' ').append(name).append(" (")
                .append(judgments.stream().filter(subset).count()).append(") |"));
        out.append("\n|---|").append("---|".repeat(SUBSETS.size())).append('\n');
        for (ConfigResult result : results) {
            out.append("| `").append(result.config().key()).append("` |");
            SUBSETS.values().forEach(subset ->
                    out.append(' ').append(number(result.score(metric, judgments, subset))).append(" |"));
            out.append('\n');
        }
        out.append('\n');
    }

    private static void targets(StringBuilder out, List<Judgment> judgments, List<ConfigResult> results) {
        out.append("## Posición de la película buscada (— = fuera del top 10)\n\n| Consulta | Texto |");
        results.forEach(result -> out.append(" `").append(result.config().key()).append("` |"));
        out.append("\n|---|---|").append("---|".repeat(results.size())).append('\n');
        for (Judgment judgment : judgments) {
            out.append("| ").append(judgment.id()).append(" | ").append(judgment.text()).append(" |");
            for (ConfigResult result : results) {
                int rank = result.targetRank(judgment);
                out.append(' ').append(rank == 0 ? "—" : String.valueOf(rank)).append(" |");
            }
            out.append('\n');
        }
        out.append('\n');
    }

    private static String number(double value) {
        return Double.isNaN(value) ? "—" : String.format(Locale.ROOT, "%.3f", value);
    }

    private static Map<String, Predicate<Judgment>> subsets() {
        Function<String, Predicate<Judgment>> lang = code -> judgment -> judgment.lang().equals(code);
        Function<String, Predicate<Judgment>> kind = code -> judgment -> judgment.kind().equals(code);
        Map<String, Predicate<Judgment>> subsets = new LinkedHashMap<>();
        subsets.put("es · trama", lang.apply("es").and(kind.apply("plot")));
        subsets.put("en · trama", lang.apply("en").and(kind.apply("plot")));
        subsets.put("es · título", lang.apply("es").and(kind.apply("title")));
        subsets.put("en · título", lang.apply("en").and(kind.apply("title")));
        return subsets;
    }
}
