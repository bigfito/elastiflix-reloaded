package com.bigfito.elastiflix.web.detail;

import java.time.LocalDate;
import java.util.List;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.core.model.MovieCollection;
import com.bigfito.elastiflix.web.layout.PosterUrls;
import com.bigfito.elastiflix.web.locale.UiMessages;
import com.bigfito.elastiflix.web.search.SearchForm;
import org.springframework.context.support.ResourceBundleMessageSource;

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
    private final MovieDetailPage page = new MovieDetailPage(posters, testMessages());

    private static UiMessages testMessages() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasenames("messages");
        source.setDefaultEncoding("UTF-8");
        return new UiMessages(source);
    }

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
    @DisplayName("un Movie vacío muestra «N/A» en cada campo, en la ficha, la cabecera y la columna derecha")
    void shouldShowNotAvailableInEveryFieldOfAnEmptyMovie() {
        // given
        Movie emptyMovie = Movie.identified("999");

        // when
        MovieDetailView view = page.build(emptyMovie, searchForm());

        // then: cabecera
        assertThat(view.title()).isEqualTo("N/A");
        assertThat(view.voteAverage()).isEqualTo("N/A");
        assertThat(view.voteCount()).isEqualTo("N/A");
        assertThat(view.popularity()).isEqualTo("N/A");
        assertThat(view.genres()).isEmpty();
        // then: columna derecha
        assertThat(view.overview()).isEqualTo("N/A");
        assertThat(view.plot()).isEqualTo("N/A");
        assertThat(view.cast()).isEmpty();
        assertThat(view.keywords()).isEmpty();
        assertThat(view.notAvailable()).isEqualTo("N/A");
        // then: todas las filas de los paneles, salvo el id
        assertThat(view.facts()).filteredOn(row -> !"id".equals(row.field()))
                .allSatisfy(row -> assertThat(row.value()).isEqualTo("N/A"));
        assertThat(view.production()).allSatisfy(row -> assertThat(row.value()).isEqualTo("N/A"));
        assertThat(view.numbers()).allSatisfy(row -> assertThat(row.value()).isEqualTo("N/A"));
        assertThat(view.images()).allSatisfy(row -> assertThat(row.value()).isEqualTo("N/A"));
        assertThat(factValue(view.numbers(), "vote_average")).isEqualTo("N/A");
        assertThat(factValue(view.numbers(), "vote_count")).isEqualTo("N/A");
        assertThat(factValue(view.numbers(), "popularity")).isEqualTo("N/A");
    }

    @Test
    @DisplayName("el total del reparto y de las palabras clave llega con el separador de miles del idioma")
    void shouldGroupCastAndKeywordCountsByLocale() {
        // given
        Movie movie = movieBuilder().cast(names(1000)).build();

        // when
        MovieDetailView spanish = page.build(movie, searchForm(), java.util.Locale.forLanguageTag("es"));
        MovieDetailView english = page.build(movie, searchForm(), java.util.Locale.ENGLISH);

        // then
        assertThat(spanish.castCount()).isEqualTo("1 000");
        assertThat(english.castCount()).isEqualTo("1,000");
        assertThat(spanish.keywordCount()).isEqualTo("0");
        assertThat(spanish.hasMoreCast()).isTrue();
    }

    @Test
    @DisplayName("textos y listas en blanco cuentan como vacíos: «N/A»")
    void shouldTreatBlankTextsAsNotAvailable() {
        // given
        Movie blank = new Movie("1", " ", " ", " ", " ", null, null, " ", null, null,
                " ", " ", " ", List.of(), List.of(), List.of(), null, null, null, null, null, null, " ",
                " ", null, null, List.of(), List.of(), List.of(), null, null, null, null);

        // when
        MovieDetailView view = page.build(blank, searchForm());

        // then
        assertThat(view.title()).isEqualTo("N/A");
        assertThat(view.originalTitle()).isEmpty();
        assertThat(view.tagline()).isEmpty();
        assertThat(view.overview()).isEqualTo("N/A");
        assertThat(view.plot()).isEqualTo("N/A");
        assertThat(factValue(view.facts(), "tagline")).isEqualTo("N/A");
        assertThat(factValue(view.production(), "spoken_languages")).isEqualTo("N/A");
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
        assertThat(stateValue(view.searchState(), "Página")).isEqualTo("2 · 50 por página");
    }

    @Test
    @DisplayName("el chip «Ajustes» sigue el mockup: la híbrida lleva tipo, operador, vector, k y candidatos")
    void shouldDescribeTheSettingsOfEachModeLikeTheMockup() {
        // when
        String fullText = stateValue(page.build(movieBuilder().build(), form("fulltext")).searchState(), "Ajustes");
        String vector = stateValue(page.build(movieBuilder().build(), form("vectorial")).searchState(), "Ajustes");
        String hybrid = stateValue(page.build(movieBuilder().build(), form("hibrida")).searchState(), "Ajustes");

        // then
        assertThat(fullText).isEqualTo("match · title · AND");
        assertThat(vector).isEqualTo("plot_512 · k=50 · num_candidates=400");
        assertThat(hybrid).isEqualTo("match · AND · plot_512 · k=50 · num_candidates=400");
    }

    @Test
    @DisplayName("un homepage con javascript: no se enlaza: sin botón y «N/A» en la ficha")
    void shouldNotLinkAJavascriptHomepage() {
        Movie movie = movieBuilder().homepage("javascript:alert(document.domain)").build();

        MovieDetailView view = page.build(movie, searchForm());

        assertThat(view.hasHomepage()).isFalse();
        assertThat(view.homepage()).isEmpty();
        FactRow row = factRow(view.facts(), "homepage");
        assertThat(row.isLink()).isFalse();
        assertThat(row.href()).isEmpty();
        assertThat(row.value()).isEqualTo("N/A");
    }

    @Test
    @DisplayName("un homepage https se enlaza en el botón y en la ficha")
    void shouldLinkAnHttpsHomepage() {
        Movie movie = movieBuilder().homepage("https://inception.com").build();

        MovieDetailView view = page.build(movie, searchForm());

        assertThat(view.hasHomepage()).isTrue();
        assertThat(view.homepage()).isEqualTo("https://inception.com");
        FactRow row = factRow(view.facts(), "homepage");
        assertThat(row.isLink()).isTrue();
        assertThat(row.href()).isEqualTo("https://inception.com");
    }

    @Test
    @DisplayName("una ruta de imagen que no es de TMDB se trata como ausente: marcador y «N/A»")
    void shouldTreatAnInvalidImagePathAsMissing() {
        // given
        Movie movie = movieBuilder().images("/p.jpg') ; x", "/b.jpg');background:url('//evil.test/a.png").build();

        // when
        MovieDetailView view = page.build(movie, searchForm());

        // then
        assertThat(view.posterUrl()).isEmpty();
        assertThat(view.backdropUrl()).isEmpty();
        assertThat(factValue(view.images(), "poster_path")).isEqualTo("N/A");
        assertThat(factValue(view.images(), "backdrop_path")).isEqualTo("N/A");
    }

    @Test
    @DisplayName("una ruta de TMDB válida arma las URL del póster y del fondo")
    void shouldBuildImageUrlsForValidPaths() {
        // when
        MovieDetailView view = page.build(movieBuilder().build(), searchForm());

        // then
        assertThat(view.posterUrl()).isEqualTo("https://image.tmdb.org/t/p/w342/poster.jpg");
        assertThat(view.backdropUrl()).isEqualTo("https://image.tmdb.org/t/p/w342/backdrop.jpg");
        assertThat(factRow(view.images(), "backdrop_path").href())
                .isEqualTo("https://image.tmdb.org/t/p/w342/backdrop.jpg");
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

    @Test
    @DisplayName("con Locale.ENGLISH formatea moneda con comas y estado de búsqueda en inglés")
    void shouldFormatInEnglish() {
        SearchForm form = new SearchForm(
                "matrix", "fulltext", "match", "title", "or", "plot_256", "100", "200", "2", "50",
                List.of("false"), List.of("Action"), List.of("en"), List.of("R"), "1999-01-01", "2003-12-31");

        Movie movie = movieBuilder()
                .id("603")
                .budget(160_000_000.0)
                .revenue(825_532_764.0)
                .build();
        MovieDetailView view = page.build(movie, form, java.util.Locale.ENGLISH);

        assertThat(factValue(view.numbers(), "budget")).isEqualTo("160,000,000 USD");
        assertThat(factValue(view.numbers(), "revenue")).isEqualTo("825,532,764 USD");

        assertThat(view.searchState()).extracting(SearchStateChip::label)
                .containsExactly("Query", "Mode", "Settings", "Filters", "Page");
        assertThat(stateValue(view.searchState(), "Query")).isEqualTo("“matrix”");
        assertThat(stateValue(view.searchState(), "Mode")).isEqualTo("Full-text");
        assertThat(stateValue(view.searchState(), "Page")).isEqualTo("2 · 50 per page");

        assertThat(factLabel(view.facts(), "id")).isEqualTo("Id");
        assertThat(factLabel(view.facts(), "release_date")).isEqualTo("Release date");
        assertThat(factLabel(view.facts(), "adult")).isEqualTo("Adult content");
        assertThat(factValue(view.facts(), "adult")).isEqualTo("No");
        assertThat(factLabel(view.numbers(), "budget")).isEqualTo("Budget");
        assertThat(factLabel(view.production(), "production_companies")).isEqualTo("Production companies");
    }

    private static String factLabel(List<FactRow> rows, String field) {
        return rows.stream()
                .filter(r -> r.field().equals(field))
                .findFirst()
                .map(FactRow::label)
                .orElse(null);
    }

    private static FactRow factRow(List<FactRow> rows, String field) {
        return rows.stream()
                .filter(r -> r.field().equals(field))
                .findFirst()
                .orElseThrow();
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

    private static SearchForm form(String mode) {
        return new SearchForm("inception", mode, "match", "title", "and", "plot_512",
                "50", "400", "1", "25", List.of(), List.of(), List.of(), List.of(), "", "");
    }

    private static List<String> names(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count).mapToObj(n -> "Persona " + n).toList();
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
        private String homepage = "https://inception.com";
        private List<String> cast = List.of("Leonardo DiCaprio");
        private String posterPath = "/poster.jpg";
        private String backdropPath = "/backdrop.jpg";

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

        TestMovieBuilder homepage(String homepage) {
            this.homepage = homepage;
            return this;
        }

        TestMovieBuilder cast(List<String> cast) {
            this.cast = cast;
            return this;
        }

        TestMovieBuilder images(String posterPath, String backdropPath) {
            this.posterPath = posterPath;
            this.backdropPath = backdropPath;
            return this;
        }

        Movie build() {
            return new Movie(
                    id, "tt1375666", title, "Inception", "en",
                    LocalDate.parse("2010-07-15"), 148, "Released", false, false,
                    "Your mind is the scene of the crime.", "Cobb is a skilled thief", "Inception plot",
                    List.of("Action", "Sci-Fi"), List.of(), cast, belongsToCollection,
                    budget, revenue, 75.2, 8.4, 32000, "PG-13",
                    homepage, posterPath, backdropPath,
                    List.of("Warner Bros."), List.of("US"), List.of("en"),
                    null, null, null, null);
        }
    }
}
