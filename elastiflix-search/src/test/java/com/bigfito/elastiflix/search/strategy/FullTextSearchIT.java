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
 * Las ocho consultas full-text de la tabla de verificación, contra el catálogo real.
 *
 * <p>Solo lee. No crea ni borra índices. No fija los totales de esa tabla: eran del catálogo de
 * 4 322 películas y cambian al importar otro archivo. OR tiene que encontrar al menos una
 * película, y AND no puede superar ese total.</p>
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
    @DisplayName("OR encuentra películas y AND no supera a OR")
    void shouldKeepAndInsideOr(String text, MatchType type, TextField field) {
        long orTotal = total(text, type, field, TextOperator.OR);
        long andTotal = total(text, type, field, TextOperator.AND);

        assertThat(orTotal).isPositive();
        assertThat(andTotal).isBetween(0L, orTotal);
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
                row("dark knight", MatchType.MATCH, TextField.TITLE),
                row("love story", MatchType.MATCH, TextField.TITLE),
                row("Kolejne 365 dni", MatchType.MATCH, TextField.TITLE),
                row("el secreto", MatchType.MATCH, TextField.ORIGINAL_TITLE),
                row("le fabuleux destin", MatchType.MATCH, TextField.ORIGINAL_TITLE),
                row("heaven cielo", MatchType.MULTI_MATCH, null),
                row("next kolejne", MatchType.MULTI_MATCH, null),
                row("parasite 기생충", MatchType.MULTI_MATCH, null));
    }

    private static Arguments row(String text, MatchType type, TextField field) {
        return Arguments.of(text, type, field);
    }
}
