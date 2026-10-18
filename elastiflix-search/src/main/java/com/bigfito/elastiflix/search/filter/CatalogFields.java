package com.bigfito.elastiflix.search.filter;

/**
 * Nombres de los campos del índice que usan los filtros y las facetas.
 */
public final class CatalogFields {

    /** Filtro y faceta de contenido para adultos. */
    public static final String ADULT = "adult";

    /** Filtro y faceta de géneros. */
    public static final String GENRES = "genres";

    /** Filtro y faceta de idiomas hablados. */
    public static final String SPOKEN_LANGUAGES = "spoken_languages";

    /** Filtro y faceta de clasificación. */
    public static final String RATING = "rating";

    /** Filtro y rango de fecha de estreno. */
    public static final String RELEASE_DATE = "release_date";

    /** Formato de {@code release_date} en el índice. */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    private CatalogFields() {
    }
}
