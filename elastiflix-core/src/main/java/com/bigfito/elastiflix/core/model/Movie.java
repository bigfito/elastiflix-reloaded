package com.bigfito.elastiflix.core.model;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Una película del catálogo, con los 33 campos del mapping.
 *
 * <p>El metadato {@code _id} no es un campo: si el JSON lo trae, se ignora. {@code id} puede llegar
 * como número y se guarda como texto. Los vectores no participan en {@code equals}.</p>
 *
 * @param id                    identificador, igual al {@code _id} del documento
 * @param imdbId                id de IMDB
 * @param title                 título
 * @param originalTitle         título original
 * @param originalLanguage      idioma original
 * @param releaseDate           fecha de estreno
 * @param runtime               duración en minutos
 * @param status                estado de publicación
 * @param adult                 si es para adultos
 * @param video                 si es un video
 * @param tagline               eslogan
 * @param overview              sinopsis breve
 * @param plot                  sinopsis de la que salen los vectores
 * @param genres                géneros
 * @param keywords              palabras clave anidadas
 * @param cast                  reparto
 * @param belongsToCollection   colección, o {@code null}
 * @param budget                presupuesto
 * @param revenue               recaudación
 * @param popularity            popularidad
 * @param voteAverage           media de votos
 * @param voteCount             cantidad de votos
 * @param rating                clasificación
 * @param homepage              página oficial
 * @param posterPath            ruta del póster
 * @param backdropPath          ruta del fondo
 * @param productionCompanies   productoras
 * @param productionCountries   países
 * @param spokenLanguages       idiomas hablados
 * @param plot256               vector de 256
 * @param plot512               vector de 512
 * @param plot1024              vector de 1024
 * @param userScore             puntuación de usuario, si viene
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Movie(
        String id,
        String imdbId,
        String title,
        String originalTitle,
        String originalLanguage,
        LocalDate releaseDate,
        Integer runtime,
        String status,
        Boolean adult,
        Boolean video,
        String tagline,
        String overview,
        String plot,
        List<String> genres,
        List<MovieKeyword> keywords,
        List<String> cast,
        MovieCollection belongsToCollection,
        Double budget,
        Double revenue,
        Double popularity,
        Double voteAverage,
        Integer voteCount,
        String rating,
        String homepage,
        String posterPath,
        String backdropPath,
        List<String> productionCompanies,
        List<String> productionCountries,
        List<String> spokenLanguages,
        float[] plot256,
        float[] plot512,
        float[] plot1024,
        Double userScore) {
}
