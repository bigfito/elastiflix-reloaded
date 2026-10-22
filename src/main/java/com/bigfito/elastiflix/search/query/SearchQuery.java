package com.bigfito.elastiflix.search.query;

/**
 * Todo lo que hace falta para una búsqueda, ya separado de la URL.
 *
 * <p>En full-text {@code knn} es {@code null}. En vectorial {@code fullText} es {@code null}.
 * En híbrida van los dos. La validación vive en {@link SearchQueryValidator}, no aquí: así una
 * prueba puede armar un caso inválido a propósito.</p>
 *
 * @param text     consulta, sin prefijo
 * @param mode     full-text, vectorial o híbrida
 * @param fullText ajustes léxicos, o {@code null}
 * @param knn      ajustes vectoriales, o {@code null}
 * @param filters  filtros; si llegan {@code null}, se usan unos vacíos
 * @param page     página desde 1
 * @param pageSize 25, 50 o 100
 */
public record SearchQuery(
        String text,
        SearchMode mode,
        FullTextSettings fullText,
        KnnSettings knn,
        SearchFilters filters,
        int page,
        int pageSize) {

    /** Tamaño de página chico. */
    public static final int PAGE_25 = 25;

    /** Tamaño de página mediano. */
    public static final int PAGE_50 = 50;

    /** Tamaño de página grande. */
    public static final int PAGE_100 = 100;

    /**
     * Rellena los filtros ausentes.
     */
    public SearchQuery {
        if (filters == null) {
            filters = SearchFilters.none();
        }
    }

    /**
     * Índice del primer resultado de esta página, contando desde cero.
     *
     * @return {@code (page - 1) * pageSize}
     */
    public int from() {
        return (page - 1) * pageSize;
    }

    /**
     * La misma consulta sin filtros, para confirmar un resultado vacío.
     *
     * @return una copia con {@link SearchFilters#none()}
     */
    public SearchQuery withoutFilters() {
        return new SearchQuery(text, mode, fullText, knn, SearchFilters.none(), page, pageSize);
    }
}
