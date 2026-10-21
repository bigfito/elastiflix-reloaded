package com.bigfito.elastiflix.web.search;

import java.util.List;

import com.bigfito.elastiflix.web.locale.SupportedLocale;
import com.bigfito.elastiflix.web.locale.UiMessages;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los chips de «Filtros aplicados» separan los valores de un filtro con «o» / «or» entre espacios.
 */
class AppliedFiltersTest {

    private static final UiMessages MESSAGES = new UiMessages(UiMessages.createMessageSource());

    @Test
    @DisplayName("dos géneros, idiomas y clasificaciones se unen con « o » en español")
    void shouldJoinValuesWithSpacedOrInSpanish() {
        List<String> chips = AppliedFilters.chips(form(), SupportedLocale.ES, MESSAGES);

        assertThat(chips).contains(
                "Géneros: Drama o Comedy",
                "Idiomas: English o Español",
                "Clasificación: PG o R");
    }

    @Test
    @DisplayName("dos géneros, idiomas y clasificaciones se unen con « or » en inglés")
    void shouldJoinValuesWithSpacedOrInEnglish() {
        List<String> chips = AppliedFilters.chips(form(), SupportedLocale.EN, MESSAGES);

        assertThat(chips).contains(
                "Genres: Drama or Comedy",
                "Spoken languages: English or Español",
                "Rating: PG or R");
    }

    private static SearchForm form() {
        return new SearchForm(
                "titanic", "fulltext", "match", "title", "or", "plot_256",
                "100", "200", "1", "25", List.of(), List.of("Drama", "Comedy"),
                List.of("English", "Español"), List.of("PG", "R"), "", "");
    }
}
