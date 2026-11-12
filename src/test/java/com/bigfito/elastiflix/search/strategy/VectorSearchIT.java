package com.bigfito.elastiflix.search.strategy;

import java.util.List;
import java.util.stream.Stream;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.search.query.KnnSettings;
import com.bigfito.elastiflix.search.query.SearchFilters;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.query.VectorField;

import org.junit.jupiter.api.Assumptions;
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
 * <p>Solo lee. No crea ni borra índices. No escribe el vector. Afirma la posición medida sobre
 * 6 899 películas, con {@code k = 100} y {@code num_candidates = 200}. La medición vigente es del 2026-11-12,
 * después de la ronda de calidad del catálogo que cambió 1 641 sinopsis de {@code data/}: varias películas pasaron
 * a tener el overview corto de TMDB y ahora compiten con las buscadas. Los vectores los genera de nuevo el servicio
 * de inferencia en cada carga y el k-NN es aproximado, así que una película al borde puede moverse un lugar tras
 * reconstruir el índice. No se afloja el criterio en el código. Si el conteo cambió, la prueba se omite.</p>
 */
@SpringBootTest
class VectorSearchIT {

    private static final int K = 100;
    private static final int CANDIDATES = 200;
    private static final long CATALOG_COUNT = 6_899L;

    private final VectorSearchStrategy strategy;
    private final CatalogIndex catalog;
    private final ElastiflixProperties properties;

    /**
     * Spring no inyecta el constructor de una prueba si no lleva {@code @Autowired}.
     *
     * @param strategy   búsqueda vectorial
     * @param catalog    {@code _count} del alias, solo lectura
     * @param properties alias configurado
     */
    @Autowired
    VectorSearchIT(
            VectorSearchStrategy strategy, CatalogIndex catalog, ElastiflixProperties properties) {
        this.strategy = strategy;
        this.catalog = catalog;
        this.properties = properties;
    }

    @ParameterizedTest(name = "{1} en {2}.º / {3}.º / {4}.º")
    @MethodSource("rows")
    @DisplayName("la película queda en la posición medida")
    void shouldFindTheMovieAtTheMeasuredRank(
            String text, String movieId, int rank256, int rank512, int rank1024) {
        requireCatalog();

        assertThat(rank(text, VectorField.PLOT_256, movieId)).isEqualTo(rank256);
        assertThat(rank(text, VectorField.PLOT_512, movieId)).isEqualTo(rank512);
        assertThat(rank(text, VectorField.PLOT_1024, movieId)).isEqualTo(rank1024);
    }

    @Test
    @DisplayName("sin filtros hay exactamente k resultados y la similitud está en (0, 1]")
    void shouldReturnExactlyKWhenThereAreNoFilters() {
        requireCatalog();
        StrategyResult result = strategy.search(query(
                "un robot solitario que limpia la Tierra", VectorField.PLOT_1024));

        assertThat(result.hits()).hasSize(K);
        assertThat(result.total()).isEqualTo(K);
        assertThat(result.hits()).allSatisfy(hit -> assertThat(hit.score()).isNotNull()
                .isGreaterThan(0d)
                .isLessThanOrEqualTo(1d));
    }

    private void requireCatalog() {
        long counted = catalog.count(properties.index().alias());
        Assumptions.assumeTrue(counted == CATALOG_COUNT, () ->
                "El catálogo cambió: se esperaban " + CATALOG_COUNT + " películas y hay " + counted);
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
                Arguments.of(
                        "un barco de lujo choca con un iceberg en su viaje inaugural", "597", 60, 19, 10),
                Arguments.of(
                        "un joven náufrago comparte un bote salvavidas con un tigre de Bengala en medio del océano",
                        "87827", 52, 13, 7),
                Arguments.of("un robot solitario que limpia la Tierra", "10681", 1, 2, 2));
    }
}
