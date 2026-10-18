package com.bigfito.elastiflix.web.search;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La paginación conserva el signo más de la consulta.
 */
class SearchLinksTest {

    @Test
    @DisplayName("la página siguiente codifica el más de «Romeo + Juliet»")
    void shouldEncodeAPlusWhenChangingPage() {
        SearchForm form = new SearchForm(
                "Romeo + Juliet", "fulltext", "match", "title", "or", "plot_256",
                "100", "200", "1", "25", List.of(), List.of(), List.of(), List.of(), "", "");

        String href = SearchLinks.page(form, 2);

        assertThat(href).contains("q=Romeo%20%2B%20Juliet");
        assertThat(href).doesNotContain("+");
        assertThat(href).contains("pagina=2");
    }
}
