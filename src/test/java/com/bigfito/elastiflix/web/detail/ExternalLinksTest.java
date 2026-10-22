package com.bigfito.elastiflix.web.detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Solo se enlazan direcciones http o https; cualquier otro valor del catálogo queda vacío.
 */
class ExternalLinksTest {

    @ParameterizedTest(name = "«{0}» no se enlaza")
    @ValueSource(strings = {
        "javascript:alert(1)", "JavaScript:alert(1)", "  javascript:alert(1)", "java\tscript:alert(1)",
        "data:text/html,<script>alert(1)</script>", "vbscript:msgbox(1)", "ftp://example.com",
        "http:example.com", "//example.com", "inception.com", "   "})
    @DisplayName("un esquema que no es web, una dirección sin autoridad o un texto en blanco dan vacío")
    void shouldRejectNonWebAddresses(String url) {
        assertThat(ExternalLinks.webOrEmpty(url)).isEmpty();
    }

    @Test
    @DisplayName("null da vacío")
    void shouldRejectNull() {
        assertThat(ExternalLinks.webOrEmpty(null)).isEmpty();
    }

    @ParameterizedTest(name = "«{0}» se enlaza")
    @ValueSource(strings = {"https://inception.com", "http://www.example.com/path?a=1", "HTTPS://Example.com"})
    @DisplayName("http y https se enlazan tal cual")
    void shouldKeepWebAddresses(String url) {
        assertThat(ExternalLinks.webOrEmpty(url)).isEqualTo(url);
    }

    @Test
    @DisplayName("los espacios de los extremos se quitan")
    void shouldStripSurroundingSpaces() {
        assertThat(ExternalLinks.webOrEmpty("  https://inception.com ")).isEqualTo("https://inception.com");
    }

    @Test
    @DisplayName("el enlace de IMDb lleva el imdb_id como un segmento de la ruta del título")
    void shouldBuildTheImdbTitlePage() {
        assertThat(ExternalLinks.imdb("tt1375666")).isEqualTo("https://www.imdb.com/title/tt1375666/");
        assertThat(ExternalLinks.imdb("../a b")).isEqualTo("https://www.imdb.com/title/..%2Fa%20b/");
    }

    @Test
    @DisplayName("sin imdb_id no hay enlace a IMDb")
    void shouldNotLinkImdbWithoutAnId() {
        assertThat(ExternalLinks.imdb(null)).isEmpty();
        assertThat(ExternalLinks.imdb("  ")).isEmpty();
    }
}
