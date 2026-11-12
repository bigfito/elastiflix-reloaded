package com.bigfito.elastiflix.web.detail;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.core.model.MovieCollection;
import com.bigfito.elastiflix.core.model.MovieKeyword;
import com.bigfito.elastiflix.search.result.SearchService;
import com.bigfito.elastiflix.web.error.WebExceptionHandler;
import com.bigfito.elastiflix.web.layout.LayoutAdvice;
import com.bigfito.elastiflix.web.layout.PosterUrls;
import com.bigfito.elastiflix.web.locale.LocaleConfig;
import com.bigfito.elastiflix.web.locale.UiMessages;
import com.bigfito.elastiflix.web.search.SearchForms;
import com.bigfito.elastiflix.web.search.SearchLinks;
import com.bigfito.elastiflix.web.search.SearchQueryMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
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
@Import({LocaleConfig.class, LayoutAdvice.class, SearchQueryMapper.class, MovieDetailPage.class, PosterUrls.class,
        WebExceptionHandler.class})
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
    @DisplayName("si Elasticsearch falla, el detalle muestra la página de error EFX y no «no encontrada»")
    void shouldShowTheEfxErrorPageWhenElasticsearchFails() throws Exception {
        // given
        when(searches.findById("27205")).thenThrow(new ElasticsearchAccessException(
                ErrorCode.ELASTICSEARCH_REJECTED, new IllegalStateException("index_not_found_exception")));

        // when / then
        mvc.perform(get("/pelicula/27205").param("q", "inception"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("EFX-ES-007")))
                .andExpect(content().string(containsString("Elasticsearch rechazó la operación")))
                .andExpect(content().string(not(containsString("Esta página no existe"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a/b", "café"})
    @DisplayName("el enlace de la tarjeta abre el detalle de un id con barra o no ASCII")
    void shouldOpenTheDetailOfAnEncodedId(String id) throws Exception {
        // given
        when(searches.findById(id)).thenReturn(Optional.of(Movie.identified(id)));
        String href = SearchLinks.detail(id, SearchForms.from(new LinkedMultiValueMap<>(Map.of("q", List.of("x")))));

        // when / then
        mvc.perform(get(URI.create(href)))
                .andExpect(status().isOk())
                .andExpect(view().name("detail"));
        verify(searches).findById(id);
    }

    @Test
    @DisplayName("una película sin datos muestra «N/A» en la cabecera, el resumen, la sinopsis y las listas")
    void shouldRenderNotAvailableForAnEmptyMovie() throws Exception {
        // given
        when(searches.findById("999")).thenReturn(Optional.of(Movie.identified("999")));

        // when / then
        mvc.perform(get("/pelicula/999").param("q", "x"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<strong class=\"rating-score\">N/A</strong>")))
                .andExpect(content().string(containsString("<span class=\"rating-votes\">· N/A</span>")))
                .andExpect(content().string(not(containsString("N/A votos"))))
                .andExpect(content().string(containsString("Popularidad N/A")))
                .andExpect(content().string(containsString(
                        "<span class=\"genre-badge empty\" title=\"genres\">N/A</span>")))
                .andExpect(content().string(containsString("<p class=\"detail-overview-text empty\">N/A</p>")))
                .andExpect(content().string(containsString("<p class=\"detail-plot-text empty\">N/A</p>")))
                .andExpect(content().string(containsString("<p class=\"detail-empty\">N/A</p>")))
                .andExpect(content().string(not(containsString("0 personas"))))
                .andExpect(content().string(not(containsString("0 palabras clave"))))
                .andExpect(content().string(not(containsString("—"))));
    }

    @Test
    @DisplayName("con más de 18 personas, el reparto completo sale sin JavaScript y el botón espera a detail.js")
    void shouldRenderTheWholeCastWithoutJavascript() throws Exception {
        // given
        when(searches.findById("27205")).thenReturn(Optional.of(movieWithCast(25)));

        // when
        String html = mvc.perform(get("/pelicula/27205").param("q", "inception"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        assertThat(html).contains("25 personas, en el orden del catálogo.", "Persona 1<", "Persona 25<");
        assertThat(html).contains("Ver todo el reparto (25)", "data-show-less=\"Mostrar menos\"");
        assertThat(html.split("cast-extra", -1)).hasSize(8);
        assertThat(html).doesNotContain("display: none");
        assertThat(html).containsPattern("id=\"cast-toggle\" hidden\\s+aria-controls=\"cast-list\"");
    }

    @Test
    @DisplayName("en inglés, el botón del reparto dice «See the full cast (N)» y «Show less», y la ficha dice «Id»")
    void shouldLabelTheCastToggleInEnglish() throws Exception {
        // given
        when(searches.findById("27205")).thenReturn(Optional.of(movieWithCast(25)));

        // when / then
        mvc.perform(get("/pelicula/27205").param("q", "inception").param("lang", "en"))
                .andExpect(content().string(containsString("See the full cast (25)")))
                .andExpect(content().string(containsString("data-show-less=\"Show less\"")))
                .andExpect(content().string(containsString("<span>Id</span><code>id</code>")))
                .andExpect(content().string(not(containsString("Identifier"))));
    }

    @Test
    @DisplayName("la tagline y la consulta van entre «…» en español y entre “…” en inglés")
    void shouldQuoteTheTaglineAndTheQueryByLocale() throws Exception {
        // given
        when(searches.findById("27205")).thenReturn(Optional.of(sampleMovie()));

        // when / then
        mvc.perform(get("/pelicula/27205").param("q", "inception"))
                .andExpect(content().string(containsString("«Your mind is the scene of the crime.»")))
                .andExpect(content().string(containsString("«inception»")));
        mvc.perform(get("/pelicula/27205").param("q", "inception").param("lang", "en"))
                .andExpect(content().string(containsString("“Your mind is the scene of the crime.”")))
                .andExpect(content().string(containsString("“inception”")))
                .andExpect(content().string(not(containsString("«"))));
    }

    @Test
    @DisplayName("con imagen de fondo, el marcador va dentro y detail.js recibe la URL para precargarla")
    void shouldCarryTheBackdropMarkerAndItsUrl() throws Exception {
        // given
        when(searches.findById("27205")).thenReturn(Optional.of(sampleMovie()));

        // when
        String html = mvc.perform(get("/pelicula/27205").param("q", "inception"))
                .andReturn().getResponse().getContentAsString();

        // then
        assertThat(html).containsPattern("<div class=\"detail-backdrop\" style=\"background-image: url\\("
                + "&#39;https://[^\"]+/backdrop\\.jpg&#39;\\);\"\\s+data-backdrop=\"https://[^\"]+/backdrop\\.jpg\"");
        assertThat(html).contains("<span class=\"mono-tag\">backdrop_path</span>");
    }

    @Test
    @DisplayName("el total del reparto se agrupa con espacio en español y coma en inglés, nunca con punto")
    void shouldGroupTheCastCountByLocale() throws Exception {
        // given
        when(searches.findById("27205")).thenReturn(Optional.of(movieWithCast(1000)));

        // when / then
        mvc.perform(get("/pelicula/27205").param("q", "inception"))
                .andExpect(content().string(containsString("1 000 personas, en el orden del catálogo.")))
                .andExpect(content().string(containsString("Ver todo el reparto (1 000)")))
                .andExpect(content().string(not(containsString("1.000"))));
        mvc.perform(get("/pelicula/27205").param("q", "inception").param("lang", "en"))
                .andExpect(content().string(containsString("1,000 people, in catalog order.")))
                .andExpect(content().string(containsString("(1,000)")));
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
        return movieWithCast(List.of("Leonardo DiCaprio", "Joseph Gordon-Levitt"));
    }

    private static Movie movieWithCast(int people) {
        return movieWithCast(IntStream.rangeClosed(1, people).mapToObj(n -> "Persona " + n).toList());
    }

    private static Movie movieWithCast(List<String> cast) {
        return new Movie(
                "27205", "tt1375666", "Inception", "Inception", "en",
                java.time.LocalDate.parse("2010-07-15"), 148, "Released", false, false,
                "Your mind is the scene of the crime.", "Cobb is a skilled thief", "Inception plot",
                List.of("Action", "Science Fiction"),
                List.of(new MovieKeyword(90L, "paris, france")),
                cast,
                new MovieCollection(12345L, "Inception Collection", "/coll_back.jpg", "/coll_post.jpg"),
                160_000_000.0, 825_532_764.0, 82.5, 8.36, 35000, "PG-13",
                "https://inception.test", "/poster.jpg", "/backdrop.jpg",
                List.of("Warner Bros. Pictures"), List.of("United States"), List.of("en", "ja"),
                null, null, null, 8.4, null);
    }
}
