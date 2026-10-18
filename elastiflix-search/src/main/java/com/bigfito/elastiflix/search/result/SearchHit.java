package com.bigfito.elastiflix.search.result;

import java.time.LocalDate;

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
 * @param posterPath    ruta del póster; la Fase 4 arma la URL
 * @param releaseDate   fecha de estreno, o {@code null}
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
        Double score,
        ComponentScore lexical,
        ComponentScore vector,
        Double rrf) {
}
