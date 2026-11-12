package com.bigfito.elastiflix.eval;

import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Response;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Llama a {@code _rank_eval} y a una búsqueda por ids con el cliente de bajo nivel.
 *
 * <p>El cliente tipado no sirve aquí: su {@code RankEvalQuery} solo admite {@code query} y {@code size}, y el
 * harness necesita {@code knn} y {@code retriever}. Solo lee; no escribe en ningún índice.</p>
 */
public final class RankEvalClient {

    private final Rest5Client client;
    private final JsonMapper json = JsonMapper.builder().build();

    /**
     * Crea el cliente.
     *
     * @param client cliente de bajo nivel que configura Spring Boot con la URL y la API key
     */
    public RankEvalClient(Rest5Client client) {
        this.client = client;
    }

    /**
     * Una consulta lista para evaluar.
     *
     * @param judgment consulta y juicios
     * @param body     cuerpo de búsqueda que armó la aplicación
     */
    public record RatedRequest(Judgment judgment, ObjectNode body) {
    }

    /**
     * Evalúa todas las consultas con una métrica.
     *
     * @param alias         alias del catálogo
     * @param concreteIndex índice al que apunta el alias; los juicios lo necesitan, no aceptan el alias
     * @param requests      consultas y cuerpos
     * @param metric        métrica
     * @return promedio y detalle
     */
    public RankEvalResult evaluate(String alias, String concreteIndex, List<RatedRequest> requests, Metric metric) {
        ObjectNode body = json.createObjectNode();
        ArrayNode items = body.putArray("requests");
        for (RatedRequest rated : requests) {
            ObjectNode item = items.addObject();
            item.put("id", rated.judgment().id());
            item.set("request", rated.body());
            ArrayNode ratings = item.putArray("ratings");
            rated.judgment().ratings().forEach((movie, rating) -> ratings.addObject()
                    .put("_index", concreteIndex).put("_id", movie).put("rating", rating));
        }
        body.set("metric", metric.spec());
        return RankEvalResult.parse(post("/" + alias + "/_rank_eval", body));
    }

    /**
     * Título, año y resumen de unas películas, para calificarlas.
     *
     * @param alias alias del catálogo
     * @param ids   ids de película
     * @return cada id con su {@code _source} reducido
     */
    public Map<String, JsonNode> describe(String alias, List<String> ids) {
        ObjectNode body = json.createObjectNode();
        body.put("size", ids.size());
        body.putArray("_source").add("title").add("release_date").add("overview");
        ArrayNode values = body.putObject("query").putObject("ids").putArray("values");
        ids.forEach(values::add);
        Map<String, JsonNode> found = new HashMap<>();
        for (JsonNode hit : post("/" + alias + "/_search", body).path("hits").path("hits")) {
            found.put(hit.path("_id").asString(), hit.path("_source"));
        }
        return found;
    }

    private JsonNode post(String endpoint, ObjectNode body) {
        Request request = new Request("POST", endpoint);
        request.setJsonEntity(json.writeValueAsString(body));
        try {
            Response response = client.performRequest(request);
            return json.readTree(EntityUtils.toString(response.getEntity()));
        } catch (IOException e) {
            throw new UncheckedIOException("Falló " + endpoint + ": " + e.getMessage(), e);
        } catch (ParseException e) {
            throw new IllegalStateException("Respuesta ilegible de " + endpoint, e);
        }
    }
}
