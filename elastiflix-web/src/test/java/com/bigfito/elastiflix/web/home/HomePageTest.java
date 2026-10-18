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
@WebMvcTest(HomeController.class)
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
                        "Escribe qué película buscas antes de pulsar Buscar.")));
    }

    @Test
    @DisplayName("cada modo tiene una sugerencia que abre /buscar con sus parámetros")
    void shouldLinkEachSuggestionToSearch() throws Exception {
        mvc.perform(get("/"))
                .andExpect(content().string(containsString(
                        "/buscar?q=Spirited%20Away&amp;modo=fulltext&amp;tipo=match&amp;operador=or&amp;campo=title")))
                .andExpect(content().string(containsString("OR: 40 resultados")))
                .andExpect(content().string(containsString("modo=vectorial")))
                .andExpect(content().string(containsString("vector=plot_1024")))
                .andExpect(content().string(containsString("Titanic: 1.º lugar")))
                .andExpect(content().string(containsString("modo=hibrida")))
                .andExpect(content().string(containsString("Titanic: 1.º vectorial, 2.º léxico")));
    }
}
