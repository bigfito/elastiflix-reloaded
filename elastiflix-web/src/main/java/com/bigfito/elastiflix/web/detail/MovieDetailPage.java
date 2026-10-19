package com.bigfito.elastiflix.web.detail;

import java.util.List;

import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.core.model.MovieCollection;
import com.bigfito.elastiflix.core.model.MovieKeyword;
import com.bigfito.elastiflix.web.layout.PosterUrls;
import com.bigfito.elastiflix.web.search.SearchForm;
import com.bigfito.elastiflix.web.search.SearchLinks;

import org.springframework.stereotype.Component;

/**
 * Transforma el documento {@link Movie} y el contexto de búsqueda en el modelo de vista {@link MovieDetailView}.
 */
@Component
public class MovieDetailPage {

    private final PosterUrls posters;

    /**
     * Crea el transformador con el CDN de pósters.
     *
     * @param posters URLs del CDN de TMDB
     */
    public MovieDetailPage(PosterUrls posters) {
        this.posters = posters;
    }

    /**
     * Construye la vista del detalle a partir de la película y los parámetros de búsqueda.
     *
     * @param m    documento de la película
     * @param form formulario con la búsqueda previa
     * @return modelo para la plantilla
     */
    public MovieDetailView build(Movie m, SearchForm form) {
        String homepage = text(m.homepage());
        MovieCollection coll = m.belongsToCollection();
        List<String> cast = list(m.cast());
        List<MovieKeyword> keywords = list(m.keywords());

        return new MovieDetailView(
                m.id(), text(m.title()), text(m.originalTitle()), text(m.tagline()),
                text(m.overview()), text(m.plot()), posters.url(m.posterPath()), posters.url(m.backdropPath()),
                MovieDetailTables.voteAverage(m.voteAverage()), MovieDetailTables.voteCount(m.voteCount()),
                MovieDetailTables.popularity(m.popularity()), imdb(m.imdbId()), homepage, !homepage.isBlank(),
                MovieDetailBadges.pills(m), MovieDetailBadges.genres(m.genres()),
                MovieDetailTables.facts(m), MovieDetailTables.production(m), MovieDetailTables.numbers(m),
                coll != null, collectionName(coll), MovieDetailTables.collection(coll),
                MovieDetailTables.images(m, posters), cast, cast.size(), keywords, keywords.size(),
                MovieDetailSearchChips.chips(form), SearchLinks.returnToSearch(m.id(), form));
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

    private static String imdb(String imdbId) {
        if (imdbId == null || imdbId.isBlank()) {
            return "";
        }
        return "https://www.imdb.com/title/" + imdbId + "/";
    }
}
