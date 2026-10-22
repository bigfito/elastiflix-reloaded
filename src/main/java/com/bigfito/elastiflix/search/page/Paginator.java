package com.bigfito.elastiflix.search.page;

import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Calcula el rango visible y, cuando la lista trae más de una página, recorta la que se pidió.
 */
@Component
public class Paginator {

    /**
     * Páginas y rango de la página, sin pasar de la ventana. No recorta la lista.
     *
     * <p>La ventana es 10 000 en full-text y {@code k} en vectorial e híbrida. El total que se
     * muestra sigue siendo el real.</p>
     *
     * @param total    películas que cumplen la búsqueda
     * @param page     página desde 1
     * @param pageSize tamaño de página
     * @param window   máximo de resultados que se pueden recorrer
     * @return la paginación
     */
    public Pagination describe(long total, int page, int pageSize, long window) {
        long start = (long) (page - 1) * pageSize;
        long visible = Math.min(total, window);
        int pages = pageCount(visible, pageSize);
        if (total <= 0 || start >= total) {
            return new Pagination(page, pages, total, pageSize, 0, 0);
        }
        return new Pagination(page, pages, total, pageSize, start + 1, Math.min(start + pageSize, total));
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
}
