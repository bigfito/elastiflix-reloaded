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

    @Test
    @DisplayName("el enlace al detalle codifica la barra de un id como «a/b» en un solo segmento")
    void shouldEncodeASlashInTheDetailId() {
        // when
        String href = SearchLinks.detail("a/b", form("matrix"));

        // then
        assertThat(href).startsWith("/pelicula/a%2Fb?");
        assertThat(href).contains("q=matrix");
    }

    @Test
    @DisplayName("el enlace al detalle codifica en UTF-8 un id no ASCII como «café»")
    void shouldEncodeANonAsciiDetailId() {
        // when
        String href = SearchLinks.detail("café", form("matrix"));

        // then
        assertThat(href).startsWith("/pelicula/caf%C3%A9?");
    }

    @Test
    @DisplayName("sin formulario, el enlace al detalle también codifica el id")
    void shouldEncodeTheIdWithoutAForm() {
        assertThat(SearchLinks.detail("a/b", null)).isEqualTo("/pelicula/a%2Fb");
        assertThat(SearchLinks.detail("café", null)).isEqualTo("/pelicula/caf%C3%A9");
        assertThat(SearchLinks.detail("27205", null)).isEqualTo("/pelicula/27205");
    }

    private static SearchForm form(String query) {
        return new SearchForm(query, "fulltext", "match", "title", "or", "plot_256",
                "100", "200", "1", "25", List.of(), List.of(), List.of(), List.of(), "", "");
    }
}
