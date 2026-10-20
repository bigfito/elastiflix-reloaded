package com.bigfito.elastiflix.web.search;

import java.time.LocalDate;
import java.util.List;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.search.facet.FacetValue;
import com.bigfito.elastiflix.search.facet.Facets;
import com.bigfito.elastiflix.search.page.Pagination;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.result.EmptyState;
import com.bigfito.elastiflix.search.result.SearchHit;
import com.bigfito.elastiflix.search.result.SearchResult;
import com.bigfito.elastiflix.search.result.SearchService;
import com.bigfito.elastiflix.search.strategy.ComponentScore;
import com.bigfito.elastiflix.web.layout.LayoutAdvice;
import com.bigfito.elastiflix.web.layout.PosterUrls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Resultados muestra los tres modos, los dos vacíos y, si candidatos es menor que k, el «qué pasó» junto al campo.
 */
@WebMvcTest(controllers = SearchController.class, properties = {
        "elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test",
        "ELASTIFLIX_POSTER_BASE_URL=https://posters.test/w92"})
@Import({LayoutAdvice.class, SearchQueryMapper.class, ResultsPage.class, ResultCards.class, PosterUrls.class,
        com.bigfito.elastiflix.web.locale.LocaleConfig.class})
@EnableConfigurationProperties(ElastiflixProperties.class)
class ResultsPageTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SearchService searches;

    @MockitoBean
    private CatalogStatusService catalog;

    @BeforeEach
    void catalogAnswers() {
        when(catalog.status()).thenReturn(new CatalogStatus("elastiflix-reloaded", "idx", 4322L, true, null));
    }

    @Test
    @DisplayName("full-text muestra BM25, el póster y el enlace de tamaño en la página 1")
    void shouldRenderFullText() throws Exception {
        when(searches.search(any(SearchQuery.class))).thenReturn(found(null, null, null, "match · title"));

        mvc.perform(get("/buscar").param("q", "titanic").param("modo", "fulltext").param("genero", "Drama"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"score-title\">Puntuación</span>")))
                .andExpect(content().string(containsString("Ordenados por puntuación BM25")))
                .andExpect(content().string(containsString("Mostrando 1–1 de 1 resultados")))
                .andExpect(content().string(containsString("Filtros aplicados:")))
                .andExpect(content().string(containsString("Géneros: Drama")))
                .andExpect(content().string(containsString("Valores actualizados con los resultados filtrados.")))
                .andExpect(content().string(containsString("<select id=\"page-size\" class=\"inp\">")))
                .andExpect(content().string(containsString("7.9 · 1 000 votos")))
                .andExpect(content().string(containsString("En estos resultados: de 1997-01-01 a 1997-12-19")))
                .andExpect(content().string(not(containsString("1997-12-19."))))
                .andExpect(content().string(containsString("src=\"https://posters.test/w92/t.jpg\" alt=\"Titanic\"")))
                .andExpect(content().string(containsString("pagina=1")))
                .andExpect(content().string(containsString("tamano=50")))
                .andExpect(content().string(containsString("tamano=100")))
                .andExpect(content().string(containsString("APLICAR")))
                .andExpect(content().string(containsString("id=\"r-1\"")))
                .andExpect(content().string(containsString("href=\"/pelicula/1?q=titanic")))
                .andExpect(content().string(containsString("M4 7V5h16v2")))
                .andExpect(content().string(not(containsString("Ver todos los géneros"))))
                .andExpect(content().string(not(containsString("Ver todos los idiomas"))));
    }

    @Test
    @DisplayName("los votos y el título del conteo agrupan los miles: «21 911 votos» y «1 234 resultados»")
    void shouldGroupThousandsInVotesAndCount() throws Exception {
        SearchHit hit = new SearchHit(
                "1", "Titanic", "Titanic", "Un barco", "/t.jpg",
                LocalDate.of(1997, 12, 19), 194, List.of("Drama"), 7.9, 21_911, "PG-13",
                12.5, null, null, null);
        when(searches.search(any(SearchQuery.class))).thenReturn(new SearchResult(List.of(hit), 1234,
                facets(), new Pagination(1, 50, 1234, 25, "1–25 de 1234"), List.of("match · title"), null));

        mvc.perform(get("/buscar").param("q", "titanic"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("7.9 · 21 911 votos")))
                .andExpect(content().string(containsString("<h1 id=\"r-title\" aria-live=\"polite\">1 234 resultados</h1>")));
    }

    @Test
    @DisplayName("el título del conteo se anuncia, rotula la sección, y APLICAR va tras su separador")
    void shouldAnnounceTheCountAndLabelTheSection() throws Exception {
        when(searches.search(any(SearchQuery.class))).thenReturn(found(null, null, null, "match · title"));

        mvc.perform(get("/buscar").param("q", "titanic"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<section aria-labelledby=\"r-title\">")))
                .andExpect(content().string(containsString("<h1 id=\"r-title\" aria-live=\"polite\">1 resultado</h1>")))
                .andExpect(content().string(not(containsString("aria-describedby=\"q-error\""))))
                .andExpect(content().string(not(containsString("id=\"q-error\""))))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).containsPattern(
                        "<div class=\"apply-row\">\\s*<p id=\"dirty\" class=\"pending\" hidden>Tienes cambios sin aplicar.</p>"
                                + "\\s*<button class=\"apply\" type=\"submit\">APLICAR</button>\\s*</div>"));
    }

    @Test
    @DisplayName("Anterior, Siguiente, los números y el tamaño 50 conservan genero y desde")
    void shouldKeepTheFiltersInEveryPageLink() throws Exception {
        SearchResult second = found(null, null, null, "match · title");
        when(searches.search(any(SearchQuery.class))).thenReturn(new SearchResult(second.hits(), 100,
                second.facets(), new Pagination(2, 4, 100, 25, "26–50 de 100"), second.adjustments(), null));

        mvc.perform(get("/buscar").param("q", "titanic").param("genero", "Drama").param("desde", "1990-01-01")
                        .param("pagina", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "href=\"" + link(1, 25) + "\" aria-label=\"Página anterior\"")))
                .andExpect(content().string(containsString(
                        "href=\"" + link(3, 25) + "\" aria-label=\"Página siguiente\"")))
                .andExpect(content().string(containsString("href=\"" + link(4, 25) + "\">4</a>")))
                .andExpect(content().string(containsString("data-href=\"" + link(1, 50) + "\"")));
    }

    @Test
    @DisplayName("«Ver todos» solo aparece cuando hay más de seis valores")
    void shouldOfferExpandOnlyPastSixValues() throws Exception {
        when(searches.search(any(SearchQuery.class))).thenReturn(found(
                null, null, null, "match · title", sevenGenres()));

        mvc.perform(get("/buscar").param("q", "titanic").param("modo", "fulltext"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ver todos los géneros")))
                .andExpect(content().string(containsString("aria-expanded=\"false\"")))
                .andExpect(content().string(not(containsString("Ver todos los idiomas"))));
    }

    @Test
    @DisplayName("vectorial e híbrida muestran su puntuación y su leyenda")
    void shouldRenderVectorAndHybrid() throws Exception {
        when(searches.search(any(SearchQuery.class))).thenReturn(
                found(new ComponentScore(1.2, "2"), new ComponentScore(0.8, "1"), 0.03, "RRF"));

        mvc.perform(get("/buscar").param("q", "barco").param("modo", "vectorial"))
                .andExpect(content().string(containsString("class=\"score-title\">Similitud</span>")))
                .andExpect(content().string(containsString("Similitud k-NN")))
                .andExpect(content().string(containsString("stroke=\"#1E7A5C\"")))
                .andExpect(content().string(containsString("Ordenados por similitud k-NN")));

        mvc.perform(get("/buscar").param("q", "barco").param("modo", "hibrida"))
                .andExpect(content().string(containsString("class=\"score-title\">Puntuación en cada lista</span>")))
                .andExpect(content().string(containsString("class=\"rrf-value\"")))
                .andExpect(content().string(containsString("posición en su lista: #2")))
                .andExpect(content().string(containsString("Ordenados por fusión RRF")));
    }

    @Test
    @DisplayName("los dos estados vacíos usan el texto del catálogo de búsqueda")
    void shouldRenderBothEmptyStates() throws Exception {
        when(searches.search(any(SearchQuery.class))).thenReturn(empty(EmptyState.none("zzz", SearchMode.FULL_TEXT)));
        mvc.perform(get("/buscar").param("q", "zzz"))
                .andExpect(content().string(containsString("No encontramos películas")));

        when(searches.search(any(SearchQuery.class))).thenReturn(empty(EmptyState.filtered()));
        mvc.perform(get("/buscar").param("q", "titanic").param("genero", "Drama"))
                .andExpect(content().string(containsString("Ningún resultado cumple los filtros")));

        when(searches.search(any(SearchQuery.class))).thenReturn(empty(EmptyState.none("zzz", SearchMode.VECTOR)));
        mvc.perform(get("/buscar").param("q", "zzz").param("modo", "vectorial"))
                .andExpect(content().string(not(containsString("La búsqueda vectorial devuelve como máximo"))));
    }

    @Test
    @DisplayName("un póster vacío muestra el marcador del mockup con aria-label")
    void shouldShowThePosterMarkWhenThePathIsEmpty() throws Exception {
        SearchHit hit = new SearchHit(
                "1", "Titanic", "Titanic", "Un barco", "",
                LocalDate.of(1997, 12, 19), 194, List.of("Drama"), 7.9, 1000, "PG-13",
                12.5, null, null, null);
        when(searches.search(any(SearchQuery.class))).thenReturn(
                new SearchResult(List.of(hit), 1, facets(), page(1), List.of("match · title"), null));

        mvc.perform(get("/buscar").param("q", "titanic"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "<div class=\"poster missing\" role=\"img\" aria-label=\"Póster de Titanic\"><svg")))
                .andExpect(content().string(containsString("<span>poster_path</span></div>")))
                .andExpect(content().string(not(containsString("<img src=\"\""))))
                .andExpect(content().string(not(containsString("onerror"))));
    }

    @Test
    @DisplayName("una consulta vacía marca la caja con aria-invalid y deja un h1")
    void shouldMarkTheQueryBoxWhenTheQueryIsEmpty() throws Exception {
        mvc.perform(get("/buscar").param("q", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-describedby=\"q-error\" aria-invalid=\"true\"")))
                .andExpect(content().string(containsString("class=\"sbox compact is-invalid\"")))
                .andExpect(content().string(containsString("<p id=\"q-error\" class=\"alert query-alert\" role=\"alert\">")))
                .andExpect(content().string(containsString("<h1 class=\"visually-hidden\">Resultados</h1>")))
                .andExpect(content().string(containsString(
                        "<span>Escribe qué película buscas antes de pulsar Buscar.</span>")));
    }

    @Test
    @DisplayName("candidatos menor que k muestra solo qué pasó y deja los filtros")
    void shouldShowCandidatesBelowK() throws Exception {
        mvc.perform(get("/buscar")
                        .param("q", "titanic")
                        .param("modo", "vectorial")
                        .param("k", "100")
                        .param("candidatos", "50"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("num_candidates debe ser mayor o igual que k")))
                .andExpect(content().string(containsString("id=\"f-title\"")));
    }

    @Test
    @DisplayName("un rango de fechas invertido deja el formulario y el texto del mockup")
    void shouldKeepTheFiltersWhenTheDateRangeIsReversed() throws Exception {
        mvc.perform(get("/buscar")
                        .param("q", "titanic")
                        .param("desde", "2020-01-02")
                        .param("hasta", "1999-01-01")
                        .param("genero", "Drama"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "La fecha «Desde» no puede ser posterior a «Hasta».")))
                .andExpect(content().string(containsString("role=\"alert\"")))
                .andExpect(content().string(containsString("value=\"2020-01-02\"")))
                .andExpect(content().string(containsString("value=\"1999-01-01\"")))
                .andExpect(content().string(containsString("disabled=\"disabled\"")))
                .andExpect(content().string(not(containsString("aria-describedby"))))
                .andExpect(content().string(containsString("Drama")));
    }

    @Test
    @DisplayName("con facetas vacías y genero=Drama, «Drama 0» sigue marcado y visible")
    void shouldKeepASelectedValueThatTheFacetNoLongerHas() throws Exception {
        when(searches.search(any(SearchQuery.class))).thenReturn(new SearchResult(
                List.of(), 0, noFacets(), page(0), List.of(), EmptyState.filtered()));

        mvc.perform(get("/buscar").param("q", "titanic").param("genero", "Drama"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).containsPattern(
                        "<label class=\"opt\">\\s*<input type=\"checkbox\" name=\"genero\" value=\"Drama\" "
                                + "checked=\"checked\">\\s*<span class=\"opt-label\">Drama</span>\\s*<span class=\"count\">0</span>"));
    }

    @Test
    @DisplayName("seis valores más un marcado que la faceta no trae no muestran «Ver todos»")
    void shouldNotOfferExpandWhenNothingIsHidden() throws Exception {
        when(searches.search(any(SearchQuery.class))).thenReturn(found(null, null, null, "chip", sixGenres()));

        mvc.perform(get("/buscar").param("q", "titanic").param("genero", "Western"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .containsPattern("name=\"genero\" value=\"Western\" checked=\"checked\">")
                        .contains("value=\"Crime\"")
                        .doesNotContain("Ver todos los géneros")
                        .doesNotContain("class=\"opt extra\""));
    }

    @Test
    @DisplayName("un valor marcado en la posición 7 no queda oculto detrás de «Ver todos»")
    void shouldNotHideASelectedValue() throws Exception {
        when(searches.search(any(SearchQuery.class))).thenReturn(found(null, null, null, "chip", sevenGenres()));

        mvc.perform(get("/buscar").param("q", "titanic").param("genero", "Horror"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).containsPattern(
                        "<label class=\"opt\">\\s*<input type=\"checkbox\" name=\"genero\" value=\"Horror\" "
                                + "checked=\"checked\">"));
    }

    @Test
    @DisplayName("con adult=true, «Solo adultos» aparece marcada y «Todos» no")
    void shouldKeepTheAdultOnlyFilterChecked() throws Exception {
        when(searches.search(any(SearchQuery.class))).thenReturn(found(null, null, null, "match · title"));

        mvc.perform(get("/buscar").param("q", "titanic").param("adult", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Adultos: Solo adultos")))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .containsPattern("<input type=\"radio\" name=\"adult\" value=\"true\" checked>"
                                + "<span class=\"opt-label\">Solo adultos</span>")
                        .doesNotContainPattern("name=\"adult\" value=\"\" checked"));
    }

    @Test
    @DisplayName("sin adult=true no aparece la opción «Solo adultos»")
    void shouldNotOfferAdultOnlyByDefault() throws Exception {
        when(searches.search(any(SearchQuery.class))).thenReturn(found(null, null, null, "match · title"));

        mvc.perform(get("/buscar").param("q", "titanic"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Solo adultos"))))
                .andExpect(content().string(containsString(
                        "<input type=\"radio\" name=\"adult\" value=\"\" checked=\"checked\">")));
    }

    @Test
    @DisplayName("con lang=en full-text muestra Score, Sorted by BM25 score, votos en inglés y filtros en inglés")
    void shouldRenderFullTextInEnglish() throws Exception {
        when(searches.search(any(SearchQuery.class))).thenReturn(found(null, null, null, "match · title"));

        mvc.perform(get("/buscar").param("q", "titanic").param("modo", "fulltext").param("genero", "Drama")
                        .param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"score-title\">Score</span>")))
                .andExpect(content().string(containsString("Sorted by BM25 score")))
                .andExpect(content().string(containsString("Showing 1–1 of 1 results")))
                .andExpect(content().string(containsString("Applied filters:")))
                .andExpect(content().string(containsString("Genres: Drama")))
                .andExpect(content().string(containsString("Values updated with the filtered results.")))
                .andExpect(content().string(containsString("Results per page")))
                .andExpect(content().string(containsString("7.9 · 1,000 votes")))
                .andExpect(content().string(containsString("In these results: from 1997-01-01 to 1997-12-19")))
                .andExpect(content().string(containsString("APPLY")));
    }

    @Test
    @DisplayName("con lang=en vectorial e híbrida muestran sus textos, k-NN similarity y outside its top 100")
    void shouldRenderVectorAndHybridInEnglish() throws Exception {
        when(searches.search(any(SearchQuery.class))).thenReturn(
                found(new ComponentScore(1.2, "2"), new ComponentScore(0.8, "fuera de su top 100"), 0.03, "RRF"));

        mvc.perform(get("/buscar").param("q", "barco").param("modo", "vectorial").param("lang", "en"))
                .andExpect(content().string(containsString("class=\"score-title\">Similarity</span>")))
                .andExpect(content().string(containsString("k-NN similarity")))
                .andExpect(content().string(containsString("Sorted by k-NN similarity")));

        mvc.perform(get("/buscar").param("q", "barco").param("modo", "hibrida").param("lang", "en"))
                .andExpect(content().string(containsString("class=\"score-title\">Score in each list</span>")))
                .andExpect(content().string(containsString("position in its list: #2")))
                .andExpect(content().string(containsString("position in its list: outside its top 100")))
                .andExpect(content().string(containsString("Sorted by RRF fusion")));
    }

    @Test
    @DisplayName("con lang=en los estados vacíos muestran We couldn't find any movies y No results match the filters")
    void shouldRenderEmptyStatesInEnglish() throws Exception {
        when(searches.search(any(SearchQuery.class))).thenReturn(empty(EmptyState.none("zzz", SearchMode.FULL_TEXT)));
        mvc.perform(get("/buscar").param("q", "zzz").param("lang", "en"))
                .andExpect(content().string(containsString("We couldn&#39;t find any movies")))
                .andExpect(content().string(containsString("The query “zzz” returned no results with the current settings.")));

        when(searches.search(any(SearchQuery.class))).thenReturn(empty(EmptyState.filtered()));
        mvc.perform(get("/buscar").param("q", "titanic").param("genero", "Drama").param("lang", "en"))
                .andExpect(content().string(containsString("No results match the filters")))
                .andExpect(content().string(containsString("Remove filters")));
    }

    @Test
    @DisplayName("con lang=en la consulta vacía muestra el mensaje de error en inglés")
    void shouldRenderValidationErrorInEnglish() throws Exception {
        mvc.perform(get("/buscar").param("q", "").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enter a movie to search for before pressing Search")));
    }


    private static SearchResult found(ComponentScore lexical, ComponentScore vector, Double rrf, String chip) {
        return found(lexical, vector, rrf, chip, facets());
    }

    private static SearchResult found(
            ComponentScore lexical, ComponentScore vector, Double rrf, String chip, Facets facets) {
        SearchHit hit = new SearchHit(
                "1", "Titanic", "Titanic", "Un barco", "/t.jpg",
                LocalDate.of(1997, 12, 19), 194, List.of("Drama"), 7.9, 1000, "PG-13",
                12.5, lexical, vector, rrf);
        return new SearchResult(List.of(hit), 1, facets, page(1), List.of(chip), null);
    }

    private static Facets sixGenres() {
        return new Facets(
                List.of(
                        new FacetValue("Drama", 1),
                        new FacetValue("Comedy", 1),
                        new FacetValue("Action", 1),
                        new FacetValue("Thriller", 1),
                        new FacetValue("Romance", 1),
                        new FacetValue("Crime", 1)),
                List.of(),
                List.of(),
                List.of(new FacetValue("false", 1)),
                LocalDate.of(1997, 1, 1),
                LocalDate.of(1997, 12, 19));
    }

    private static Facets sevenGenres() {
        return new Facets(
                List.of(
                        new FacetValue("Drama", 1),
                        new FacetValue("Comedy", 1),
                        new FacetValue("Action", 1),
                        new FacetValue("Thriller", 1),
                        new FacetValue("Romance", 1),
                        new FacetValue("Crime", 1),
                        new FacetValue("Horror", 1)),
                List.of(),
                List.of(),
                List.of(new FacetValue("false", 1)),
                LocalDate.of(1997, 1, 1),
                LocalDate.of(1997, 12, 19));
    }

    private static SearchResult empty(EmptyState state) {
        return new SearchResult(List.of(), 0, facets(), page(0), List.of(), state);
    }

    private static Facets facets() {
        return new Facets(
                List.of(new FacetValue("Drama", 1)),
                List.of(),
                List.of(new FacetValue("PG-13", 1)),
                List.of(new FacetValue("false", 1)),
                LocalDate.of(1997, 1, 1),
                LocalDate.of(1997, 12, 19));
    }

    private static Facets noFacets() {
        return new Facets(List.of(), List.of(), List.of(), List.of(), null, null);
    }

    private static String link(int page, int size) {
        return "/buscar?q=titanic&amp;modo=fulltext&amp;tipo=match&amp;campo=title&amp;operador=or"
                + "&amp;vector=plot_256&amp;k=100&amp;candidatos=200&amp;genero=Drama&amp;desde=1990-01-01"
                + "&amp;pagina=" + page + "&amp;tamano=" + size;
    }

    private static Pagination page(long total) {
        int pages = total == 0 ? 0 : 1;
        return new Pagination(1, pages, total, 25, "1–1 de 1");
    }
}
