package com.bigfito.elastiflix.web.about;

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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acerca de muestra las secciones del mockup, la tabla de precisión y los créditos de TMDB.
 */
@WebMvcTest(controllers = AboutController.class, properties = {
        "elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test"})
@Import(LayoutAdvice.class)
class AboutPageTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogStatusService catalog;

    @BeforeEach
    void catalogOfFourThousand() {
        when(catalog.status()).thenReturn(new CatalogStatus(
                "elastiflix-reloaded", "elastiflix-reloaded-20261018-000000", 4322L, true, null));
    }

    @Test
    @DisplayName("las secciones, la precisión y Créditos usan el alias y el conteo reales")
    void shouldShowSectionsPrecisionAndCredits() throws Exception {
        mvc.perform(get("/acerca"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Full-text (léxica)")))
                .andExpect(content().string(containsString("Vectorial")))
                .andExpect(content().string(containsString("Híbrida")))
                .andExpect(content().string(containsString("Resultados y filtros")))
                .andExpect(content().string(containsString("Carga de datos")))
                .andExpect(content().string(containsString("El catálogo")))
                .andExpect(content().string(containsString(
                        "Medido en el catálogo original de 4 322 películas")))
                .andExpect(content().string(containsString("ninguna película del catálogo lo es")))
                .andExpect(content().string(containsString("de 1902 a 2023")))
                .andExpect(content().string(containsString("22 de 32 (69 %)")))
                .andExpect(content().string(containsString("0.774")))
                .andExpect(content().string(containsString("0.853")))
                .andExpect(content().string(containsString("0.880")))
                .andExpect(content().string(containsString("Créditos")))
                .andExpect(content().string(containsString(CatalogTexts.TMDB)))
                .andExpect(content().string(containsString(CatalogTexts.TMDB_ES)))
                .andExpect(content().string(containsString("blue_short-")))
                .andExpect(content().string(containsString("elastiflix-reloaded")))
                .andExpect(content().string(containsString("Tiene 4 322 películas.")))
                .andExpect(content().string(containsString("se generan siempre")))
                .andExpect(content().string(containsString("Un CSV sin encabezado se rechaza")))
                .andExpect(content().string(containsString("El índice anterior se borra")))
                .andExpect(content().string(containsString("Similarity (BM25)")));
    }
}
