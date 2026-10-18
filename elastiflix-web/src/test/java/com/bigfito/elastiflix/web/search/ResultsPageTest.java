package com.bigfito.elastiflix.web.search;

import java.time.LocalDate;
import java.util.List;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
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
 * Resultados muestra los tres modos, los dos vacíos y el código si candidatos es menor que k.
 */
@WebMvcTest(controllers = SearchController.class, properties = {
        "elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test"})
@Import({LayoutAdvice.class, SearchQueryMapper.class, ResultsPage.class, ResultCards.class, PosterUrls.class})
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
                .andExpect(content().string(containsString("class=\"eyebrow\">Puntuación</span>")))
                .andExpect(content().string(containsString("Ordenados por puntuación BM25")))
                .andExpect(content().string(containsString("Mostrando 1–1 de 1 resultados")))
                .andExpect(content().string(containsString("Filtros aplicados:")))
                .andExpect(content().string(containsString("Géneros: Drama")))
                .andExpect(content().string(containsString("Valores actualizados con los resultados filtrados.")))
                .andExpect(content().string(containsString("<select id=\"page-size\">")))
                .andExpect(content().string(containsString("7.9 · 1000 votos")))
                .andExpect(content().string(containsString("En estos resultados: de 1997-01-01 a 1997-12-19")))
                .andExpect(content().string(not(containsString("1997-12-19."))))
                .andExpect(content().string(containsString("alt=\"Titanic\"")))
                .andExpect(content().string(containsString("pagina=1")))
                .andExpect(content().string(containsString("tamano=50")))
                .andExpect(content().string(containsString("tamano=100")))
                .andExpect(content().string(containsString("APLICAR")))
                .andExpect(content().string(containsString("Tienes cambios sin aplicar.")))
                .andExpect(content().string(containsString("M4 7V5h16v2")))
                .andExpect(content().string(not(containsString("Ver todos los géneros"))))
                .andExpect(content().string(not(containsString("Ver todos los idiomas"))));
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
                .andExpect(content().string(containsString("class=\"eyebrow\">Similitud</span>")))
                .andExpect(content().string(containsString("Similitud k-NN")))
                .andExpect(content().string(containsString("stroke=\"#1E7A5C\"")))
                .andExpect(content().string(containsString("Ordenados por similitud k-NN")));

        mvc.perform(get("/buscar").param("q", "barco").param("modo", "hibrida"))
                .andExpect(content().string(containsString("class=\"eyebrow\">Puntuación en cada lista</span>")))
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
                        "<label>\\s*<input type=\"checkbox\" name=\"genero\" value=\"Drama\" checked=\"checked\">"
                                + "\\s*<span>Drama</span>\\s*<span>0</span>"));
    }

    @Test
    @DisplayName("un valor marcado en la posición 7 no queda oculto detrás de «Ver todos»")
    void shouldNotHideASelectedValue() throws Exception {
        when(searches.search(any(SearchQuery.class))).thenReturn(found(null, null, null, "chip", sevenGenres()));

        mvc.perform(get("/buscar").param("q", "titanic").param("genero", "Horror"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).containsPattern(
                        "<label>\\s*<input type=\"checkbox\" name=\"genero\" value=\"Horror\" checked=\"checked\">"));
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

    private static Pagination page(long total) {
        int pages = total == 0 ? 0 : 1;
        return new Pagination(1, pages, total, 25, "1–1 de 1");
    }
}
