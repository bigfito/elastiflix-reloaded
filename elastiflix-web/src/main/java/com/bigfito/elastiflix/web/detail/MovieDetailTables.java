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

    private static final String KEY_POSTER = "detail.coll.poster";
    private static final String KEY_BACKDROP = "detail.coll.backdrop";
    private static final String POSTER_PATH = "poster_path";
    private static final String BACKDROP_PATH = "backdrop_path";

    private MovieDetailTables() {
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
        String runtimeStr = m.runtime() != null ? m.runtime() + " min" : null;
        String releaseDateStr = m.releaseDate() != null ? m.releaseDate().toString() : null;
        String yesText = messages.getFor(locale, "detail.fact.yes");
        String noText = messages.getFor(locale, "detail.fact.no");
        return List.of(
                FactRow.text(messages.getFor(locale, "detail.fact.id"), "id", m.id()),
                FactRow.link(messages.getFor(locale, "detail.fact.imdb"), "imdb_id", m.imdbId(),
                        ExternalLinks.imdb(m.imdbId())),
                FactRow.text(messages.getFor(locale, "detail.fact.title"), "title", m.title()),
                FactRow.text(messages.getFor(locale, "detail.fact.originalTitle"), "original_title",
                        m.originalTitle()),
                FactRow.text(messages.getFor(locale, "detail.fact.tagline"), "tagline", m.tagline()),
                FactRow.text(messages.getFor(locale, "detail.fact.originalLanguage"), "original_language",
                        m.originalLanguage()),
                FactRow.text(messages.getFor(locale, "detail.fact.releaseDate"), "release_date", releaseDateStr),
                FactRow.text(messages.getFor(locale, "detail.fact.runtime"), "runtime", runtimeStr),
                FactRow.text(messages.getFor(locale, "detail.fact.status"), "status", m.status()),
                FactRow.text(messages.getFor(locale, "detail.fact.rating"), "rating", m.rating()),
                FactRow.yesNo(messages.getFor(locale, "detail.fact.adult"), "adult", m.adult(), yesText, noText),
                FactRow.yesNo(messages.getFor(locale, "detail.fact.video"), "video", m.video(), yesText, noText),
                FactRow.link(messages.getFor(locale, "detail.fact.homepage"), "homepage", m.homepage(),
                        ExternalLinks.webOrEmpty(m.homepage())));
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
                FactRow.list(messages.getFor(locale, "detail.prod.companies"), "production_companies",
                        m.productionCompanies()),
                FactRow.list(messages.getFor(locale, "detail.prod.countries"), "production_countries",
                        m.productionCountries()),
                FactRow.list(messages.getFor(locale, "detail.prod.languages"), "spoken_languages",
                        m.spokenLanguages()));
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
        return List.of(
                FactRow.money(messages.getFor(locale, "detail.num.budget"), "budget", m.budget(), locale),
                FactRow.money(messages.getFor(locale, "detail.num.revenue"), "revenue", m.revenue(), locale),
                FactRow.text(messages.getFor(locale, "detail.num.voteAverage"), "vote_average",
                        voteAverage(m.voteAverage())),
                FactRow.text(messages.getFor(locale, "detail.num.votes"), "vote_count",
                        voteCount(m.voteCount(), locale)),
                FactRow.text(messages.getFor(locale, "detail.num.popularity"), "popularity",
                        popularity(m.popularity())),
                FactRow.text(messages.getFor(locale, "detail.num.userScore"), "user_score",
                        userScore(m.userScore())));
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
                FactRow.text(messages.getFor(locale, "detail.fact.id"), "id",
                        coll.id() != null ? String.valueOf(coll.id()) : null),
                FactRow.text(messages.getFor(locale, "detail.coll.name"), "name", coll.name()),
                FactRow.text(messages.getFor(locale, KEY_POSTER), POSTER_PATH, coll.posterPath()),
                FactRow.text(messages.getFor(locale, KEY_BACKDROP), BACKDROP_PATH, coll.backdropPath()));
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
        return List.of(
                FactRow.link(messages.getFor(locale, KEY_POSTER), POSTER_PATH, m.posterPath(),
                        posters.url(m.posterPath())),
                FactRow.link(messages.getFor(locale, KEY_BACKDROP), BACKDROP_PATH, m.backdropPath(),
                        posters.url(m.backdropPath())));
    }

    /**
     * Formatea {@code vote_average} con dos decimales.
     *
     * @param avg promedio; puede ser nulo
     * @return texto, o {@code null} si no hay valor (la ficha y la cabecera muestran «N/A»)
     */
    static String voteAverage(Double avg) {
        return avg == null ? null : String.format(Locale.US, "%.2f", avg);
    }

    /**
     * Formatea {@code popularity} con un decimal.
     *
     * @param pop popularidad; puede ser nula
     * @return texto, o {@code null} si no hay valor
     */
    static String popularity(Double pop) {
        return pop == null ? null : String.format(Locale.US, "%.1f", pop);
    }

    /**
     * Formatea {@code vote_count} con el separador de miles del idioma.
     *
     * @param count  votos; pueden ser nulos
     * @param locale idioma activo
     * @return texto, o {@code null} si no hay valor (nunca «0» inventado)
     */
    static String voteCount(Integer count, Locale locale) {
        return count == null ? null : Thousands.of(count, locale);
    }

    private static String userScore(Double score) {
        return score == null ? null : String.format(Locale.US, "%.1f", score);
    }
}
