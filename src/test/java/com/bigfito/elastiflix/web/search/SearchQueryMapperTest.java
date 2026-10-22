package com.bigfito.elastiflix.web.search;

import java.util.List;

import com.bigfito.elastiflix.core.error.SearchValidationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Un número que no cabe en un entero no se convierte en una página 500.
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
