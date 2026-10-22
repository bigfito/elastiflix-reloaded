package com.bigfito.elastiflix.web.detail;

import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.model.Movie;

/**
 * Genera los pills informativos del encabezado y las insignias de géneros del detalle.
 */
final class MovieDetailBadges {

    private static final String ICON_DATE = "M4 6h16v14H4z M4 10h16 M8 3v5 M16 3v5";
    private static final String ICON_TIME = "M12 3a9 9 0 1 0 0 18a9 9 0 0 0 0-18z M12 7v5l3 2";
    private static final String ICON_RATING = "M12 3l7 3v5c0 5-3.5 8.5-7 10c-3.5-1.5-7-5-7-10V6z";
    private static final String ICON_LANG =
            "M4 5h10 M9 3v2 M6 5c1 4 4 7 8 8 M13 5c-1 4-4 7-8 8 M14 21l4-9l4 9 M15.5 18h5";
    private static final String ICON_STATUS = "M5 12l4 4l10-10";

    private static final List<String> PASTEL_COLORS = List.of(
            "#BFE0F6", "#BDE8D7", "#FBE7A1", "#CDBDF6", "#F7C1D9", "#FFCDB2");

    private MovieDetailBadges() {
    }

    /**
     * Construye los pills informativos visibles en la cabecera del detalle.
     *
     * @param m película
     * @return lista inmutable de pills
     */
    static List<DetailPill> pills(Movie m) {
        List<DetailPill> list = new ArrayList<>();
        if (m.releaseDate() != null) {
            list.add(new DetailPill("release_date", ICON_DATE, m.releaseDate().toString()));
        }
        if (m.runtime() != null) {
            list.add(new DetailPill("runtime", ICON_TIME, m.runtime() + " min"));
        }
        if (m.rating() != null && !m.rating().isBlank()) {
            list.add(new DetailPill("rating", ICON_RATING, m.rating()));
        }
        if (m.originalLanguage() != null && !m.originalLanguage().isBlank()) {
            list.add(new DetailPill("original_language", ICON_LANG, m.originalLanguage()));
        }
        if (m.status() != null && !m.status().isBlank()) {
            list.add(new DetailPill("status", ICON_STATUS, m.status()));
        }
        return List.copyOf(list);
    }

    /**
     * Construye las insignias de género con paleta pastel rotativa.
     *
     * @param genreNames nombres de géneros
     * @return lista inmutable de insignias
     */
    static List<GenreBadge> genres(List<String> genreNames) {
        if (genreNames == null || genreNames.isEmpty()) {
            return List.of();
        }
        List<GenreBadge> badges = new ArrayList<>();
        int i = 0;
        for (String name : genreNames) {
            badges.add(new GenreBadge(name, PASTEL_COLORS.get(i % PASTEL_COLORS.size())));
            i++;
        }
        return List.copyOf(badges);
    }
}
