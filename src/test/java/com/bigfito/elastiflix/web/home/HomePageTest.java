package com.bigfito.elastiflix.web.home;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.web.layout.LayoutAdvice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Inicio muestra los textos del mockup y enlaza cada sugerencia con su modo.
 */
@WebMvcTest(controllers = HomeController.class, properties = {
        "elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test"})
@Import({LayoutAdvice.class, SuggestionCatalog.class})
class HomePageTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogStatusService catalog;

    @BeforeEach
    void catalogAnswers() {
        when(catalog.status()).thenReturn(new CatalogStatus(
                "elastiflix-reloaded", "elastiflix-reloaded-1", 4322L, true, null));
    }

    @Test
    @DisplayName("la página trae los tres modos, la ayuda y el aviso de consulta vacía")
    void shouldShowTheModeCopy() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Full-text (léxica)")))
                .andExpect(content().string(containsString("Retrieval con BM25")))
                .andExpect(content().string(containsString("k-NN (k vecinos más cercanos)")))
                .andExpect(content().string(containsString("Reciprocal Rank Fusion")))
                .andExpect(content().string(containsString(
                        "Escribe qué película buscas antes de pulsar Buscar.")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("id=\"query-error\" class=\"alert\" role=\"alert\"")))
                .andExpect(content().string(containsString("aria-describedby=\"query-error\"")))
                .andExpect(content().string(containsString("aria-describedby=\"knn-error\"")));
    }

    @Test
    @DisplayName("cada modo tiene una sugerencia que abre /buscar con sus parámetros")
    void shouldLinkEachSuggestionToSearch() throws Exception {
        mvc.perform(get("/"))
                .andExpect(content().string(containsString(
                        "/buscar?q=Spirited%20Away&amp;modo=fulltext&amp;tipo=match&amp;operador=or&amp;campo=title")))
                .andExpect(content().string(containsString("OR: 57 resultados")))
                .andExpect(content().string(containsString("AND: 2 resultados")))
                .andExpect(content().string(containsString("título original · en title: 3")))
                .andExpect(content().string(containsString("data-key=\"fulltext-match-plot-or\"")))
                .andExpect(content().string(containsString(
                        "/buscar?q=ogre%20donkey%20princess&amp;modo=fulltext&amp;tipo=match&amp;operador=or"
                                + "&amp;campo=plot")))
                .andExpect(content().string(containsString("OR: 190 resultados")))
                .andExpect(content().string(containsString("data-value=\"plot\"")))
                .andExpect(content().string(containsString("modo=vectorial")))
                .andExpect(content().string(containsString("vector=plot_1024")))
                .andExpect(content().string(containsString("Titanic: 3.º lugar")))
                .andExpect(content().string(containsString("Titanic: 4.º lugar")))
                .andExpect(content().string(containsString("Life of Pi: fuera del top 100")))
                .andExpect(content().string(containsString("Inception: 3.º lugar")))
                .andExpect(content().string(containsString("modo=hibrida")))
                .andExpect(content().string(containsString("Titanic: 1.º vectorial, 1.º léxico")))
                .andExpect(content().string(containsString("href=\"/buscar?q=un%20barco%20de%20lujo%20choca%20con%20un"
                        + "%20iceberg%20en%20su%20viaje%20inaugural&amp;modo=vectorial&amp;vector=plot_256&amp;k=100"
                        + "&amp;candidatos=200\"")))
                .andExpect(content().string(containsString("href=\"/buscar?q=dark%20knight%20joker%20caos%20en%20la"
                        + "%20ciudad&amp;modo=hibrida&amp;tipo=match&amp;campo=title&amp;operador=or&amp;vector=plot_256"
                        + "&amp;k=100&amp;candidatos=200\"")));
    }

    @Test
    @DisplayName("con lang=en muestra Inicio y las sugerencias verificadas en inglés")
    void shouldShowEnglishModeCopyAndSuggestionsWhenLangIsEn() throws Exception {
        mvc.perform(get("/").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"en\">")))
                .andExpect(content().string(containsString("Full-text (lexical)")))
                .andExpect(content().string(containsString("Retrieval with BM25:")))
                .andExpect(content().string(containsString("k-NN (k nearest neighbors):")))
                .andExpect(content().string(containsString("Type the movie you&#39;re looking for before you press Search.")))
                .andExpect(content().string(containsString("Try these queries")))
                .andExpect(content().string(containsString("OR: 57 results")))
                .andExpect(content().string(containsString("Titanic: 5th place")))
                .andExpect(content().string(containsString("Titanic: 2nd place")))
                .andExpect(content().string(containsString("Titanic: 3rd place")))
                .andExpect(content().string(containsString("Inception: 4th place")))
                .andExpect(content().string(containsString("Inception: 3rd place")))
                .andExpect(content().string(containsString("Inception: 1st place")))
                .andExpect(content().string(containsString("in Spanish · WALL·E: 1st place")))
                .andExpect(content().string(containsString("Titanic: 1st vector, 4th lexical")))
                .andExpect(content().string(containsString("AND: lexical 0 · vector decides")));
    }
}
