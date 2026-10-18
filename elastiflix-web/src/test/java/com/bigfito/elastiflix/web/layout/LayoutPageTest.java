package com.bigfito.elastiflix.web.layout;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.web.home.HomeController;
import com.bigfito.elastiflix.web.home.SuggestionCatalog;

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
 * La cabecera y el pie se repiten en cada página con el conteo del catálogo.
 */
@WebMvcTest(controllers = HomeController.class, properties = {
        "elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test"})
@Import({LayoutAdvice.class, SuggestionCatalog.class})
class LayoutPageTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogStatusService catalog;

    @Test
    @DisplayName("el header y el pie muestran la conexión, el índice y la atribución de TMDB")
    void shouldShowConnectionIndexAndTmdbOnTheHomePage() throws Exception {
        when(catalog.status()).thenReturn(new CatalogStatus(
                "elastiflix-reloaded", "elastiflix-reloaded-20261018-000000", 4322L, true, null));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Conectado · Elasticsearch Serverless")))
                .andExpect(content().string(containsString("Índice: elastiflix-reloaded · 4 322 películas")))
                .andExpect(content().string(containsString(CatalogTexts.TMDB)));
    }
}
