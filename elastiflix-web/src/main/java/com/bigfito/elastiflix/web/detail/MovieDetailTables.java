package com.bigfito.elastiflix.web.detail;

import java.util.List;
import java.util.Locale;

import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.core.model.MovieCollection;
import com.bigfito.elastiflix.core.text.Thousands;
import com.bigfito.elastiflix.web.layout.PosterUrls;

/**
 * Genera las tablas de datos estructurados para la ficha técnica, producción, finanzas, colección e imágenes.
 */
final class MovieDetailTables {

    private MovieDetailTables() {
    }

    /**
     * Construye las filas de datos de la ficha técnica.
     *
     * @param m película
     * @return filas de la ficha técnica
     */
    static List<FactRow> facts(Movie m) {
        String imdbHref = m.imdbId() != null && !m.imdbId().isBlank()
                ? "https://www.imdb.com/title/" + m.imdbId() + "/" : "";
        String runtimeStr = m.runtime() != null ? m.runtime() + " min" : null;
        String releaseDateStr = m.releaseDate() != null ? m.releaseDate().toString() : null;
        return List.of(
                FactRow.text("Identificador", "id", m.id()),
                FactRow.link("IMDb", "imdb_id", m.imdbId(), imdbHref),
                FactRow.text("Título", "title", m.title()),
                FactRow.text("Título original", "original_title", m.originalTitle()),
                FactRow.text("Lema", "tagline", m.tagline()),
                FactRow.text("Idioma original", "original_language", m.originalLanguage()),
                FactRow.text("Fecha de estreno", "release_date", releaseDateStr),
                FactRow.text("Duración", "runtime", runtimeStr),
                FactRow.text("Estado", "status", m.status()),
                FactRow.text("Clasificación", "rating", m.rating()),
                FactRow.yesNo("Contenido para adultos", "adult", m.adult()),
                FactRow.yesNo("Video", "video", m.video()),
                FactRow.link("Sitio oficial", "homepage", m.homepage(), m.homepage()));
    }

    /**
     * Construye las filas de producción.
     *
     * @param m película
     * @return filas de producción
     */
    static List<FactRow> production(Movie m) {
        return List.of(
                FactRow.list("Productoras", "production_companies", m.productionCompanies()),
                FactRow.list("Países de producción", "production_countries", m.productionCountries()),
                FactRow.list("Idiomas hablados", "spoken_languages", m.spokenLanguages()));
    }

    /**
     * Construye las filas de finanzas y puntuaciones.
     *
     * @param m película
     * @return filas de finanzas y puntuaciones
     */
    static List<FactRow> numbers(Movie m) {
        String voteStr = m.voteCount() != null ? Thousands.of(m.voteCount()) : null;
        return List.of(
                FactRow.money("Presupuesto", "budget", m.budget()),
                FactRow.money("Recaudación", "revenue", m.revenue()),
                FactRow.text("Calificación promedio", "vote_average", voteAverage(m.voteAverage())),
                FactRow.text("Votos", "vote_count", voteStr),
                FactRow.text("Popularidad", "popularity", popularity(m.popularity())),
                FactRow.text("Puntuación de usuarios", "user_score", userScore(m.userScore())));
    }

    /**
     * Construye las filas descriptivas de la colección.
     *
     * @param coll colección de películas
     * @return filas de colección
     */
    static List<FactRow> collection(MovieCollection coll) {
        if (coll == null) {
            return List.of();
        }
        return List.of(
                FactRow.text("Identificador", "id", coll.id() != null ? String.valueOf(coll.id()) : null),
                FactRow.text("Nombre", "name", coll.name()),
                FactRow.text("Póster", "poster_path", coll.posterPath()),
                FactRow.text("Imagen de fondo", "backdrop_path", coll.backdropPath()));
    }

    /**
     * Construye las filas con enlaces a imágenes en el CDN de TMDB.
     *
     * @param m       película
     * @param posters servicio de URLs de pósters
     * @return filas de imágenes
     */
    static List<FactRow> images(Movie m, PosterUrls posters) {
        String posterUrl = posters.url(m.posterPath());
        String backdropUrl = posters.url(m.backdropPath());
        return List.of(
                FactRow.link("Póster", "poster_path", m.posterPath(), posterUrl),
                FactRow.link("Imagen de fondo", "backdrop_path", m.backdropPath(), backdropUrl));
    }

    static String voteAverage(Double avg) {
        if (avg == null) {
            return "—";
        }
        return String.format(Locale.US, "%.2f", avg);
    }

    static String popularity(Double pop) {
        if (pop == null) {
            return "—";
        }
        return String.format(Locale.US, "%.1f", pop);
    }

    static String voteCount(Integer count) {
        if (count == null) {
            return "0";
        }
        return Thousands.of(count);
    }

    private static String userScore(Double score) {
        if (score == null) {
            return null;
        }
        return String.format(Locale.US, "%.1f", score);
    }
}
