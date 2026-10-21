package com.bigfito.elastiflix.web.locale;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.search.result.SearchService;
import com.bigfito.elastiflix.web.detail.MovieDetailController;
import com.bigfito.elastiflix.web.detail.MovieDetailPage;
import com.bigfito.elastiflix.web.error.ErrorProbeController;
import com.bigfito.elastiflix.web.error.NotFoundAdvice;
import com.bigfito.elastiflix.web.error.WebExceptionHandler;
import com.bigfito.elastiflix.web.home.HomeController;
import com.bigfito.elastiflix.web.home.SuggestionCatalog;
import com.bigfito.elastiflix.web.layout.LayoutAdvice;
import com.bigfito.elastiflix.web.search.SearchQueryMapper;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Resolución de idioma por cookie y parámetro {@code lang}, y enlaces {@code ES · EN} del header en Inicio, el
 * detalle, la página de error y el 404.
 */
@WebMvcTest(controllers = {HomeController.class, MovieDetailController.class, ErrorProbeController.class},
        properties = {"elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test"})
@Import({LayoutAdvice.class, NotFoundAdvice.class, WebExceptionHandler.class, SuggestionCatalog.class,
        SearchQueryMapper.class})
class LocaleSwitchTest {

    private static final String COOKIE = "elastiflix-lang";
    private static final String SPANISH_PAGE = "<html lang=\"es\">";
    private static final String ENGLISH_PAGE = "<html lang=\"en\">";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogStatusService catalog;

    @MockitoBean
    private SearchService searches;

    @MockitoBean
    private MovieDetailPage detailPage;

    @BeforeEach
    void connectedCatalog() {
        when(catalog.status()).thenReturn(new CatalogStatus(
                "elastiflix-reloaded", "elastiflix-reloaded-20261018-000000", 6899L, true, null));
        when(searches.findById(anyString())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("sin cookie, responde en español aunque el navegador envíe Accept-Language en inglés")
    void shouldDefaultToSpanishIgnoringAcceptLanguage() throws Exception {
        mvc.perform(get("/").header("Accept-Language", "en-US,en;q=0.9"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(SPANISH_PAGE)))
                .andExpect(content().string(containsString("Conectado · Elasticsearch Serverless")))
                .andExpect(content().string(containsString("Índice: elastiflix-reloaded · 6 899 películas")))
                .andExpect(content().string(containsString(
                        "href=\"/?lang=es\" lang=\"es\" hreflang=\"es\" title=\"Español\"")))
                .andExpect(content().string(containsString("aria-current=\"true\"")));
    }

    @Test
    @DisplayName("con lang=en cambia a inglés, fija la cookie elastiflix-lang=en y formatea miles con coma")
    void shouldSwitchToEnglishAndSetCookie() throws Exception {
        mvc.perform(get("/").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(cookie().value(COOKIE, "en"))
                .andExpect(content().string(containsString(ENGLISH_PAGE)))
                .andExpect(content().string(containsString("Connected · Elasticsearch Serverless")))
                .andExpect(content().string(containsString("Index: elastiflix-reloaded · 6,899 movies")))
                .andExpect(content().string(containsString("<nav class=\"lang-switch\" aria-label=\"Language\">")));
    }

    @Test
    @DisplayName("un valor inválido de lang se ignora y conserva el idioma de la cookie actual")
    void shouldIgnoreUnsupportedLangParameterAndKeepCurrentCookieLocale() throws Exception {
        mvc.perform(get("/")
                        .cookie(new Cookie(COOKIE, "en"))
                        .param("lang", "fr"))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist(COOKIE))
                .andExpect(content().string(containsString(ENGLISH_PAGE)))
                .andExpect(content().string(containsString("Connected · Elasticsearch Serverless")));
    }

    @Test
    @DisplayName("lang=EN (en mayúsculas) no es un valor admitido: se ignora y no fija la cookie")
    void shouldIgnoreAnUppercaseLang() throws Exception {
        mvc.perform(get("/").queryParam("lang", "EN"))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist(COOKIE))
                .andExpect(content().string(containsString(SPANISH_PAGE)))
                .andExpect(content().string(containsString("href=\"/?lang=en\"")));
    }

    @Test
    @DisplayName("lang vacío se ignora: conserva el idioma de la cookie y no la vuelve a fijar")
    void shouldIgnoreAnEmptyLang() throws Exception {
        mvc.perform(get("/").cookie(new Cookie(COOKIE, "en")).queryParam("lang", ""))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist(COOKIE))
                .andExpect(content().string(containsString(ENGLISH_PAGE)));
    }

    @Test
    @DisplayName("una cookie ilegible (elastiflix-lang=e$) se ignora: la página responde 200 en español")
    void shouldIgnoreAnUnreadableCookie() throws Exception {
        mvc.perform(get("/").cookie(new Cookie(COOKIE, "e$")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(SPANISH_PAGE)))
                .andExpect(content().string(containsString("Conectado · Elasticsearch Serverless")));
    }

    @Test
    @DisplayName("la cookie del idioma es HttpOnly, SameSite=Lax, de todo el sitio y dura un año")
    void shouldSetTheCookieAttributes() throws Exception {
        mvc.perform(get("/").param("lang", "en"))
                .andExpect(cookie().value(COOKIE, "en"))
                .andExpect(cookie().httpOnly(COOKIE, true))
                .andExpect(cookie().sameSite(COOKIE, "Lax"))
                .andExpect(cookie().path(COOKIE, "/"))
                .andExpect(cookie().maxAge(COOKIE, (int) Duration.ofDays(365).toSeconds()));
    }

    @Test
    @DisplayName("los valores no ASCII y los caracteres especiales de la URL pasan al selector codificados una vez")
    void shouldKeepNonAsciiValuesInTheLanguageLinks() throws Exception {
        mvc.perform(get(URI.create("/?q=a%2Bb+%26+c%23d+%C3%B1&genero=Acci%C3%B3n")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "href=\"/?q=a%2Bb%20%26%20c%23d%20%C3%B1&amp;genero=Acci%C3%B3n&amp;lang=en\"")));
    }

    @Test
    @DisplayName("en /pelicula/{id} con un id no ASCII, el selector conserva la ruta y la búsqueda sin recodificar")
    void shouldLinkTheDetailOfANonAsciiId() throws Exception {
        mvc.perform(get(URI.create("/pelicula/caf%C3%A9?q=caf%C3%A9&lang=en")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(ENGLISH_PAGE)))
                .andExpect(content().string(containsString("href=\"/pelicula/caf%C3%A9?q=caf%C3%A9&amp;lang=es\"")))
                .andExpect(content().string(not(containsString("%25C3"))));
    }

    @Test
    @DisplayName("en /pelicula/a%2Fb, el selector deja la barra codificada dentro del id")
    void shouldLinkTheDetailOfAnIdWithASlash() throws Exception {
        mvc.perform(get(URI.create("/pelicula/a%2Fb?q=x")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("href=\"/pelicula/a%2Fb?q=x&amp;lang=en\"")));
    }

    @Test
    @DisplayName("la página de error EFX y el 404 llevan el selector con su ruta y sus parámetros")
    void shouldLinkTheErrorAndNotFoundPages() throws Exception {
        mvc.perform(get(URI.create("/probe/elastiflix?x=%C3%B1&lang=en")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString(ENGLISH_PAGE)))
                .andExpect(content().string(containsString("href=\"/probe/elastiflix?x=%C3%B1&amp;lang=es\"")));

        mvc.perform(get(URI.create("/no-existe?x=%C3%B1&lang=en")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("This page does not exist")))
                .andExpect(content().string(containsString("href=\"/no-existe?x=%C3%B1&amp;lang=es\"")));
    }

    @Test
    @DisplayName("SupportedLocale normaliza cualquier locale no soportado a español")
    void shouldNormalizeUnsupportedLocalesToSpanish() {
        assertThat(SupportedLocale.normalize(null)).isEqualTo(SupportedLocale.ES);
        assertThat(SupportedLocale.normalize(Locale.FRENCH)).isEqualTo(SupportedLocale.ES);
        assertThat(SupportedLocale.normalize(Locale.US)).isEqualTo(SupportedLocale.EN);
        assertThat(SupportedLocale.fromParam("en")).contains(SupportedLocale.EN);
        assertThat(SupportedLocale.fromParam("es")).contains(SupportedLocale.ES);
        assertThat(SupportedLocale.fromParam("pt")).isEmpty();
        assertThat(SupportedLocale.fromParam("EN")).isEmpty();
        assertThat(SupportedLocale.fromParam("")).isEmpty();
    }
}
