package com.bigfito.elastiflix.web.layout;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.web.error.NotFoundAdvice;
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
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La cabecera y el pie se repiten en cada página con el conteo del catálogo.
 */
@WebMvcTest(controllers = HomeController.class, properties = {
        "elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test"})
@Import({LayoutAdvice.class, NotFoundAdvice.class, SuggestionCatalog.class})
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
                .andExpect(content().string(containsString(CatalogTexts.TMDB)))
                .andExpect(content().string(containsString(
                        "<link rel=\"icon\" type=\"image/svg+xml\" href=\"/img/favicon.svg\">")));
    }

    @Test
    @DisplayName("el ícono declarado se sirve como recurso estático, sin pasar por el 404 que lee el catálogo")
    void shouldServeTheIconWithoutReadingTheCatalog() throws Exception {
        mvc.perform(get("/img/favicon.svg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/svg+xml"));

        verify(catalog, never()).status();
    }

    @Test
    @DisplayName("sin conexión, el pie dice que el conteo no está disponible")
    void shouldSayTheCountIsUnavailableWhenOffline() throws Exception {
        when(catalog.status()).thenReturn(new CatalogStatus("elastiflix-reloaded", null, null, false, "EFX-ES-001"));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Índice: elastiflix-reloaded · conteo no disponible")))
                .andExpect(content().string(not(containsString("sin películas"))));
    }
}
