package com.bigfito.elastiflix.search.result;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bigfito.elastiflix.core.error.SearchValidationException;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.search.facet.FacetCalculator;
import com.bigfito.elastiflix.search.facet.FacetValue;
import com.bigfito.elastiflix.search.filter.CatalogFields;
import com.bigfito.elastiflix.search.page.Paginator;
import com.bigfito.elastiflix.search.query.FullTextSettings;
import com.bigfito.elastiflix.search.query.KnnSettings;
import com.bigfito.elastiflix.search.query.MatchType;
import com.bigfito.elastiflix.search.query.SearchFilters;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.query.TextField;
import com.bigfito.elastiflix.search.query.TextOperator;
import com.bigfito.elastiflix.search.query.VectorField;
import com.bigfito.elastiflix.search.strategy.RankedHit;
import com.bigfito.elastiflix.search.strategy.SearchStrategy;
import com.bigfito.elastiflix.search.strategy.StrategyResult;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Los tres modos y los dos estados vacíos, con estrategias simuladas.
 */
class SearchServiceTest {

    private static final String NONE = "No encontramos películas";
    private static final String FILTERED = "Ningún resultado cumple los filtros";

    private final FakeStrategy fullText = new FakeStrategy(SearchMode.FULL_TEXT);
    private final FakeStrategy vector = new FakeStrategy(SearchMode.VECTOR);
    private final FakeStrategy hybrid = new FakeStrategy(SearchMode.HYBRID);
    private final SearchService service = new SearchService(
            List.of(fullText, vector, hybrid), new FacetCalculator(), new Paginator());

    @Test
    @DisplayName("full-text deja la página que ya trajo Elasticsearch y lee las agregaciones")
    void shouldKeepTheElasticsearchPageWhenTheModeIsFullText() {
        fullText.next = new StrategyResult(
                List.of(hit("1", "Horror"), hit("2", "Horror")),
                40,
                Map.of(CatalogFields.GENRES, terms("Drama", 9)));
        SearchResult result = service.search(fullText("dark knight", 2, SearchFilters.none()));

        assertThat(result.hits()).extracting(SearchHit::id).containsExactly("1", "2");
        assertThat(result.total()).isEqualTo(40);
        assertThat(result.pagination().range()).isEqualTo("26–40 de 40");
        assertThat(result.facets().genres()).containsExactly(new FacetValue("Drama", 9));
        assertThat(result.adjustments()).containsExactly("match · title", "OR");
        assertThat(result.empty()).isNull();
        assertThat(fullText.unfilteredCalls).isZero();
    }

    @Test
    @DisplayName("multi-match etiqueta best_fields")
    void shouldLabelMultiMatchWhenThereIsNoField() {
        fullText.next = new StrategyResult(List.of(hit("1", "Drama")), 1, Map.of());
        SearchQuery query = new SearchQuery(
                "heaven cielo",
                SearchMode.FULL_TEXT,
                new FullTextSettings(MatchType.MULTI_MATCH, null, TextOperator.AND),
                null,
                SearchFilters.none(),
                1,
                SearchQuery.PAGE_25);
        assertThat(service.search(query).adjustments())
                .containsExactly("multi-match · best_fields", "AND");
    }

    @Test
    @DisplayName("k = 10 y tamaño 25 devuelve los 10 resultados de la primera página")
    void shouldReturnEveryHitWhenKIsSmallerThanThePage() {
        vector.next = new StrategyResult(hits(10), 10, Map.of());
        SearchQuery query = vector("un barco", 10, 1, SearchQuery.PAGE_25);

        SearchResult result = service.search(query);

        assertThat(result.hits()).hasSize(10);
        assertThat(result.pagination().pageCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("k = 150 y tamaño 100 dejan 50 resultados en la segunda página")
    void shouldReturnThePartialSecondPageWhenKDoesNotFillIt() {
        vector.next = new StrategyResult(hits(150), 150, Map.of());
        SearchQuery query = vector("un barco", 150, 2, SearchQuery.PAGE_100);

        SearchResult result = service.search(query);

        assertThat(result.hits()).hasSize(50);
        assertThat(result.hits().getFirst().id()).isEqualTo("100");
        assertThat(result.pagination().pageCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("full-text no ofrece páginas más allá de 10000")
    void shouldStopThePageCountAtTheFullTextWindow() {
        fullText.next = new StrategyResult(List.of(hit("1", "Drama")), 12_000, Map.of());
        SearchResult result = service.search(fullText("titanic", 400, SearchFilters.none()));

        assertThat(result.pagination().pageCount()).isEqualTo(400);
        assertThat(result.pagination().total()).isEqualTo(12_000);
    }

    @Test
    @DisplayName("vectorial recorta la página y cuenta las k películas, no solo la página")
    void shouldSliceThePageAndCountEveryMovieWhenTheModeIsVector() {
        vector.next = new StrategyResult(thirty(), 30, Map.of());
        SearchResult result = service.search(vector("un barco", 2));

        assertThat(result.hits()).hasSize(5);
        assertThat(result.hits().getFirst().id()).isEqualTo("25");
        assertThat(result.hits().getFirst().rrf()).as("fuera de la híbrida no hay RRF").isNull();
        assertThat(result.facets().genres()).containsExactly(
                new FacetValue("Action", 25),
                new FacetValue("Drama", 5));
        assertThat(result.pagination().range()).isEqualTo("26–30 de 30");
        assertThat(result.adjustments()).containsExactly("plot_1024", "k 100", "num_candidates 200");
        assertThat(result.empty()).isNull();
    }

    @Test
    @DisplayName("híbrida etiqueta el vector, k y RRF, y su puntuación es la de RRF")
    void shouldLabelRrfWhenTheModeIsHybrid() {
        hybrid.next = new StrategyResult(List.of(hit("1", "Drama")), 1, Map.of());
        SearchQuery query = new SearchQuery(
                "toy story",
                SearchMode.HYBRID,
                new FullTextSettings(MatchType.MATCH, TextField.TITLE, TextOperator.AND),
                new KnnSettings(VectorField.PLOT_1024, 100, 200),
                SearchFilters.none(),
                1,
                SearchQuery.PAGE_25);

        SearchResult result = service.search(query);

        assertThat(result.adjustments()).containsExactly(
                "match · title", "AND", "plot_1024", "k 100", "num_candidates 200", "RRF");
        assertThat(result.hits().getFirst().rrf()).as("en la híbrida la puntuación es la de RRF").isEqualTo(1.0);
    }

    @Test
    @DisplayName("sin filtros y sin resultados usa los consejos del modo")
    void shouldExplainAnEmptyQueryWhenThereAreNoFilters() {
        fullText.next = new StrategyResult(List.of(), 0, Map.of());
        SearchResult result = service.search(fullText("dark knight", 1, SearchFilters.none()));

        assertThat(result.empty().title()).isEqualTo(NONE);
        assertThat(result.empty().text())
                .isEqualTo("La consulta «dark knight» no devolvió resultados con los ajustes actuales.");
        assertThat(result.empty().tips()).containsExactly(
                "Con AND deben aparecer todas las palabras; prueba con OR.",
                "Con match se busca en un solo campo; prueba multi-match.",
                "Revisa la ortografía y los apóstrofos (\u2019 y ').");
        assertThat(fullText.unfilteredCalls).isZero();
    }

    @Test
    @DisplayName("si sin filtros sí hay películas, el vacío es por los filtros")
    void shouldExplainTheFiltersWhenTheUnfilteredQueryFindsMovies() {
        fullText.next = new StrategyResult(List.of(), 0, Map.of());
        fullText.unfiltered = 4;
        SearchResult result = service.search(fullText("dark knight", 1, drama()));

        assertThat(result.empty().title()).isEqualTo(FILTERED);
        assertThat(result.empty().text()).isEqualTo(
                "La búsqueda sí encontró películas, pero ninguna cumple a la vez todos los filtros aplicados.");
        assertThat(result.empty().tips()).containsExactly(
                "Desmarca algún valor o quita un filtro y pulsa APLICAR.",
                "Amplía el rango de fechas de estreno.");
        assertThat(fullText.unfilteredCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("si sin filtros tampoco hay películas, el vacío no es por los filtros")
    void shouldExplainTheQueryWhenTheUnfilteredQueryIsAlsoEmpty() {
        vector.next = new StrategyResult(List.of(), 0, Map.of());
        vector.unfiltered = 0;
        SearchQuery query = new SearchQuery(
                "un barco",
                SearchMode.VECTOR,
                null,
                new KnnSettings(VectorField.PLOT_1024, 30, 200),
                drama(),
                1,
                SearchQuery.PAGE_25);
        SearchResult result = service.search(query);

        assertThat(result.empty().title()).isEqualTo(NONE);
        assertThat(result.empty().tips()).containsExactly(
                "Prueba a describir la trama con otras palabras.",
                "Cambia de modo o de ajustes de búsqueda.");
        assertThat(vector.unfilteredCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("una página más allá del total no es el estado vacío")
    void shouldKeepTheTotalWhenThePageIsPastTheResults() {
        vector.next = new StrategyResult(List.of(hit("1", "Drama")), 10, Map.of());
        SearchResult result = service.search(vector("un barco", 2));

        assertThat(result.hits()).isEmpty();
        assertThat(result.total()).isEqualTo(10);
        assertThat(result.empty()).isNull();
        assertThat(result.pagination().range()).isEqualTo("0–0 de 10");
    }

    @Test
    @DisplayName("un texto vacío no llama a la estrategia")
    void shouldRejectABlankQueryBeforeCallingTheStrategy() {
        SearchQuery query = fullText("  ", 1, SearchFilters.none());
        assertThatThrownBy(() -> service.search(query)).isInstanceOf(SearchValidationException.class);
        assertThat(fullText.searches).isZero();
    }

    @Test
    @DisplayName("quitar los filtros conserva el texto y el modo")
    void shouldDropOnlyTheFiltersWhenConfirmingAnEmptyResult() {
        SearchQuery open = fullText("hola", 1, drama()).withoutFilters();

        assertThat(open.filters().isEmpty()).isTrue();
        assertThat(open.text()).isEqualTo("hola");
        assertThat(open.mode()).isEqualTo(SearchMode.FULL_TEXT);
    }

    private static SearchQuery fullText(String text, int page, SearchFilters filters) {
        return new SearchQuery(
                text,
                SearchMode.FULL_TEXT,
                new FullTextSettings(MatchType.MATCH, TextField.TITLE, TextOperator.OR),
                null,
                filters,
                page,
                SearchQuery.PAGE_25);
    }

    private static SearchQuery vector(String text, int page) {
        return vector(text, 100, page, SearchQuery.PAGE_25);
    }

    private static SearchQuery vector(String text, int k, int page, int pageSize) {
        return new SearchQuery(
                text,
                SearchMode.VECTOR,
                null,
                new KnnSettings(VectorField.PLOT_1024, k, Math.max(k, 200)),
                SearchFilters.none(),
                page,
                pageSize);
    }

    private static SearchFilters drama() {
        return new SearchFilters(List.of(), List.of("Drama"), List.of(), List.of(), null, null);
    }

    private static List<RankedHit> hits(int count) {
        List<RankedHit> hits = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            hits.add(hit(Integer.toString(index), "Action"));
        }
        return hits;
    }

    private static List<RankedHit> thirty() {
        List<RankedHit> hits = new ArrayList<>();
        for (int index = 0; index < 30; index++) {
            String genre = "Action";
            if (index >= 25) {
                genre = "Drama";
            }
            hits.add(hit(Integer.toString(index), genre));
        }
        return hits;
    }

    private static RankedHit hit(String id, String genre) {
        return RankedHit.scored(movie(id, genre), 1.0);
    }

    private static Movie movie(String id, String genre) {
        return new Movie(id, null, id, null, null, LocalDate.parse("1997-12-19"), null, null, false, null,
                null, null, "sinopsis", List.of(genre), null, null, null, null, null, null,
                null, null, "PG-13", null, "/p.jpg", null, null, null, List.of("en"), null, null, null, null);
    }

    private static Aggregate terms(String label, long count) {
        return Aggregate.of(agg -> agg.sterms(terms -> terms.buckets(buckets -> buckets.array(List.of(
                StringTermsBucket.of(bucket -> bucket.key(FieldValue.of(label)).docCount(count)))))));
    }

    private static final class FakeStrategy implements SearchStrategy {

        private final SearchMode mode;
        private StrategyResult next = new StrategyResult(List.of(), 0, Map.of());
        private long unfiltered;
        private int searches;
        private int unfilteredCalls;

        private FakeStrategy(SearchMode mode) {
            this.mode = mode;
        }

        @Override
        public SearchMode mode() {
            return mode;
        }

        @Override
        public StrategyResult search(SearchQuery query) {
            searches++;
            return next;
        }

        @Override
        public long foundWithoutFilters(SearchQuery query, float[] vector) {
            unfilteredCalls++;
            return unfiltered;
        }
    }
}
