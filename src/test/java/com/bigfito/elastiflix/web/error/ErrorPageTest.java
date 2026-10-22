package com.bigfito.elastiflix.web.error;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.web.layout.CatalogTexts;
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
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Una excepción de la demo muestra su código y una ruta desconocida ofrece la navegación.
 */
@WebMvcTest(controllers = ErrorProbeController.class, properties = {
        "elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test"})
@Import({WebExceptionHandler.class, NotFoundAdvice.class, LayoutAdvice.class})
class ErrorPageTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogStatusService catalog;

    @BeforeEach
    void connectedCatalog() {
        when(catalog.status()).thenReturn(new CatalogStatus(
                "elastiflix-reloaded", "elastiflix-reloaded-20261018-000000", 4322L, true, null));
    }

    @Test
    @DisplayName("ElastiflixException muestra el código, el mensaje del catálogo y el pie con TMDB")
    void shouldShowTheCatalogMessage() throws Exception {
        mvc.perform(get("/probe/elastiflix"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("EFX-ES-001")))
                .andExpect(content().string(containsString("<p>No se pudo conectar con Elasticsearch.</p>")))
                .andExpect(content().string(not(containsString("EFX-ES-001 ·"))))
                .andExpect(content().string(containsString("<footer class=\"site-footer\">")))
                .andExpect(content().string(containsString("Índice: elastiflix-reloaded · 4 322 películas")))
                .andExpect(content().string(containsString(CatalogTexts.TMDB)));
    }

    @Test
    @DisplayName("una ElastiflixException lee el estado del catálogo una sola vez")
    void shouldReadTheCatalogOnceForAnElastiflixError() throws Exception {
        // when
        mvc.perform(get("/probe/elastiflix"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Índice: elastiflix-reloaded · 4 322 películas")));

        // then
        verify(catalog, times(1)).status();
    }

    @Test
    @DisplayName("una ruta inexistente muestra el 404 y los enlaces de la demo")
    void shouldShowNavigationForAnUnknownRoute() throws Exception {
        mvc.perform(get("/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Esta página no existe")))
                .andExpect(content().string(containsString("href=\"/\">Buscar")))
                .andExpect(content().string(not(containsString("href=\"/buscar\""))))
                .andExpect(content().string(containsString("href=\"/cargar\"")))
                .andExpect(content().string(containsString("href=\"/acerca\"")));
    }

    @Test
    @DisplayName("con lang=en muestra el error EFX y el 404 en inglés")
    void shouldShowEnglishErrorAndNotFoundWhenLangIsEn() throws Exception {
        mvc.perform(get("/probe/elastiflix").param("lang", "en"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("EFX-ES-001")))
                .andExpect(content().string(containsString("<p>Couldn&#39;t connect to Elasticsearch.</p>")))
                .andExpect(content().string(containsString("Could not complete")))
                .andExpect(content().string(containsString("Back to search")))
                .andExpect(content().string(containsString("Index: elastiflix-reloaded · 4,322 movies")));

        mvc.perform(get("/no-existe").param("lang", "en"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("This page does not exist")))
                .andExpect(content().string(containsString("The address does not match any screen in the demo.")))
                .andExpect(content().string(containsString("href=\"/\">Search")));
    }
}
