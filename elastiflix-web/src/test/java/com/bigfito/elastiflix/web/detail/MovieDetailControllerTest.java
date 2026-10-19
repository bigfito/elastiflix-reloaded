package com.bigfito.elastiflix.web.detail;

import java.util.Optional;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.search.result.SearchService;
import com.bigfito.elastiflix.web.layout.LayoutAdvice;
import com.bigfito.elastiflix.web.layout.PosterUrls;
import com.bigfito.elastiflix.web.search.SearchQueryMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Pruebas del controlador {@link MovieDetailController}: validación de q previa, 404 y 400.
 */
@WebMvcTest(controllers = MovieDetailController.class, properties = {
        "elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test"})
@Import({LayoutAdvice.class, SearchQueryMapper.class, MovieDetailPage.class, PosterUrls.class})
@EnableConfigurationProperties(ElastiflixProperties.class)
class MovieDetailControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SearchService searches;

    @MockitoBean
    private CatalogStatusService catalog;

    @BeforeEach
    void setupCatalog() {
        when(catalog.status()).thenReturn(new CatalogStatus("elastiflix-reloaded", "idx", 6899L, true, null));
    }

    @Test
    @DisplayName("acceso sin parámetro q devuelve HTTP 400 y error controlado EFX-SRCH-012")
    void shouldReturnBadRequestWhenQueryIsMissing() throws Exception {
        mvc.perform(get("/pelicula/550"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString(ErrorCode.DIRECT_DETAIL_WITHOUT_QUERY.code())))
                .andExpect(content().string(containsString("La consulta de búsqueda no es válida o está ausente")));
    }

    @Test
    @DisplayName("acceso con parámetro q en blanco devuelve HTTP 400 y error controlado EFX-SRCH-012")
    void shouldReturnBadRequestWhenQueryIsBlank() throws Exception {
        mvc.perform(get("/pelicula/550").param("q", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString(ErrorCode.DIRECT_DETAIL_WITHOUT_QUERY.code())));
    }

    @Test
    @DisplayName("acceso con parámetros que la búsqueda rechazaría devuelve HTTP 400 y error controlado")
    void shouldReturnBadRequestWhenSearchParamsAreInvalid() throws Exception {
        mvc.perform(get("/pelicula/550")
                        .param("q", "matrix")
                        .param("desde", "2025-01-01")
                        .param("hasta", "2010-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString(ErrorCode.DIRECT_DETAIL_WITHOUT_QUERY.code())));
    }

    @Test
    @DisplayName("película inexistente con búsqueda válida devuelve HTTP 404")
    void shouldReturnNotFoundWhenMovieDoesNotExist() throws Exception {
        when(searches.findById("999999")).thenReturn(Optional.empty());

        mvc.perform(get("/pelicula/999999").param("q", "matrix"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("not-found"))
                .andExpect(content().string(containsString("Esta página no existe")));
    }
}
