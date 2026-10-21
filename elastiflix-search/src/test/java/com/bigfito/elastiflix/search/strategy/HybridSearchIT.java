package com.bigfito.elastiflix.search.strategy;

import java.util.List;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.search.TestApplication;
import com.bigfito.elastiflix.search.query.FullTextSettings;
import com.bigfito.elastiflix.search.query.KnnSettings;
import com.bigfito.elastiflix.search.query.MatchType;
import com.bigfito.elastiflix.search.query.SearchFilters;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.query.TextField;
import com.bigfito.elastiflix.search.query.TextOperator;
import com.bigfito.elastiflix.search.query.VectorField;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * La fórmula RRF y la fila híbrida de la tabla de verificación, contra el catálogo real.
 *
 * <p>Solo lee. No crea ni borra índices. El campo vectorial de la fila es {@code plot_1024}.
 * Si el catálogo ya no tiene 6 899 películas, la prueba se omite.</p>
 */
@SpringBootTest(classes = TestApplication.class)
class HybridSearchIT {

    private static final int K = 100;
    private static final int CANDIDATES = 200;
    private static final double TOLERANCE = 1e-6;
    private static final long CATALOG_COUNT = 6_899L;
    private static final String TOY_STORY = "toy story juguetes que cobran vida";

    private final HybridSearchStrategy strategy;
    private final CatalogIndex catalog;
    private final ElastiflixProperties properties;

    /**
     * Spring no inyecta el constructor de una prueba si no lleva {@code @Autowired}.
     *
     * @param strategy   búsqueda híbrida
     * @param catalog    {@code _count} del alias, solo lectura
     * @param properties alias configurado
     */
    @Autowired
    HybridSearchIT(
            HybridSearchStrategy strategy, CatalogIndex catalog, ElastiflixProperties properties) {
        this.strategy = strategy;
        this.catalog = catalog;
        this.properties = properties;
    }

    @Test
    @DisplayName("toy story AND sobre title deja el léxico vacío y el orden del vectorial en plot_1024")
    void shouldFollowTheVectorListWhenTheLexicalComponentIsEmpty() {
        requireCatalog();
        StrategyResult result = strategy.search(query(TOY_STORY, TextOperator.AND));

        assertThat(result.hits()).hasSize(K);
        assertThat(result.total()).isEqualTo(K);
        for (int index = 0; index < result.hits().size(); index++) {
            RankedHit hit = result.hits().get(index);
            assertThat(hit.lexical().score()).as("léxico de %s", hit.movie().id()).isNull();
            assertThat(hit.lexical().isOutside()).isTrue();
            assertThat(hit.lexical().k()).isEqualTo(K);
            assertThat(hit.vector().rank()).isEqualTo(index + 1);
            assertThat(hit.vector().score()).as("vector de %s", hit.movie().id()).isNotNull();
        }
        assertFormula(result.hits());
    }

    @Test
    @DisplayName("la puntuación RRF suma 1/(60+posición) solo en las listas donde la película aparece")
    void shouldMatchTheRrfFormulaWhenBothListsCanContribute() {
        requireCatalog();
        List<RankedHit> hits = strategy.search(query("dark knight", TextOperator.OR)).hits();

        assertThat(hits).isNotEmpty();
        assertFormula(hits);
        long inBoth = hits.stream()
                .filter(hit -> hit.lexical().score() != null && hit.vector().score() != null)
                .count();
        assertThat(inBoth).as("películas presentes en las dos listas").isPositive();
    }

    private void requireCatalog() {
        long counted = catalog.count(properties.index().alias());
        Assumptions.assumeTrue(counted == CATALOG_COUNT, () ->
                "El catálogo cambió: se esperaban " + CATALOG_COUNT + " películas y hay " + counted);
    }

    private static void assertFormula(List<RankedHit> hits) {
        for (RankedHit hit : hits) {
            assertThat(hit.score())
                    .as("id %s léxico %s vector %s", hit.movie().id(),
                            hit.lexical().rank(), hit.vector().rank())
                    .isCloseTo(expected(hit), within(TOLERANCE));
        }
    }

    private static double expected(RankedHit hit) {
        return term(hit.lexical()) + term(hit.vector());
    }

    private static double term(ComponentScore score) {
        if (score.score() == null) {
            return 0d;
        }
        return 1d / (Rrf.RANK_CONSTANT + score.rank());
    }

    private static SearchQuery query(String text, TextOperator operator) {
        return new SearchQuery(
                text,
                SearchMode.HYBRID,
                new FullTextSettings(MatchType.MATCH, TextField.TITLE, operator),
                new KnnSettings(VectorField.PLOT_1024, K, CANDIDATES),
                SearchFilters.none(),
                1,
                SearchQuery.PAGE_25);
    }
}
