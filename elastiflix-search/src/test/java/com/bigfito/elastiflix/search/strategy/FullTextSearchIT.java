package com.bigfito.elastiflix.search.strategy;

import java.util.stream.Stream;

import com.bigfito.elastiflix.search.TestApplication;
import com.bigfito.elastiflix.search.query.FullTextSettings;
import com.bigfito.elastiflix.search.query.MatchType;
import com.bigfito.elastiflix.search.query.SearchFilters;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.query.TextField;
import com.bigfito.elastiflix.search.query.TextOperator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las ocho filas full-text de la tabla de verificación, contra el catálogo real.
 *
 * <p>Solo lee. No crea ni borra índices.</p>
 */
@SpringBootTest(classes = TestApplication.class)
class FullTextSearchIT {

    private final FullTextSearchStrategy strategy;

    /**
     * Spring no inyecta el constructor de una prueba si no lleva {@code @Autowired}.
     *
     * @param strategy búsqueda full-text
     */
    @Autowired
    FullTextSearchIT(FullTextSearchStrategy strategy) {
        this.strategy = strategy;
    }

    @ParameterizedTest(name = "{0} · {1}")
    @MethodSource("rows")
    @DisplayName("el total OR y AND coincide con la tabla")
    void shouldCountTheVerificationRows(String text, MatchType type, TextField field, long orTotal, long andTotal) {
        assertThat(total(text, type, field, TextOperator.OR)).isEqualTo(orTotal);
        assertThat(total(text, type, field, TextOperator.AND)).isEqualTo(andTotal);
    }

    private long total(String text, MatchType type, TextField field, TextOperator operator) {
        SearchQuery query = new SearchQuery(
                text,
                SearchMode.FULL_TEXT,
                new FullTextSettings(type, field, operator),
                null,
                SearchFilters.none(),
                1,
                SearchQuery.PAGE_25);
        return strategy.search(query).total();
    }

    private static Stream<Arguments> rows() {
        return Stream.of(
                row("dark knight", MatchType.MATCH, TextField.TITLE, 40, 3),
                row("love story", MatchType.MATCH, TextField.TITLE, 65, 1),
                row("Kolejne 365 dni", MatchType.MATCH, TextField.TITLE, 0, 0),
                row("el secreto", MatchType.MATCH, TextField.ORIGINAL_TITLE, 16, 1),
                row("le fabuleux destin", MatchType.MATCH, TextField.ORIGINAL_TITLE, 14, 1),
                row("heaven cielo", MatchType.MULTI_MATCH, null, 6, 0),
                row("next kolejne", MatchType.MULTI_MATCH, null, 11, 0),
                row("parasite 기생충", MatchType.MULTI_MATCH, null, 1, 0));
    }

    private static Arguments row(String text, MatchType type, TextField field, long orTotal, long andTotal) {
        return Arguments.of(text, type, field, orTotal, andTotal);
    }
}
