package com.bigfito.elastiflix.core.embedding;

import java.util.List;

import com.bigfito.elastiflix.core.error.EmbeddingException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La inferencia tiene que devolver un vector por cada texto pedido.
 */
class InferenceEmbeddingServiceTest {

    @Test
    @DisplayName("si faltan vectores, EFX-EMB-004")
    void shouldRejectWhenTheVectorCountDoesNotMatch() {
        assertThatThrownBy(() -> InferenceEmbeddingService.requireSameSize(List.of("una", "otra"), List.of(vector())))
                .isInstanceOf(EmbeddingException.class)
                .extracting(thrown -> ((EmbeddingException) thrown).errorCode())
                .isEqualTo(ErrorCode.INCOMPLETE_EMBEDDING_BATCH);
    }

    @Test
    @DisplayName("si hay un vector por texto, los devuelve")
    void shouldKeepTheVectorsWhenTheCountMatches() {
        List<float[]> vectors = List.of(vector());

        assertThat(InferenceEmbeddingService.requireSameSize(List.of("una"), vectors)).isSameAs(vectors);
    }

    private static float[] vector() {
        return new float[] {1f};
    }
}
