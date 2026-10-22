package com.bigfito.elastiflix.search.page;

/**
 * Dónde está la página dentro del total. Los números van sin formato: la web los agrupa por idioma.
 *
 * @param page      página pedida, desde 1
 * @param pageCount páginas que caben; 0 si el total es 0
 * @param total     películas que cumplen la búsqueda
 * @param pageSize  25, 50 o 100
 * @param first     primera película de la página, desde 1; 0 si la página está vacía
 * @param last      última película de la página; 0 si la página está vacía
 */
public record Pagination(int page, int pageCount, long total, int pageSize, long first, long last) {
}
