package com.bigfito.elastiflix.search.strategy;

import java.util.List;
import java.util.stream.Stream;

import com.bigfito.elastiflix.search.TestApplication;
import com.bigfito.elastiflix.search.query.KnnSettings;
import com.bigfito.elastiflix.search.query.SearchFilters;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.query.VectorField;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las tres filas vectoriales de la tabla de verificación, contra el catálogo real.
 *
 * <p>Solo lee. No crea ni borra índices. No escribe el vector. No fija la posición de la tabla:
 * era del catálogo de 4 322 películas y se mueve al importar otro archivo. La película tiene que
 * seguir entre los {@code k} resultados de los tres campos.</p>
 */
@SpringBootTest(classes = TestApplication.class)
class VectorSearchIT {

    private static final int K = 100;
    private static final int CANDIDATES = 200;

    private final VectorSearchStrategy strategy;

    /**
     * Spring no inyecta el constructor de una prueba si no lleva {@code @Autowired}.
     *
     * @param strategy búsqueda vectorial
     */
    @Autowired
    VectorSearchIT(VectorSearchStrategy strategy) {
        this.strategy = strategy;
    }

    @ParameterizedTest(name = "{1} en la consulta {0}")
    @MethodSource("rows")
    @DisplayName("la película aparece entre los k resultados")
    void shouldFindTheMovieWithinK(String text, String movieId) {
        assertThat(rank(text, VectorField.PLOT_256, movieId)).isBetween(1, K);
        assertThat(rank(text, VectorField.PLOT_512, movieId)).isBetween(1, K);
        assertThat(rank(text, VectorField.PLOT_1024, movieId)).isBetween(1, K);
    }

    @Test
    @DisplayName("sin filtros hay exactamente k resultados y la similitud está en (0, 1]")
    void shouldReturnExactlyKWhenThereAreNoFilters() {
        StrategyResult result = strategy.search(query(
                "un robot solitario que limpia la Tierra", VectorField.PLOT_1024));

        assertThat(result.hits()).hasSize(K);
        assertThat(result.total()).isEqualTo(K);
        assertThat(result.hits()).allSatisfy(hit -> assertThat(hit.score()).isNotNull()
                .isGreaterThan(0d)
                .isLessThanOrEqualTo(1d));
    }

    private int rank(String text, VectorField field, String movieId) {
        List<RankedHit> hits = strategy.search(query(text, field)).hits();
        for (int index = 0; index < hits.size(); index++) {
            if (movieId.equals(hits.get(index).movie().id())) {
                return index + 1;
            }
        }
        return 0;
    }

    private static SearchQuery query(String text, VectorField field) {
        return new SearchQuery(
                text,
                SearchMode.VECTOR,
                null,
                new KnnSettings(field, K, CANDIDATES),
                SearchFilters.none(),
                1,
                SearchQuery.PAGE_25);
    }

    private static Stream<Arguments> rows() {
        return Stream.of(
                Arguments.of("un barco de lujo choca con un iceberg en su viaje inaugural", "597"),
                Arguments.of(
                        "un joven náufrago comparte un bote salvavidas con un tigre de Bengala en medio del océano",
                        "87827"),
                Arguments.of("un robot solitario que limpia la Tierra", "10681"));
    }
}
