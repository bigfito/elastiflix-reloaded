package com.bigfito.elastiflix.ingest.vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.embedding.EmbeddingService;
import com.bigfito.elastiflix.core.embedding.VectorProjector;
import com.bigfito.elastiflix.core.error.EmbeddingException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Los vectores se piden para todas las sinopsis, también si el archivo ya traía alguno.
 */
@ExtendWith(MockitoExtension.class)
class PlotVectorizerTest {

    @Mock
    private EmbeddingService embeddings;

    @InjectMocks
    private PlotVectorizer vectorizer;

    @Test
    @DisplayName("reemplaza los vectores que ya venían y deriva 512 y 256")
    void shouldVectorizeEveryPlotIncludingOnesThatAlreadyHadVectors() {
        float[] previous = filled(1024, 9f);
        Movie stale = movie("1", "una").withPlotVectors(filled(256, 9f), filled(512, 9f), previous);
        float[] generated = filled(1024, 0.02f);
        when(embeddings.embedDocuments(List.of("una", "otra"))).thenReturn(List.of(generated, generated));

        List<Movie> ready = vectorizer.apply(List.of(stale, movie("2", "otra")));

        verify(embeddings).embedDocuments(List.of("una", "otra"));
        assertThat(ready.getFirst().plot1024()).isEqualTo(generated);
        assertThat(ready.getFirst().plot512()).isEqualTo(VectorProjector.project(generated, 512));
        assertThat(ready.getFirst().plot256()).isEqualTo(VectorProjector.project(generated, 256));
        assertThat(ready.get(1).plot1024()).isEqualTo(generated);
        assertThat(ready.getFirst().plot1024()).isNotEqualTo(previous);
    }

    @Test
    @DisplayName("si la inferencia falla, la excepción sale y no hay nada que publicar")
    void shouldStopBeforePublishingWhenInferenceFails() {
        when(embeddings.embedDocuments(List.of("una")))
                .thenThrow(new EmbeddingException(ErrorCode.INFERENCE_UNAVAILABLE));
        List<String> published = new ArrayList<>();

        assertThatThrownBy(() -> {
            vectorizer.apply(List.of(movie("1", "una")));
            published.add("publicado");
        }).isInstanceOf(EmbeddingException.class)
                .extracting(exception -> ((EmbeddingException) exception).errorCode())
                .isEqualTo(ErrorCode.INFERENCE_UNAVAILABLE);
        assertThat(published).isEmpty();
    }

    @Test
    @DisplayName("una lista vacía no llama al servicio")
    void shouldSkipTheServiceWhenThereAreNoMovies() {
        assertThat(vectorizer.apply(List.of())).isEmpty();
        verifyNoInteractions(embeddings);
    }

    private static Movie movie(String id, String plot) {
        String json = "{\"id\":\"" + id + "\",\"title\":\"T\",\"plot\":\"" + plot + "\"}";
        return MovieJsonMapper.create().readValue(json, Movie.class);
    }

    private static float[] filled(int size, float value) {
        float[] vector = new float[size];
        Arrays.fill(vector, value);
        return vector;
    }
}
