package com.bigfito.elastiflix.web.locale;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.web.error.NotFoundAdvice;
import com.bigfito.elastiflix.web.home.HomeController;
import com.bigfito.elastiflix.web.home.SuggestionCatalog;
import com.bigfito.elastiflix.web.layout.LayoutAdvice;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Resolución de idioma por cookie y parámetro {@code lang}, y enlaces {@code ES · EN} del header.
 */
@WebMvcTest(controllers = HomeController.class, properties = {
        "elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test"})
@Import({LayoutAdvice.class, NotFoundAdvice.class, SuggestionCatalog.class, LocaleConfig.class})
class LocaleSwitchTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogStatusService catalog;

    @Test
    @DisplayName("sin cookie, responde en español aunque el navegador envíe Accept-Language en inglés")
    void shouldDefaultToSpanishIgnoringAcceptLanguage() throws Exception {
        when(catalog.status()).thenReturn(new CatalogStatus(
                "elastiflix-reloaded", "elastiflix-reloaded-20261018-000000", 6899L, true, null));

        mvc.perform(get("/").header("Accept-Language", "en-US,en;q=0.9"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"es\">")))
                .andExpect(content().string(containsString("Conectado · Elasticsearch Serverless")))
                .andExpect(content().string(containsString("Índice: elastiflix-reloaded · 6 899 películas")))
                .andExpect(content().string(containsString(
                        "href=\"/?lang=es\" lang=\"es\" hreflang=\"es\" title=\"Español\"")))
                .andExpect(content().string(containsString("aria-current=\"true\"")));
    }

    @Test
    @DisplayName("con lang=en cambia a inglés, fija la cookie elastiflix-lang=en y formatea miles con coma")
    void shouldSwitchToEnglishAndSetCookie() throws Exception {
        when(catalog.status()).thenReturn(new CatalogStatus(
                "elastiflix-reloaded", "elastiflix-reloaded-20261018-000000", 6899L, true, null));

        mvc.perform(get("/").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(cookie().value("elastiflix-lang", "en"))
                .andExpect(content().string(containsString("<html lang=\"en\">")))
                .andExpect(content().string(containsString("Connected · Elasticsearch Serverless")))
                .andExpect(content().string(containsString("Index: elastiflix-reloaded · 6,899 movies")))
                .andExpect(content().string(containsString("<nav class=\"lang-switch\" aria-label=\"Language\">")));
    }

    @Test
    @DisplayName("un valor inválido de lang se ignora y conserva el idioma de la cookie actual")
    void shouldIgnoreUnsupportedLangParameterAndKeepCurrentCookieLocale() throws Exception {
        when(catalog.status()).thenReturn(new CatalogStatus(
                "elastiflix-reloaded", "elastiflix-reloaded-20261018-000000", 6899L, true, null));

        mvc.perform(get("/")
                        .cookie(new Cookie("elastiflix-lang", "en"))
                        .param("lang", "fr"))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("elastiflix-lang"))
                .andExpect(content().string(containsString("<html lang=\"en\">")))
                .andExpect(content().string(containsString("Connected · Elasticsearch Serverless")));
    }

    @Test
    @DisplayName("una cookie ilegible (elastiflix-lang=e$) se ignora: la página responde 200 en español")
    void shouldIgnoreAnUnreadableCookie() throws Exception {
        when(catalog.status()).thenReturn(new CatalogStatus(
                "elastiflix-reloaded", "elastiflix-reloaded-20261018-000000", 6899L, true, null));

        mvc.perform(get("/").cookie(new Cookie("elastiflix-lang", "e$")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"es\">")))
                .andExpect(content().string(containsString("Conectado · Elasticsearch Serverless")));
    }

    @Test
    @DisplayName("la cookie del idioma es HttpOnly, SameSite=Lax, de todo el sitio y dura un año")
    void shouldSetTheCookieAttributes() throws Exception {
        when(catalog.status()).thenReturn(new CatalogStatus(
                "elastiflix-reloaded", "elastiflix-reloaded-20261018-000000", 6899L, true, null));

        mvc.perform(get("/").param("lang", "en"))
                .andExpect(cookie().value("elastiflix-lang", "en"))
                .andExpect(cookie().httpOnly("elastiflix-lang", true))
                .andExpect(cookie().sameSite("elastiflix-lang", "Lax"))
                .andExpect(cookie().path("elastiflix-lang", "/"))
                .andExpect(cookie().maxAge("elastiflix-lang", (int) Duration.ofDays(365).toSeconds()));
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
    }
}
