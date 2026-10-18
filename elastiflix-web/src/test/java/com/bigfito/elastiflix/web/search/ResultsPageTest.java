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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Resultados muestra los tres modos, los dos vacíos y el código si candidatos es menor que k.
 */
@WebMvcTest(SearchController.class)
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

        mvc.perform(get("/buscar").param("q", "titanic").param("modo", "fulltext"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("BM25")))
                .andExpect(content().string(containsString("Ordenados por puntuación BM25")))
                .andExpect(content().string(containsString("alt=\"Titanic\"")))
                .andExpect(content().string(containsString("pagina=1")))
                .andExpect(content().string(containsString("tamano=50")))
                .andExpect(content().string(containsString("tamano=100")))
                .andExpect(content().string(containsString("APLICAR")))
                .andExpect(content().string(containsString("Tienes cambios sin aplicar.")))
                .andExpect(content().string(containsString("Ver todos los géneros")));
    }

    @Test
    @DisplayName("vectorial e híbrida muestran su puntuación y su leyenda")
    void shouldRenderVectorAndHybrid() throws Exception {
        when(searches.search(any(SearchQuery.class))).thenReturn(
                found(new ComponentScore(1.2, "2"), new ComponentScore(0.8, "1"), 0.03, "RRF"));

        mvc.perform(get("/buscar").param("q", "barco").param("modo", "vectorial"))
                .andExpect(content().string(containsString("Similitud k-NN")))
                .andExpect(content().string(containsString("Ordenados por similitud k-NN")));

        mvc.perform(get("/buscar").param("q", "barco").param("modo", "hibrida"))
                .andExpect(content().string(containsString("RRF")))
                .andExpect(content().string(containsString("posición en su lista: 2")))
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
    }

    @Test
    @DisplayName("candidatos menor que k se muestra con EFX-SRCH-003")
    void shouldShowCandidatesBelowK() throws Exception {
        mvc.perform(get("/buscar")
                        .param("q", "titanic")
                        .param("modo", "vectorial")
                        .param("k", "100")
                        .param("candidatos", "50"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("EFX-SRCH-003")))
                .andExpect(content().string(containsString("num_candidates debe ser mayor o igual que k")));
    }

    private static SearchResult found(ComponentScore lexical, ComponentScore vector, Double rrf, String chip) {
        SearchHit hit = new SearchHit(
                "1", "Titanic", "Titanic", "Un barco", "/t.jpg",
                LocalDate.of(1997, 12, 19), 194, List.of("Drama"), 7.9, 1000, "PG-13",
                12.5, lexical, vector, rrf);
        return new SearchResult(List.of(hit), 1, facets(), page(1), List.of(chip), null);
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

    private static Pagination page(long total) {
        int pages = total == 0 ? 0 : 1;
        return new Pagination(1, pages, total, 25, "1–1 de 1");
    }
}
