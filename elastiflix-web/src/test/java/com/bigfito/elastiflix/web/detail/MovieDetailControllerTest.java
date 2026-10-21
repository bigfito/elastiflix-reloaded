package com.bigfito.elastiflix.web.detail;

import java.util.List;
import java.util.Optional;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.core.model.MovieCollection;
import com.bigfito.elastiflix.core.model.MovieKeyword;
import com.bigfito.elastiflix.search.result.SearchService;
import com.bigfito.elastiflix.web.layout.LayoutAdvice;
import com.bigfito.elastiflix.web.layout.PosterUrls;
import com.bigfito.elastiflix.web.locale.LocaleConfig;
import com.bigfito.elastiflix.web.locale.UiMessages;
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
@Import({LocaleConfig.class, LayoutAdvice.class, SearchQueryMapper.class, MovieDetailPage.class, PosterUrls.class})
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
                .andExpect(content().string(containsString("Tu búsqueda falló y la ficha de la película no se puede abrir.")));
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

    @Test
    @DisplayName("película existente con búsqueda previa responde 200 OK y renderiza la ficha técnica completa")
    void shouldRenderMovieDetailPageWhenMovieExists() throws Exception {
        Movie movie = sampleMovie();
        when(searches.findById("27205")).thenReturn(Optional.of(movie));

        mvc.perform(get("/pelicula/27205").param("q", "inception"))
                .andExpect(status().isOk())
                .andExpect(view().name("detail"))
                .andExpect(content().string(containsString("Inception")))
                .andExpect(content().string(containsString("Película · id 27205")))
                .andExpect(content().string(containsString("Volver a los resultados")))
                .andExpect(content().string(containsString("#r-27205")))
                .andExpect(content().string(containsString("160 000 000 USD")))
                .andExpect(content().string(containsString("825 532 764 USD")))
                .andExpect(content().string(containsString("Colección")))
                .andExpect(content().string(containsString("Inception Collection")))
                .andExpect(content().string(containsString("Leonardo DiCaprio")))
                .andExpect(content().string(containsString("paris, france")));
    }

    @Test
    @DisplayName("con lang=en responde 200 OK y renderiza la ficha técnica completa en inglés")
    void shouldRenderMovieDetailPageInEnglishWhenLangIsEn() throws Exception {
        Movie movie = sampleMovie();
        when(searches.findById("27205")).thenReturn(Optional.of(movie));

        mvc.perform(get("/pelicula/27205").param("q", "inception").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(view().name("detail"))
                .andExpect(content().string(containsString("<html lang=\"en\">")))
                .andExpect(content().string(containsString("Inception")))
                .andExpect(content().string(containsString("Movie · id 27205")))
                .andExpect(content().string(containsString("Original title:")))
                .andExpect(content().string(containsString("Back to results")))
                .andExpect(content().string(containsString("Technical details")))
                .andExpect(content().string(containsString("Production and languages")))
                .andExpect(content().string(containsString("Figures")))
                .andExpect(content().string(containsString("160,000,000 USD")))
                .andExpect(content().string(containsString("825,532,764 USD")))
                .andExpect(content().string(containsString("Collection")))
                .andExpect(content().string(containsString("Inception Collection")))
                .andExpect(content().string(containsString("Cast")))
                .andExpect(content().string(containsString("people, in catalog order.")))
                .andExpect(content().string(containsString("Keywords")))
                .andExpect(content().string(containsString("keywords, with their id.")))
                .andExpect(content().string(containsString("Back to the top")));
    }

    @Test
    @DisplayName("con lang=en acceso sin q devuelve HTTP 400 y mensaje en inglés")
    void shouldReturnBadRequestInEnglishWhenQueryIsMissing() throws Exception {
        mvc.perform(get("/pelicula/550").param("lang", "en"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString(ErrorCode.DIRECT_DETAIL_WITHOUT_QUERY.code())))
                .andExpect(content().string(containsString("Your search failed, so the movie details can&#39;t be opened.")));
    }

    private static Movie sampleMovie() {
        return new Movie(
                "27205", "tt1375666", "Inception", "Inception", "en",
                java.time.LocalDate.parse("2010-07-15"), 148, "Released", false, false,
                "Your mind is the scene of the crime.", "Cobb is a skilled thief", "Inception plot",
                List.of("Action", "Science Fiction"),
                List.of(new MovieKeyword(90L, "paris, france")),
                List.of("Leonardo DiCaprio", "Joseph Gordon-Levitt"),
                new MovieCollection(12345L, "Inception Collection", "/coll_back.jpg", "/coll_post.jpg"),
                160_000_000.0, 825_532_764.0, 82.5, 8.36, 35000, "PG-13",
                "https://inception.test", "/poster.jpg", "/backdrop.jpg",
                List.of("Warner Bros. Pictures"), List.of("United States"), List.of("en", "ja"),
                null, null, null, 8.4);
    }
}
