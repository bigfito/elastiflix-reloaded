package com.bigfito.elastiflix.web.search;

import java.util.List;

import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.web.locale.SupportedLocale;
import com.bigfito.elastiflix.web.locale.UiMessages;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La nota de límite de Vectorial e Híbrida escribe {@code k} con los miles del idioma, no con los de
 * {@code MessageFormat}.
 */
class ModeCopiesTest {

    private static final UiMessages MESSAGES = new UiMessages(UiMessages.createMessageSource());
    private static final String THOUSAND = "1000";
    private static final String CANDIDATES = "2000";

    private final SearchQueryMapper mapper = new SearchQueryMapper();

    @Test
    @DisplayName("Vectorial con k = 1000 dice «k = 1 000» en español y «k = 1,000» en inglés")
    void shouldGroupKInTheVectorLimit() {
        // given
        SearchQuery query = mapper.toQuery(form("vectorial"));

        // when
        String spanish = ModeCopies.of(query, SupportedLocale.ES, MESSAGES).limit();
        String english = ModeCopies.of(query, SupportedLocale.EN, MESSAGES).limit();

        // then
        assertThat(spanish).contains("(k = 1 000)").doesNotContain("1.000");
        assertThat(english).contains("(k = 1,000)");
    }

    @Test
    @DisplayName("Híbrida con k = 1000 escribe las dos cifras con los miles del idioma")
    void shouldGroupKInTheHybridLimit() {
        // given
        SearchQuery query = mapper.toQuery(form("hibrida"));

        // when
        String spanish = ModeCopies.of(query, SupportedLocale.ES, MESSAGES).limit();
        String english = ModeCopies.of(query, SupportedLocale.EN, MESSAGES).limit();

        // then
        assertThat(spanish).contains("rank_window_size = k = 1 000", "hasta 1 000 resultados")
                .doesNotContain("1.000");
        assertThat(english).contains("rank_window_size = k = 1,000", "up to 1,000 combined results");
    }

    private static SearchForm form(String mode) {
        return new SearchForm(
                "titanic", mode, "match", "title", "or", "plot_256",
                THOUSAND, CANDIDATES, "1", "25", List.of(), List.of(), List.of(), List.of(), "", "");
    }
}
