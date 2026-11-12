package com.bigfito.elastiflix.web.search;

import java.util.List;

import com.bigfito.elastiflix.core.error.SearchValidationException;
import com.bigfito.elastiflix.search.query.TextField;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Un número que no cabe en un entero no se convierte en una página 500, y el campo de texto se lee del formulario.
 */
class SearchQueryMapperTest {

    private final SearchQueryMapper mapper = new SearchQueryMapper();

    @Test
    @DisplayName("una página enorme vuelve a la primera en lugar de tronar")
    void shouldTreatAnOverflowingPageAsTheFallback() {
        var query = mapper.toQuery(form("999999999999", "25", "10", "100"));

        assertThat(query.page()).isEqualTo(1);
    }

    @Test
    @DisplayName("un k enorme falla la validación y no sale como NumberFormatException")
    void shouldRejectAnOverflowingKAsValidation() {
        assertThatThrownBy(() -> mapper.toQuery(form("1", "25", "999999999999", "100", "vectorial")))
                .isInstanceOf(SearchValidationException.class);
    }

    @Test
    @DisplayName("campo=plot busca en la sinopsis; un campo desconocido vuelve a title")
    void shouldMapThePlotFieldAndFallBackToTitle() {
        assertThat(mapper.toQuery(form("plot")).fullText().field()).isEqualTo(TextField.PLOT);
        assertThat(mapper.toQuery(form("original_title")).fullText().field()).isEqualTo(TextField.ORIGINAL_TITLE);
        assertThat(mapper.toQuery(form("overview")).fullText().field()).isEqualTo(TextField.TITLE);
    }

    private static SearchForm form(String field) {
        return new SearchForm(
                "titanic", "fulltext", "match", field, "or", "plot_256",
                "100", "200", "1", "25", List.of(), List.of(), List.of(), List.of(), "", "");
    }

    private static SearchForm form(String page, String pageSize, String k, String candidates) {
        return form(page, pageSize, k, candidates, "fulltext");
    }

    private static SearchForm form(
            String page, String pageSize, String k, String candidates, String mode) {
        return new SearchForm(
                "titanic", mode, "match", "title", "or", "plot_256",
                k, candidates, page, pageSize, List.of(), List.of(), List.of(), List.of(), "", "");
    }
}
