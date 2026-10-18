package com.bigfito.elastiflix.search.page;

/**
 * Dónde está la página dentro del total.
 *
 * @param page      página pedida, desde 1
 * @param pageCount páginas que caben; 0 si el total es 0
 * @param total     películas que cumplen la búsqueda
 * @param pageSize  25, 50 o 100
 * @param range     texto {@code 1–25 de 100}, con guion largo
 */
public record Pagination(int page, int pageCount, long total, int pageSize, String range) {
}
