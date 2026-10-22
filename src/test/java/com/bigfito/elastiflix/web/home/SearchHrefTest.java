package com.bigfito.elastiflix.web.home;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Una sugerencia conserva el signo más de la consulta al paginar o al volver a buscar.
 */
class SearchHrefTest {

    @Test
    @DisplayName("«Romeo + Juliet» queda con el más codificado en los tres modos")
    void shouldEncodeAPlusInEveryMode() {
        assertPlus(SearchHref.fullText("Romeo + Juliet", "match", "title", "or"));
        assertPlus(SearchHref.vector("Romeo + Juliet", "plot_256"));
        assertPlus(SearchHref.hybrid("Romeo + Juliet", "or"));
    }

    private static void assertPlus(String href) {
        assertThat(href).contains("q=Romeo%20%2B%20Juliet");
        assertThat(href).doesNotContain("+");
    }
}
