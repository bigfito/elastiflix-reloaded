package com.bigfito.elastiflix.search.strategy;

import java.util.stream.Stream;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.search.query.FullTextSettings;
import com.bigfito.elastiflix.search.query.MatchType;
import com.bigfito.elastiflix.search.query.SearchFilters;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.query.TextField;
import com.bigfito.elastiflix.search.query.TextOperator;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las diez consultas full-text de la tabla de verificación, contra el catálogo real.
 *
 * <p>Solo lee. No crea ni borra índices. Afirma los totales OR y AND medidos sobre 6 899 películas el
 * 2026-10-18, y los de {@code plot} el 2026-11-12. No se afloja el criterio en el código. Si el conteo cambió,
 * la prueba se omite.</p>
 */
@SpringBootTest
class FullTextSearchIT {

    private static final long CATALOG_COUNT = 6_899L;

    private final FullTextSearchStrategy strategy;
    private final CatalogIndex catalog;
    private final ElastiflixProperties properties;

    /**
     * Spring no inyecta el constructor de una prueba si no lleva {@code @Autowired}.
     *
     * @param strategy   búsqueda full-text
     * @param catalog    {@code _count} del alias, solo lectura
     * @param properties alias configurado
     */
    @Autowired
    FullTextSearchIT(
            FullTextSearchStrategy strategy, CatalogIndex catalog, ElastiflixProperties properties) {
        this.strategy = strategy;
        this.catalog = catalog;
        this.properties = properties;
    }

    @ParameterizedTest(name = "{0} · OR {3} · AND {4}")
    @MethodSource("rows")
    @DisplayName("OR y AND coinciden con los valores de referencia")
    void shouldMatchTheMeasuredTotals(
            String text, MatchType type, TextField field, long orExpected, long andExpected) {
        requireCatalog();

        assertThat(total(text, type, field, TextOperator.OR)).isEqualTo(orExpected);
        assertThat(total(text, type, field, TextOperator.AND)).isEqualTo(andExpected);
    }

    private void requireCatalog() {
        long counted = catalog.count(properties.index().alias());
        Assumptions.assumeTrue(counted == CATALOG_COUNT, () ->
                "El catálogo cambió: se esperaban " + CATALOG_COUNT + " películas y hay " + counted);
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
                row("dark knight", MatchType.MATCH, TextField.TITLE, 57, 5),
                row("love story", MatchType.MATCH, TextField.TITLE, 107, 3),
                row("Kolejne 365 dni", MatchType.MATCH, TextField.TITLE, 3, 0),
                row("el secreto", MatchType.MATCH, TextField.ORIGINAL_TITLE, 35, 2),
                row("le fabuleux destin", MatchType.MATCH, TextField.ORIGINAL_TITLE, 26, 1),
                row("heaven cielo", MatchType.MULTI_MATCH, null, 10, 0),
                row("next kolejne", MatchType.MULTI_MATCH, null, 15, 0),
                row("parasite 기생충", MatchType.MULTI_MATCH, null, 1, 0),
                row("ogre donkey princess", MatchType.MATCH, TextField.PLOT, 162, 3),
                row("bathhouse spirits", MatchType.MATCH, TextField.PLOT, 87, 1));
    }

    private static Arguments row(
            String text, MatchType type, TextField field, long orTotal, long andTotal) {
        return Arguments.of(text, type, field, orTotal, andTotal);
    }
}
