package com.bigfito.elastiflix.search.result;

import java.time.LocalDate;
import java.util.List;

import com.bigfito.elastiflix.search.strategy.ComponentScore;

/**
 * Una película de la página, sin vectores.
 *
 * <p>En full-text y en vectorial solo viene {@code score}. En híbrida {@code lexical} y
 * {@code vector} dicen la puntuación y la posición de cada lista, y {@code rrf} es la fusión.</p>
 *
 * @param id            identificador
 * @param title         título
 * @param originalTitle título original
 * @param plot          sinopsis
 * @param posterPath    ruta del póster; la web arma la URL
 * @param releaseDate   fecha de estreno, o {@code null}
 * @param runtime       duración en minutos, o {@code null}
 * @param genres        géneros; lista vacía si no hay
 * @param voteAverage   media de votos, o {@code null}
 * @param voteCount     cantidad de votos, o {@code null}
 * @param rating        clasificación, o {@code null}
 * @param score         puntuación de la lista principal, o {@code null}
 * @param lexical       BM25 y posición, o {@code null} fuera de la híbrida
 * @param vector        similitud y posición, o {@code null} fuera de la híbrida
 * @param rrf           puntuación RRF, o {@code null} fuera de la híbrida
 */
public record SearchHit(
        String id,
        String title,
        String originalTitle,
        String plot,
        String posterPath,
        LocalDate releaseDate,
        Integer runtime,
        List<String> genres,
        Double voteAverage,
        Integer voteCount,
        String rating,
        Double score,
        ComponentScore lexical,
        ComponentScore vector,
        Double rrf) {

    /**
     * Copia los géneros. Una lista ausente queda vacía.
     */
    public SearchHit {
        if (genres == null) {
            genres = List.of();
        } else {
            genres = List.copyOf(genres);
        }
    }
}
