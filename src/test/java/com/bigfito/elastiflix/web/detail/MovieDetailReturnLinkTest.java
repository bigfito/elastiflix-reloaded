package com.bigfito.elastiflix.web.detail;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.search.result.SearchService;
import com.bigfito.elastiflix.web.layout.LayoutAdvice;
import com.bigfito.elastiflix.web.layout.PosterUrls;
import com.bigfito.elastiflix.web.locale.LocaleConfig;
import com.bigfito.elastiflix.web.search.SearchForm;
import com.bigfito.elastiflix.web.search.SearchForms;
import com.bigfito.elastiflix.web.search.SearchLinks;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ida y vuelta: el enlace de la tarjeta abre el detalle y «Volver a los resultados» lleva a {@code /buscar} con
 * exactamente la misma búsqueda (PLAN › Requerimiento 1), en cualquier modo y con filtros de varios valores.
 */
@WebMvcTest(controllers = MovieDetailController.class, properties = {
        "elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test"})
@Import({LocaleConfig.class, LayoutAdvice.class, SearchQueryMapper.class, MovieDetailPage.class, PosterUrls.class})
@EnableConfigurationProperties(ElastiflixProperties.class)
class MovieDetailReturnLinkTest {

    private static final String ID = "603";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SearchService searches;

    @MockitoBean
    private CatalogStatusService catalog;

    @BeforeEach
    void facades() {
        when(catalog.status()).thenReturn(new CatalogStatus("elastiflix-reloaded", "idx", 6899L, true, null));
        when(searches.findById(anyString())).thenReturn(Optional.of(Movie.identified(ID)));
    }

    @Test
    @DisplayName("full-text con filtros de varios valores y fechas vuelve con todos los filtros")
    void shouldKeepMultiValueFiltersOnTheWayBack() throws Exception {
        SearchForm form = new SearchForm("matrix", "fulltext", "match", "original_title", "and", "plot_256",
                "100", "200", "3", "50", List.of("false", "true"), List.of("Drama", "Science Fiction"),
                List.of("en", "es"), List.of("PG-13", "R"), "1990-01-01", "2005-12-31");

        assertRoundTrip(form);
    }

    @Test
    @DisplayName("vectorial vuelve con vector, k y candidatos")
    void shouldKeepTheVectorSettingsOnTheWayBack() throws Exception {
        SearchForm form = new SearchForm("a dream inside a dream", "vectorial", "match", "title", "or", "plot_1024",
                "50", "400", "2", "25", List.of(), List.of("Action"), List.of(), List.of(), "", "");

        assertRoundTrip(form);
    }

    @Test
    @DisplayName("híbrida vuelve con tipo, operador, vector, k y candidatos")
    void shouldKeepTheHybridSettingsOnTheWayBack() throws Exception {
        SearchForm form = new SearchForm("heist thriller", "hibrida", "multi_match", "title", "and", "plot_512",
                "30", "300", "1", "100", List.of("false"), List.of(), List.of("en"), List.of(), "2000-01-01", "");

        assertRoundTrip(form);
    }

    @Test
    @DisplayName("la consulta «a+b & c#d ñ» llega intacta al detalle y de vuelta a /buscar")
    void shouldKeepAQueryWithReservedCharactersOnTheWayBack() throws Exception {
        SearchForm form = new SearchForm("a+b & c#d ñ", "fulltext", "match", "title", "or", "plot_256",
                "100", "200", "1", "25", List.of(), List.of(), List.of(), List.of(), "", "");

        String returnHref = assertRoundTrip(form);

        assertThat(returnHref).contains("q=a%2Bb%20%26%20c%23d%20%C3%B1");
    }

    // Abre el enlace de la tarjeta, lee «Volver a los resultados» y comprueba que reconstruye la misma búsqueda.
    private String assertRoundTrip(SearchForm original) throws Exception {
        // when: ida
        String detailHref = SearchLinks.detail(ID, original);
        MovieDetailView detail = (MovieDetailView) mvc.perform(get(URI.create(detailHref)))
                .andExpect(status().isOk())
                .andReturn().getModelAndView().getModel().get("detail");

        // then: vuelta
        UriComponents back = UriComponentsBuilder.fromUriString(detail.returnHref()).build(true);
        assertThat(back.getPath()).isEqualTo("/buscar");
        assertThat(back.getFragment()).isEqualTo("r-" + ID);
        assertThat(SearchForms.from(decoded(back.getQueryParams()))).isEqualTo(original);
        return detail.returnHref();
    }

    private static MultiValueMap<String, String> decoded(MultiValueMap<String, String> encoded) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        encoded.forEach((name, values) -> values.forEach(value ->
                params.add(name, UriUtils.decode(value, StandardCharsets.UTF_8))));
        return params;
    }
}
