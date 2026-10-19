package com.bigfito.elastiflix.core.embedding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.EmbeddingException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.inference.DenseEmbeddingResult;
import co.elastic.clients.elasticsearch.inference.ElasticsearchInferenceClient;
import co.elastic.clients.elasticsearch.inference.InferenceRequest;
import co.elastic.clients.elasticsearch.inference.InferenceResponse;
import co.elastic.clients.json.JsonpMappingException;
import co.elastic.clients.util.ObjectBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La inferencia tiene que devolver un vector por cada texto pedido.
 */
class InferenceEmbeddingServiceTest {

    @Test
    @DisplayName("el servicio, con un cliente que devuelve menos vectores que sinopsis, responde EFX-EMB-004")
    void shouldRejectAShortBatchFromTheClient() throws Exception {
        // given
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        ElasticsearchInferenceClient inference = mock(ElasticsearchInferenceClient.class);
        when(client.inference()).thenReturn(inference);
        when(inference.inference(any(Function.class))).thenReturn(InferenceResponse.of(response -> response
                .result(result -> result.embeddings(List.of(DenseEmbeddingResult.of(row -> row.embedding(modelVector())))))));
        InferenceEmbeddingService service = new InferenceEmbeddingService(
                client, new ElastiflixProperties(null, null, null, null, null, null, null));

        // when / then
        assertThatThrownBy(() -> service.embedDocuments(List.of("una sinopsis", "otra sinopsis")))
                .isInstanceOf(EmbeddingException.class)
                .extracting(thrown -> ((EmbeddingException) thrown).errorCode())
                .isEqualTo(ErrorCode.INCOMPLETE_EMBEDDING_BATCH);
    }

    @Test
    @DisplayName("una respuesta de inferencia que el cliente no puede leer da EFX-EMB-001")
    void shouldTranslateAnUnreadableResponse() throws Exception {
        // given
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        ElasticsearchInferenceClient inference = mock(ElasticsearchInferenceClient.class);
        when(client.inference()).thenReturn(inference);
        when(inference.inference(any(Function.class)))
                .thenThrow(new JsonpMappingException("respuesta ilegible", null));
        InferenceEmbeddingService service = new InferenceEmbeddingService(
                client, new ElastiflixProperties(null, null, null, null, null, null, null));

        // when / then
        assertThatThrownBy(() -> service.embedQuery("titanic"))
                .isInstanceOf(EmbeddingException.class)
                .hasCauseInstanceOf(JsonpMappingException.class)
                .extracting(thrown -> ((EmbeddingException) thrown).errorCode())
                .isEqualTo(ErrorCode.INFERENCE_UNAVAILABLE);
    }

    @Test
    @DisplayName("embedQuery envía input_type=search y antepone «Query: » a la consulta")
    @SuppressWarnings("unchecked")
    void shouldSendTheSearchInputTypeAndTheQueryPrefix() throws Exception {
        // given
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        ElasticsearchInferenceClient inference = mock(ElasticsearchInferenceClient.class);
        when(client.inference()).thenReturn(inference);
        when(inference.inference(any(Function.class))).thenReturn(InferenceResponse.of(response -> response
                .result(result -> result.embeddings(List.of(DenseEmbeddingResult.of(row -> row
                        .embedding(modelVector())))))));
        InferenceEmbeddingService service = new InferenceEmbeddingService(
                client, new ElastiflixProperties(null, null, null, null, null, null, null));
        ArgumentCaptor<Function<InferenceRequest.Builder, ObjectBuilder<InferenceRequest>>> captor =
                ArgumentCaptor.forClass(Function.class);

        // when
        service.embedQuery("titanic");

        // then
        verify(inference).inference(captor.capture());
        InferenceRequest sent = captor.getValue().apply(new InferenceRequest.Builder()).build();
        assertThat(sent.inputType()).isEqualTo("search");
        assertThat(sent.input()).containsExactly("Query: titanic");
    }

    /** Un vector válido de 1024 dimensiones: así falla solo el tamaño del lote. */
    private static List<Float> modelVector() {
        List<Float> values = new ArrayList<>(Collections.nCopies(1024, 0f));
        values.set(0, 1f);
        return values;
    }
}
