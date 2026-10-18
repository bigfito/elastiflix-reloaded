package com.bigfito.elastiflix.search.page;

import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Calcula el rango visible y, cuando la lista trae más de una página, recorta la que se pidió.
 */
@Component
public class Paginator {

    private static final String DASH = "–";

    /**
     * Páginas y texto del rango. No recorta la lista.
     *
     * @param total    películas que cumplen la búsqueda
     * @param page     página desde 1
     * @param pageSize tamaño de página
     * @return la paginación
     */
    public Pagination describe(long total, int page, int pageSize) {
        long start = (long) (page - 1) * pageSize;
        return new Pagination(page, pageCount(total, pageSize), total, pageSize, range(total, start, pageSize));
    }

    /**
     * La página pedida dentro de una lista ya completa, como los {@code k} de vectorial e híbrida.
     *
     * @param hits     lista completa
     * @param page     página desde 1
     * @param pageSize tamaño de página
     * @param <T>      tipo de cada elemento
     * @return la rebanada, o una lista vacía si la página se pasa del final
     */
    public <T> List<T> slice(List<T> hits, int page, int pageSize) {
        int start = (page - 1) * pageSize;
        if (start >= hits.size()) {
            return List.of();
        }
        int end = Math.min(start + pageSize, hits.size());
        return List.copyOf(hits.subList(start, end));
    }

    private static int pageCount(long total, int pageSize) {
        if (total <= 0) {
            return 0;
        }
        return (int) ((total + pageSize - 1) / pageSize);
    }

    private static String range(long total, long start, int pageSize) {
        if (total <= 0 || start >= total) {
            return "0" + DASH + "0 de " + total;
        }
        long end = Math.min(start + pageSize, total);
        return (start + 1) + DASH + end + " de " + total;
    }
}
