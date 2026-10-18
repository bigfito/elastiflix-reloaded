package com.bigfito.elastiflix.ingest.vector;

import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.embedding.EmbeddingService;
import com.bigfito.elastiflix.core.embedding.VectorProjector;
import com.bigfito.elastiflix.core.error.EmbeddingException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;

import org.springframework.stereotype.Component;

/**
 * Pide el vector de 1024 de cada sinopsis y deriva los de 512 y 256.
 *
 * <p>No usa los vectores que pudiera traer el archivo: esos ya se descartaron. Si la inferencia
 * falla, la excepción {@code EFX-EMB-*} sale sin cambios para que la importación no publique.</p>
 */
@Component
public final class PlotVectorizer {

    private static final int MEDIUM = 512;
    private static final int SMALL = 256;

    private final EmbeddingService embeddings;

    /**
     * Crea el vectorizador con el servicio de inferencia.
     *
     * @param embeddings servicio que vectoriza sinopsis, no consultas
     */
    public PlotVectorizer(EmbeddingService embeddings) {
        this.embeddings = embeddings;
    }

    /**
     * Devuelve las películas con sus tres vectores. Una lista vacía no llama al servicio.
     *
     * @param movies películas ya validadas, en orden
     * @return las mismas películas, con vectores nuevos
     */
    public List<Movie> apply(List<Movie> movies) {
        if (movies.isEmpty()) {
            return List.of();
        }
        List<float[]> vectors = embeddings.embedDocuments(plots(movies));
        return attach(movies, vectors);
    }

    private static List<String> plots(List<Movie> movies) {
        List<String> plots = new ArrayList<>();
        for (Movie movie : movies) {
            plots.add(movie.plot());
        }
        return plots;
    }

    private static List<Movie> attach(List<Movie> movies, List<float[]> vectors) {
        if (vectors == null || vectors.size() != movies.size()) {
            throw new EmbeddingException(ErrorCode.UNEXPECTED_VECTOR);
        }
        List<Movie> ready = new ArrayList<>();
        for (int index = 0; index < movies.size(); index++) {
            ready.add(withVector(movies.get(index), vectors.get(index)));
        }
        return ready;
    }

    private static Movie withVector(Movie movie, float[] plot1024) {
        return movie.withPlotVectors(
                VectorProjector.project(plot1024, SMALL),
                VectorProjector.project(plot1024, MEDIUM),
                plot1024);
    }
}
