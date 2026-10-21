package com.bigfito.elastiflix.web.detail;

import java.util.List;

import java.util.Locale;

import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.core.model.MovieCollection;
import com.bigfito.elastiflix.core.model.MovieKeyword;
import com.bigfito.elastiflix.web.layout.PosterUrls;
import com.bigfito.elastiflix.web.locale.SupportedLocale;
import com.bigfito.elastiflix.web.locale.UiMessages;
import com.bigfito.elastiflix.web.search.SearchForm;
import com.bigfito.elastiflix.web.search.SearchLinks;

import org.springframework.stereotype.Component;

/**
 * Transforma el documento {@link Movie} y el contexto de búsqueda en el modelo de vista {@link MovieDetailView}.
 */
@Component
public class MovieDetailPage {

    private final PosterUrls posters;
    private final UiMessages messages;

    /**
     * Crea el transformador con el CDN de pósters y el catálogo de mensajes.
     *
     * @param posters  URLs del CDN de TMDB
     * @param messages catálogo de mensajes para i18n
     */
    public MovieDetailPage(PosterUrls posters, UiMessages messages) {
        this.posters = posters;
        this.messages = messages;
    }

    /**
     * Construye la vista del detalle a partir de la película y los parámetros de búsqueda en español.
     *
     * @param m    documento de la película
     * @param form formulario con la búsqueda previa
     * @return modelo para la plantilla
     */
    public MovieDetailView build(Movie m, SearchForm form) {
        return build(m, form, SupportedLocale.ES);
    }

    /**
     * Construye la vista del detalle a partir de la película, parámetros de búsqueda e idioma activo.
     *
     * @param m      documento de la película
     * @param form   formulario con la búsqueda previa
     * @param locale idioma activo
     * @return modelo para la plantilla
     */
    public MovieDetailView build(Movie m, SearchForm form, Locale locale) {
        String homepage = ExternalLinks.webOrEmpty(m.homepage());
        MovieCollection coll = m.belongsToCollection();
        List<String> cast = list(m.cast());
        List<MovieKeyword> keywords = list(m.keywords());

        return new MovieDetailView(
                m.id(), text(m.title()), text(m.originalTitle()), text(m.tagline()),
                text(m.overview()), text(m.plot()), posters.url(m.posterPath()), posters.url(m.backdropPath()),
                MovieDetailTables.voteAverage(m.voteAverage()), MovieDetailTables.voteCount(m.voteCount(), locale),
                MovieDetailTables.popularity(m.popularity()), ExternalLinks.imdb(m.imdbId()), homepage,
                !homepage.isBlank(),
                MovieDetailBadges.pills(m), MovieDetailBadges.genres(m.genres()),
                MovieDetailTables.facts(m, locale, messages),
                MovieDetailTables.production(m, locale, messages),
                MovieDetailTables.numbers(m, locale, messages),
                coll != null, collectionName(coll), MovieDetailTables.collection(coll, locale, messages),
                MovieDetailTables.images(m, posters, locale, messages), cast, cast.size(), keywords, keywords.size(),
                MovieDetailSearchChips.chips(form, locale, messages), SearchLinks.returnToSearch(m.id(), form));
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static <T> List<T> list(List<T> items) {
        return items == null ? List.of() : items;
    }

    private static String collectionName(MovieCollection coll) {
        return coll != null && coll.name() != null ? coll.name() : "";
    }
}
