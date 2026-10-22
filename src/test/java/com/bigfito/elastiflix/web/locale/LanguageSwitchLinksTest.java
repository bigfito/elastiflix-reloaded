package com.bigfito.elastiflix.web.locale;

import jakarta.servlet.RequestDispatcher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enlaces {@code ES · EN} del selector: misma ruta, mismos parámetros de la URL y solo {@code lang} cambia.
 */
class LanguageSwitchLinksTest {

    private static final String EN = "en";
    private static final String ES = "es";

    @Test
    @DisplayName("conserva la ruta y todos los parámetros de la URL, también los repetidos")
    void shouldPreserveAllQueryParametersInLanguageLinks() {
        // given
        MockHttpServletRequest request = get("/buscar", "q=titanic&modo=hibrida&genero=Drama&genero=Romance&lang=es");

        // then
        assertThat(LanguageSwitchLinks.hrefFor(request, EN))
                .isEqualTo("/buscar?q=titanic&modo=hibrida&genero=Drama&genero=Romance&lang=en");
        assertThat(LanguageSwitchLinks.hrefFor(request, ES))
                .isEqualTo("/buscar?q=titanic&modo=hibrida&genero=Drama&genero=Romance&lang=es");
    }

    @Test
    @DisplayName("un id no ASCII ya codificado en la ruta no se codifica dos veces")
    void shouldNotEncodeANonAsciiIdTwice() {
        // given
        MockHttpServletRequest request = get("/pelicula/caf%C3%A9", "q=caf%C3%A9");

        // when
        String href = LanguageSwitchLinks.hrefFor(request, EN);

        // then
        assertThat(href).isEqualTo("/pelicula/caf%C3%A9?q=caf%C3%A9&lang=en");
    }

    @Test
    @DisplayName("un id con barra codificada (a%2Fb) sigue siendo un solo segmento")
    void shouldKeepAnEncodedSlashInsideTheSegment() {
        // given
        MockHttpServletRequest request = get("/pelicula/a%2Fb", "q=x");

        // when
        String href = LanguageSwitchLinks.hrefFor(request, ES);

        // then
        assertThat(href).isEqualTo("/pelicula/a%2Fb?q=x&lang=es");
    }

    @Test
    @DisplayName("un segmento que llega sin codificar o con caracteres reservados sale codificado una vez")
    void shouldEncodeRawAndReservedCharactersOnce() {
        // given
        MockHttpServletRequest raw = get("/pelicula/café", null);
        MockHttpServletRequest reserved = get("/pelicula/a%3Bb%20c", null);

        // then
        assertThat(LanguageSwitchLinks.hrefFor(raw, EN)).isEqualTo("/pelicula/caf%C3%A9?lang=en");
        assertThat(LanguageSwitchLinks.hrefFor(reserved, EN)).isEqualTo("/pelicula/a%3Bb%20c?lang=en");
    }

    @Test
    @DisplayName("una ruta con un escape mal formado o con barras repetidas no sale del sitio")
    void shouldFallBackToHomeForAMalformedPath() {
        // given
        MockHttpServletRequest malformed = get("/pelicula/%zz", null);
        MockHttpServletRequest doubled = get("//otro.sitio/x", null);

        // then
        assertThat(LanguageSwitchLinks.hrefFor(malformed, EN)).isEqualTo("/?lang=en");
        assertThat(LanguageSwitchLinks.hrefFor(doubled, EN)).isEqualTo("/otro.sitio/x?lang=en");
    }

    @Test
    @DisplayName("los valores con espacios, +, &, # y ñ salen decodificados y codificados una sola vez")
    void shouldReencodeSpecialCharactersOfTheQuery() {
        // given
        MockHttpServletRequest request = get("/buscar", "q=a%2Bb+%26+c%23d+%C3%B1&lang=en");

        // when
        String href = LanguageSwitchLinks.hrefFor(request, ES);

        // then
        assertThat(href).isEqualTo("/buscar?q=a%2Bb%20%26%20c%23d%20%C3%B1&lang=es");
    }

    @Test
    @DisplayName("solo copia los parámetros de la URL, no los del cuerpo de un formulario")
    void shouldIgnoreParametersThatDidNotComeInTheUrl() {
        // given
        MockHttpServletRequest request = get("/cargar", "job=7b0c");
        request.setParameter("job", "7b0c");
        request.setParameter("plot", "texto del cuerpo");

        // when
        String href = LanguageSwitchLinks.hrefFor(request, EN);

        // then
        assertThat(href).isEqualTo("/cargar?job=7b0c&lang=en");
    }

    @Test
    @DisplayName("un parámetro con un escape mal formado se omite, como lo omite el contenedor")
    void shouldSkipAMalformedParameter() {
        // given
        MockHttpServletRequest request = get("/buscar", "q=%zz&modo=fulltext&=sin-nombre");

        // when
        String href = LanguageSwitchLinks.hrefFor(request, EN);

        // then
        assertThat(href).isEqualTo("/buscar?modo=fulltext&lang=en");
    }

    @Test
    @DisplayName("la respuesta de un POST en /cargar/... enlaza a /cargar sin los campos del formulario")
    void shouldLinkToTheIngestPageAfterAPost() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/cargar/registro");
        request.setParameter("id", "42");
        request.setParameter("plot", "texto del formulario");

        // when
        String href = LanguageSwitchLinks.hrefFor(request, EN);

        // then
        assertThat(href).isEqualTo("/cargar?lang=en");
    }

    @Test
    @DisplayName("la respuesta de otro método que no es GET enlaza a Inicio")
    void shouldLinkToHomeAfterAnotherMethod() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/acerca");
        request.setQueryString("x=1");

        // when
        String href = LanguageSwitchLinks.hrefFor(request, ES);

        // then
        assertThat(href).isEqualTo("/?lang=es");
    }

    @Test
    @DisplayName("en el reenvío /error usa la ruta, el método y la URL de la página original")
    void shouldUseTheOriginalRequestOnAnErrorDispatch() {
        // given
        MockHttpServletRequest get = errorDispatch("GET", "/buscar", "q=a+b&lang=en");
        MockHttpServletRequest post = errorDispatch("POST", "/cargar/registro", null);

        // then
        assertThat(LanguageSwitchLinks.hrefFor(get, ES)).isEqualTo("/buscar?q=a%20b&lang=es");
        assertThat(LanguageSwitchLinks.hrefFor(post, ES)).isEqualTo("/cargar?lang=es");
    }

    @Test
    @DisplayName("sin petición, o en /error sin la página original, enlaza a Inicio")
    void shouldLinkToHomeWithoutAnOriginalPage() {
        // given
        MockHttpServletRequest error = get("/error", null);

        // then
        assertThat(LanguageSwitchLinks.hrefFor(null, EN)).isEqualTo("/?lang=en");
        assertThat(LanguageSwitchLinks.hrefFor(error, EN)).isEqualTo("/?lang=en");
    }

    private static MockHttpServletRequest get(String uri, String query) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setQueryString(query);
        return request;
    }

    private static MockHttpServletRequest errorDispatch(String method, String uri, String query) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/error");
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, uri);
        request.setAttribute(RequestDispatcher.ERROR_METHOD, method);
        if (query != null) {
            request.setAttribute(RequestDispatcher.ERROR_QUERY_STRING, query);
        }
        return request;
    }
}
