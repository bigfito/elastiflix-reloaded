package com.bigfito.elastiflix.web.search;

import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.search.page.Pagination;

/**
 * Números de página alrededor de la actual. Como mucho siete.
 */
public final class PageButtons {

    private static final int WINDOW = 7;
    private static final int HALF = 3;

    private PageButtons() {
    }

    /**
     * Botones de la ventana visible.
     *
     * @param form       consulta, para armar los enlaces
     * @param pagination página actual y total
     * @return botones; vacío si no hay resultados
     */
    public static List<PageButton> of(SearchForm form, Pagination pagination) {
        if (pagination.pageCount() < 1) {
            return List.of();
        }
        int start = start(pagination);
        int end = Math.min(pagination.pageCount(), start + WINDOW - 1);
        return buttons(form, pagination.page(), start, end);
    }

    private static int start(Pagination pagination) {
        int start = pagination.page() - HALF;
        if (start < 1) {
            return 1;
        }
        int lastStart = pagination.pageCount() - WINDOW + 1;
        if (start > lastStart) {
            return Math.max(1, lastStart);
        }
        return start;
    }

    private static List<PageButton> buttons(SearchForm form, int current, int start, int end) {
        List<PageButton> buttons = new ArrayList<>();
        for (int number = start; number <= end; number++) {
            buttons.add(new PageButton(number, SearchLinks.page(form, number), number == current));
        }
        return List.copyOf(buttons);
    }
}
