package com.bigfito.elastiflix.web.detail;

import java.time.LocalDate;
import java.util.List;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.core.model.MovieCollection;
import com.bigfito.elastiflix.web.layout.PosterUrls;
import com.bigfito.elastiflix.web.search.SearchForm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias de {@link MovieDetailPage}: formateos de moneda, N/A y estado de búsqueda.
 */
class MovieDetailPageTest {

    private final ElastiflixProperties properties = new ElastiflixProperties(
            null, null, null, null, new ElastiflixProperties.Poster("https://image.tmdb.org/t/p/w342"), null, null);
    private final PosterUrls posters = new PosterUrls(properties);
    private final MovieDetailPage page = new MovieDetailPage(posters);

    @Test
    @DisplayName("formatea presupuesto y recaudación en USD con miles, y en 0 o nulo muestra N/A")
    void shouldFormatCurrencyInUsdOrNotAvailable() {
        Movie movieWithMoney = movieBuilder()
                .budget(160_000_000.0)
                .revenue(825_532_764.0)
                .build();
        MovieDetailView viewWithMoney = page.build(movieWithMoney, searchForm());

        assertThat(factValue(viewWithMoney.numbers(), "budget")).isEqualTo("160 000 000 USD");
        assertThat(factValue(viewWithMoney.numbers(), "revenue")).isEqualTo("825 532 764 USD");

        Movie movieZeroMoney = movieBuilder()
                .budget(0.0)
                .revenue(null)
                .build();
        MovieDetailView viewZeroMoney = page.build(movieZeroMoney, searchForm());

        assertThat(factValue(viewZeroMoney.numbers(), "budget")).isEqualTo("N/A");
        assertThat(factValue(viewZeroMoney.numbers(), "revenue")).isEqualTo("N/A");
    }

    @Test
    @DisplayName("los campos ausentes se muestran como N/A")
    void shouldDisplayNotAvailableForMissingFields() {
        Movie emptyMovie = new Movie("999", null, "Untitled", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        MovieDetailView view = page.build(emptyMovie, searchForm());

        assertThat(factValue(view.facts(), "release_date")).isEqualTo("N/A");
        assertThat(factValue(view.facts(), "runtime")).isEqualTo("N/A");
        assertThat(factValue(view.facts(), "status")).isEqualTo("N/A");
        assertThat(factValue(view.facts(), "rating")).isEqualTo("N/A");
        assertThat(factValue(view.facts(), "adult")).isEqualTo("N/A");
        assertThat(factValue(view.facts(), "video")).isEqualTo("N/A");
        assertThat(factValue(view.production(), "production_companies")).isEqualTo("N/A");
        assertThat(view.hasCollection()).isFalse();
        assertThat(view.collection()).isEmpty();
    }

    @Test
    @DisplayName("conserva el estado de búsqueda y genera el ancla de retorno #r-{id}")
    void shouldPreserveSearchStateAndReturnHref() {
        SearchForm form = new SearchForm(
                "matrix", "fulltext", "match", "title", "or", "plot_256", "100", "200", "2", "50",
                List.of("false"), List.of("Action"), List.of("en"), List.of("R"), "1999-01-01", "2003-12-31");

        Movie movie = movieBuilder().id("603").build();
        MovieDetailView view = page.build(movie, form);

        assertThat(view.returnHref()).contains("/buscar?");
        assertThat(view.returnHref()).contains("q=matrix");
        assertThat(view.returnHref()).contains("pagina=2");
        assertThat(view.returnHref()).contains("tamano=50");
        assertThat(view.returnHref()).endsWith("#r-603");

        assertThat(view.searchState()).extracting(SearchStateChip::label)
                .containsExactly("Consulta", "Modo", "Ajustes", "Filtros", "Página");
        assertThat(stateValue(view.searchState(), "Consulta")).isEqualTo("«matrix»");
        assertThat(stateValue(view.searchState(), "Modo")).isEqualTo("Full-text");
        assertThat(stateValue(view.searchState(), "Página")).isEqualTo("Página 2 · 50 por página");
    }

    @Test
    @DisplayName("procesa colección existente con filas completas")
    void shouldProcessExistingCollection() {
        MovieCollection coll = new MovieCollection(12345L, "The Matrix Collection", "/back.jpg", "/poster.jpg");
        Movie movie = movieBuilder().belongsToCollection(coll).build();

        MovieDetailView view = page.build(movie, searchForm());

        assertThat(view.hasCollection()).isTrue();
        assertThat(view.collName()).isEqualTo("The Matrix Collection");
        assertThat(factValue(view.collection(), "id")).isEqualTo("12345");
        assertThat(factValue(view.collection(), "name")).isEqualTo("The Matrix Collection");
        assertThat(factValue(view.collection(), "poster_path")).isEqualTo("/poster.jpg");
        assertThat(factValue(view.collection(), "backdrop_path")).isEqualTo("/back.jpg");
    }

    private static String factValue(List<FactRow> rows, String field) {
        return rows.stream()
                .filter(r -> r.field().equals(field))
                .findFirst()
                .map(FactRow::value)
                .orElse(null);
    }

    private static String stateValue(List<SearchStateChip> chips, String label) {
        return chips.stream()
                .filter(c -> c.label().equals(label))
                .findFirst()
                .map(SearchStateChip::value)
                .orElse(null);
    }

    private static SearchForm searchForm() {
        return new SearchForm("inception", "fulltext", "match", "title", "or", "plot_256",
                "100", "200", "1", "25", List.of(), List.of(), List.of(), List.of(), "", "");
    }

    private static TestMovieBuilder movieBuilder() {
        return new TestMovieBuilder();
    }

    private static final class TestMovieBuilder {
        private String id = "27205";
        private String title = "Inception";
        private Double budget = 160_000_000.0;
        private Double revenue = 825_532_764.0;
        private MovieCollection belongsToCollection = null;

        TestMovieBuilder id(String id) {
            this.id = id;
            return this;
        }

        TestMovieBuilder budget(Double budget) {
            this.budget = budget;
            return this;
        }

        TestMovieBuilder revenue(Double revenue) {
            this.revenue = revenue;
            return this;
        }

        TestMovieBuilder belongsToCollection(MovieCollection coll) {
            this.belongsToCollection = coll;
            return this;
        }

        Movie build() {
            return new Movie(
                    id, "tt1375666", title, "Inception", "en",
                    LocalDate.parse("2010-07-15"), 148, "Released", false, false,
                    "Your mind is the scene of the crime.", "Cobb is a skilled thief", "Inception plot",
                    List.of("Action", "Sci-Fi"), List.of(), List.of("Leonardo DiCaprio"), belongsToCollection,
                    budget, revenue, 75.2, 8.4, 32000, "PG-13",
                    "https://inception.com", "/poster.jpg", "/backdrop.jpg",
                    List.of("Warner Bros."), List.of("US"), List.of("en"),
                    null, null, null, null);
        }
    }
}
