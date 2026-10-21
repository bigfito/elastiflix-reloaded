package com.bigfito.elastiflix.web.locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enlaces {@code ES · EN} del selector: misma ruta, mismos parámetros y solo {@code lang} cambia.
 */
class LanguageSwitchLinksTest {

    @Test
    @DisplayName("conserva la ruta y todos los parámetros de consulta, también los repetidos")
    void shouldPreserveAllQueryParametersInLanguageLinks() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/buscar");
        request.setQueryString("q=titanic&modo=hibrida&genero=Drama&genero=Romance&lang=es");
        request.setParameter("q", "titanic");
        request.setParameter("modo", "hibrida");
        request.setParameter("genero", "Drama", "Romance");
        request.setParameter("lang", "es");

        // then
        assertThat(LanguageSwitchLinks.hrefFor(request, "en"))
                .isEqualTo("/buscar?q=titanic&modo=hibrida&genero=Drama&genero=Romance&lang=en");
        assertThat(LanguageSwitchLinks.hrefFor(request, "es"))
                .isEqualTo("/buscar?q=titanic&modo=hibrida&genero=Drama&genero=Romance&lang=es");
    }

    @Test
    @DisplayName("un id no ASCII ya codificado en la ruta no se codifica dos veces")
    void shouldNotEncodeANonAsciiIdTwice() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/pelicula/caf%C3%A9");
        request.setQueryString("q=caf%C3%A9");
        request.setParameter("q", "café");

        // when
        String href = LanguageSwitchLinks.hrefFor(request, "en");

        // then
        assertThat(href).isEqualTo("/pelicula/caf%C3%A9?q=caf%C3%A9&lang=en");
    }

    @Test
    @DisplayName("un id con barra codificada (a%2Fb) sigue siendo un solo segmento")
    void shouldKeepAnEncodedSlashInsideTheSegment() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/pelicula/a%2Fb");
        request.setQueryString("q=x");
        request.setParameter("q", "x");

        // when
        String href = LanguageSwitchLinks.hrefFor(request, "es");

        // then
        assertThat(href).isEqualTo("/pelicula/a%2Fb?q=x&lang=es");
    }

    @Test
    @DisplayName("un segmento que llega sin codificar o con caracteres reservados sale codificado una vez")
    void shouldEncodeRawAndReservedCharactersOnce() {
        // given
        MockHttpServletRequest raw = new MockHttpServletRequest("GET", "/pelicula/café");
        MockHttpServletRequest reserved = new MockHttpServletRequest("GET", "/pelicula/a%3Bb%20c");

        // then
        assertThat(LanguageSwitchLinks.hrefFor(raw, "en")).isEqualTo("/pelicula/caf%C3%A9?lang=en");
        assertThat(LanguageSwitchLinks.hrefFor(reserved, "en")).isEqualTo("/pelicula/a%3Bb%20c?lang=en");
    }

    @Test
    @DisplayName("una ruta con un escape mal formado o con barras repetidas no sale del sitio")
    void shouldFallBackToHomeForAMalformedPath() {
        // given
        MockHttpServletRequest malformed = new MockHttpServletRequest("GET", "/pelicula/%zz");
        MockHttpServletRequest doubled = new MockHttpServletRequest("GET", "//otro.sitio/x");

        // then
        assertThat(LanguageSwitchLinks.hrefFor(malformed, "en")).isEqualTo("/?lang=en");
        assertThat(LanguageSwitchLinks.hrefFor(doubled, "en")).isEqualTo("/otro.sitio/x?lang=en");
    }
}
