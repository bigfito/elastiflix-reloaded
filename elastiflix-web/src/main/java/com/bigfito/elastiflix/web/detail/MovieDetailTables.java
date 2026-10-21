package com.bigfito.elastiflix.web.detail;

import java.util.List;
import java.util.Locale;

import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.core.model.MovieCollection;
import com.bigfito.elastiflix.core.text.Thousands;
import com.bigfito.elastiflix.web.layout.PosterUrls;

import com.bigfito.elastiflix.web.locale.UiMessages;

/**
 * Genera las tablas de datos estructurados para la ficha técnica, producción, finanzas, colección e imágenes.
 */
final class MovieDetailTables {

    private MovieDetailTables() {
    }

    /**
     * Construye las filas de datos de la ficha técnica en español.
     *
     * @param m película
     * @return filas de la ficha técnica
     */
    static List<FactRow> facts(Movie m) {
        return facts(m, Locale.forLanguageTag("es"), null);
    }

    /**
     * Construye las filas de datos de la ficha técnica localizadas.
     *
     * @param m        película
     * @param locale   idioma activo
     * @param messages catálogo de mensajes
     * @return filas de la ficha técnica
     */
    static List<FactRow> facts(Movie m, Locale locale, UiMessages messages) {
        String imdbHref = ExternalLinks.imdb(m.imdbId());
        String runtimeStr = m.runtime() != null ? m.runtime() + " min" : null;
        String releaseDateStr = m.releaseDate() != null ? m.releaseDate().toString() : null;
        String yesText = msg(messages, locale, "detail.fact.yes", "Sí");
        String noText = msg(messages, locale, "detail.fact.no", "No");
        return List.of(
                FactRow.text(msg(messages, locale, "detail.fact.id", "Identificador"), "id", m.id()),
                FactRow.link(msg(messages, locale, "detail.fact.imdb", "IMDb"), "imdb_id", m.imdbId(), imdbHref),
                FactRow.text(msg(messages, locale, "detail.fact.title", "Título"), "title", m.title()),
                FactRow.text(msg(messages, locale, "detail.fact.originalTitle", "Título original"),
                        "original_title", m.originalTitle()),
                FactRow.text(msg(messages, locale, "detail.fact.tagline", "Lema"), "tagline", m.tagline()),
                FactRow.text(msg(messages, locale, "detail.fact.originalLanguage", "Idioma original"),
                        "original_language", m.originalLanguage()),
                FactRow.text(msg(messages, locale, "detail.fact.releaseDate", "Fecha de estreno"),
                        "release_date", releaseDateStr),
                FactRow.text(msg(messages, locale, "detail.fact.runtime", "Duración"), "runtime", runtimeStr),
                FactRow.text(msg(messages, locale, "detail.fact.status", "Estado"), "status", m.status()),
                FactRow.text(msg(messages, locale, "detail.fact.rating", "Clasificación"), "rating", m.rating()),
                FactRow.yesNo(msg(messages, locale, "detail.fact.adult", "Contenido para adultos"),
                        "adult", m.adult(), yesText, noText),
                FactRow.yesNo(msg(messages, locale, "detail.fact.video", "Video"), "video", m.video(), yesText, noText),
                FactRow.link(msg(messages, locale, "detail.fact.homepage", "Sitio oficial"),
                        "homepage", m.homepage(), ExternalLinks.webOrEmpty(m.homepage())));
    }

    /**
     * Construye las filas de producción en español.
     *
     * @param m película
     * @return filas de producción
     */
    static List<FactRow> production(Movie m) {
        return production(m, Locale.forLanguageTag("es"), null);
    }

    /**
     * Construye las filas de producción localizadas.
     *
     * @param m        película
     * @param locale   idioma activo
     * @param messages catálogo de mensajes
     * @return filas de producción
     */
    static List<FactRow> production(Movie m, Locale locale, UiMessages messages) {
        return List.of(
                FactRow.list(msg(messages, locale, "detail.prod.companies", "Productoras"),
                        "production_companies", m.productionCompanies()),
                FactRow.list(msg(messages, locale, "detail.prod.countries", "Países de producción"),
                        "production_countries", m.productionCountries()),
                FactRow.list(msg(messages, locale, "detail.prod.languages", "Idiomas hablados"),
                        "spoken_languages", m.spokenLanguages()));
    }

    /**
     * Construye las filas de finanzas y puntuaciones en español.
     *
     * @param m película
     * @return filas de finanzas y puntuaciones
     */
    static List<FactRow> numbers(Movie m) {
        return numbers(m, Locale.forLanguageTag("es"), null);
    }

    /**
     * Construye las filas de finanzas y puntuaciones localizadas.
     *
     * @param m        película
     * @param locale   idioma activo
     * @param messages catálogo de mensajes
     * @return filas de finanzas y puntuaciones
     */
    static List<FactRow> numbers(Movie m, Locale locale, UiMessages messages) {
        String voteStr = m.voteCount() != null ? Thousands.of(m.voteCount(), locale) : null;
        return List.of(
                FactRow.money(msg(messages, locale, "detail.num.budget", "Presupuesto"),
                        "budget", m.budget(), locale),
                FactRow.money(msg(messages, locale, "detail.num.revenue", "Recaudación"),
                        "revenue", m.revenue(), locale),
                FactRow.text(msg(messages, locale, "detail.num.voteAverage", "Calificación promedio"),
                        "vote_average", voteAverage(m.voteAverage())),
                FactRow.text(msg(messages, locale, "detail.num.votes", "Votos"), "vote_count", voteStr),
                FactRow.text(msg(messages, locale, "detail.num.popularity", "Popularidad"),
                        "popularity", popularity(m.popularity())),
                FactRow.text(msg(messages, locale, "detail.num.userScore", "Puntuación de usuarios"),
                        "user_score", userScore(m.userScore())));
    }

    /**
     * Construye las filas descriptivas de la colección en español.
     *
     * @param coll colección de películas
     * @return filas de colección
     */
    static List<FactRow> collection(MovieCollection coll) {
        return collection(coll, Locale.forLanguageTag("es"), null);
    }

    /**
     * Construye las filas descriptivas de la colección localizadas.
     *
     * @param coll     colección de películas
     * @param locale   idioma activo
     * @param messages catálogo de mensajes
     * @return filas de colección
     */
    static List<FactRow> collection(MovieCollection coll, Locale locale, UiMessages messages) {
        if (coll == null) {
            return List.of();
        }
        return List.of(
                FactRow.text(msg(messages, locale, "detail.fact.id", "Identificador"),
                        "id", coll.id() != null ? String.valueOf(coll.id()) : null),
                FactRow.text(msg(messages, locale, "detail.coll.name", "Nombre"), "name", coll.name()),
                FactRow.text(msg(messages, locale, "detail.coll.poster", "Póster"), "poster_path", coll.posterPath()),
                FactRow.text(msg(messages, locale, "detail.coll.backdrop", "Imagen de fondo"),
                        "backdrop_path", coll.backdropPath()));
    }

    /**
     * Construye las filas con enlaces a imágenes en el CDN de TMDB en español.
     *
     * @param m       película
     * @param posters servicio de URLs de pósters
     * @return filas de imágenes
     */
    static List<FactRow> images(Movie m, PosterUrls posters) {
        return images(m, posters, Locale.forLanguageTag("es"), null);
    }

    /**
     * Construye las filas con enlaces a imágenes en el CDN de TMDB localizadas.
     *
     * @param m        película
     * @param posters  servicio de URLs de pósters
     * @param locale   idioma activo
     * @param messages catálogo de mensajes
     * @return filas de imágenes
     */
    static List<FactRow> images(Movie m, PosterUrls posters, Locale locale, UiMessages messages) {
        String posterUrl = posters.url(m.posterPath());
        String backdropUrl = posters.url(m.backdropPath());
        return List.of(
                FactRow.link(msg(messages, locale, "detail.coll.poster", "Póster"),
                        "poster_path", m.posterPath(), posterUrl),
                FactRow.link(msg(messages, locale, "detail.coll.backdrop", "Imagen de fondo"),
                        "backdrop_path", m.backdropPath(), backdropUrl));
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
        return voteCount(count, Locale.forLanguageTag("es"));
    }

    static String voteCount(Integer count, Locale locale) {
        if (count == null) {
            return "0";
        }
        return Thousands.of(count, locale);
    }

    private static String userScore(Double score) {
        if (score == null) {
            return null;
        }
        return String.format(Locale.US, "%.1f", score);
    }

    private static String msg(UiMessages messages, Locale locale, String key, String fallback) {
        return messages != null ? messages.getFor(locale, key) : fallback;
    }
}
