package com.bigfito.elastiflix.core.embedding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.EmbeddingException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.inference.DenseEmbeddingResult;
import co.elastic.clients.elasticsearch.inference.InferenceRequest;
import co.elastic.clients.elasticsearch.inference.InferenceResponse;
import co.elastic.clients.elasticsearch.inference.InferenceResult;
import co.elastic.clients.elasticsearch.inference.TaskType;
import co.elastic.clients.json.JsonpMappingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Pide los vectores al endpoint de inferencia de Elasticsearch.
 *
 * <p>Ese endpoint es de tipo {@code embedding}. Un {@code input_type} vacío no se envía.</p>
 */
@Component
public class InferenceEmbeddingService implements EmbeddingService {

    /** Textos por llamada a {@code _inference}. No es una propiedad: el diseño lo fijó en 16. */
    static final int BATCH_SIZE = 16;

    private static final int MODEL_DIMENSIONS = VectorProjector.MODEL_DIMENSIONS;

    /** Consultas: fijo, reproduce {@code query-vector-titanic.json}. */
    private static final String QUERY_INPUT_TYPE = "search";

    /** Prefijo de las consultas, fijo. */
    private static final String QUERY_PREFIX = "Query: ";
    private static final Logger LOG = LoggerFactory.getLogger(InferenceEmbeddingService.class);

    private final ElasticsearchClient client;
    private final ElastiflixProperties properties;

    /**
     * Crea el gateway con el cliente y el id del endpoint.
     *
     * @param client     cliente de Elasticsearch
     * @param properties de aquí salen el endpoint y la combinación de documento
     */
    public InferenceEmbeddingService(ElasticsearchClient client, ElastiflixProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public float[] embedQuery(String text) {
        return one(text, QUERY_INPUT_TYPE, QUERY_PREFIX);
    }

    @Override
    public List<float[]> embedDocuments(List<String> texts) {
        ElastiflixProperties.Embedding embedding = properties.embedding();
        return embedAll(texts, embedding.documentInputType(), embedding.documentPrefix());
    }

    /**
     * Genera un vector de calibración. Un {@code inputType} o {@code prefix} nulo no se envía.
     *
     * @param text      sinopsis cruda
     * @param inputType {@code ingest}, {@code search}, o {@code null} para omitirlo
     * @param prefix    texto a anteponer, o {@code null} para no anteponer nada
     * @return vector de 1024 dimensiones
     */
    float[] embedForCalibration(String text, String inputType, String prefix) {
        return one(text, inputType, prefix);
    }

    private float[] one(String text, String inputType, String prefix) {
        return call(List.of(prepare(text, prefix)), inputType).getFirst();
    }

    private List<float[]> embedAll(List<String> texts, String inputType, String prefix) {
        if (texts == null) {
            throw new EmbeddingException(ErrorCode.EMPTY_EMBEDDING_TEXT);
        }
        List<float[]> vectors = new ArrayList<>();
        for (int start = 0; start < texts.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, texts.size());
            vectors.addAll(call(prepareAll(texts.subList(start, end), prefix), inputType));
        }
        return List.copyOf(vectors);
    }

    private List<float[]> call(List<String> prepared, String inputType) {
        logBatch(prepared.size());
        try {
            InferenceResponse response = client.inference()
                    .inference(request -> requestFor(request, prepared, inputType));
            List<float[]> vectors = vectorsOf(response.result());
            if (vectors.size() != prepared.size()) {
                // Un vector por cada texto del lote; si no, EFX-EMB-004.
                throw new EmbeddingException(ErrorCode.INCOMPLETE_EMBEDDING_BATCH);
            }
            return vectors;
        } catch (IOException | ElasticsearchException | JsonpMappingException exception) {
            // Una respuesta que el cliente no puede leer también es un endpoint que falla: EFX-EMB-001.
            LOG.error("{} en {}", ErrorCode.INFERENCE_UNAVAILABLE.code(), properties.inference().id());
            throw new EmbeddingException(ErrorCode.INFERENCE_UNAVAILABLE, exception);
        }
    }

    private InferenceRequest.Builder requestFor(
            InferenceRequest.Builder request, List<String> prepared, String inputType) {
        request.inferenceId(properties.inference().id())
                .taskType(TaskType.Embedding)
                .input(prepared);
        if (inputType != null && !inputType.isBlank()) {
            request.inputType(inputType);
        }
        return request;
    }

    private static List<String> prepareAll(List<String> texts, String prefix) {
        List<String> prepared = new ArrayList<>();
        for (String text : texts) {
            prepared.add(prepare(text, prefix));
        }
        return prepared;
    }

    private static String prepare(String text, String prefix) {
        requireText(text);
        if (prefix == null || prefix.isBlank()) {
            return text;
        }
        return prefix + text;
    }

    private static void requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new EmbeddingException(ErrorCode.EMPTY_EMBEDDING_TEXT);
        }
    }

    private static List<float[]> vectorsOf(InferenceResult result) {
        List<float[]> vectors = new ArrayList<>();
        for (DenseEmbeddingResult row : rowsOf(result)) {
            vectors.add(toArray(row.embedding()));
        }
        return vectors;
    }

    private static List<DenseEmbeddingResult> rowsOf(InferenceResult result) {
        if (result.isTextEmbedding()) {
            return result.textEmbedding();
        }
        if (result.isEmbeddings()) {
            return result.embeddings();
        }
        throw new EmbeddingException(ErrorCode.UNEXPECTED_VECTOR);
    }

    private static float[] toArray(List<Float> values) {
        if (values == null || values.size() != MODEL_DIMENSIONS) {
            throw new EmbeddingException(ErrorCode.UNEXPECTED_VECTOR);
        }
        float[] vector = new float[MODEL_DIMENSIONS];
        for (int index = 0; index < MODEL_DIMENSIONS; index++) {
            Float value = values.get(index);
            // Un NaN o un infinito llegaría a Elasticsearch y volvería como un 400 (EFX-ES-007).
            if (value == null || !Float.isFinite(value)) {
                throw new EmbeddingException(ErrorCode.UNEXPECTED_VECTOR);
            }
            vector[index] = value;
        }
        return vector;
    }

    private void logBatch(int size) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Lote de inferencia: tamaño={} endpoint={}", size, properties.inference().id());
        }
    }
}
