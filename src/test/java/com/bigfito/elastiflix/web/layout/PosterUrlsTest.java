package com.bigfito.elastiflix.web.layout;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PosterUrls} solo compone la URL con rutas que tienen la forma de una ruta de TMDB.
 */
class PosterUrlsTest {

    private final PosterUrls posters = new PosterUrls(new ElastiflixProperties(
            null, null, null, null, new ElastiflixProperties.Poster("https://image.tmdb.org/t/p/w342"), null, null));

    @ParameterizedTest
    @ValueSource(strings = {"/8IB2e4r4oVhHnANbnm7O3Tj6tF8.jpg", "/s3TBrRGB1iav7gFOCNx3H31MoES.jpg", "/a_b-c.png"})
    @DisplayName("une el CDN y una ruta de TMDB válida")
    void shouldBuildTheUrlWhenThePathIsValid(String path) {
        assertThat(posters.url(path)).isEqualTo("https://image.tmdb.org/t/p/w342" + path);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "   ",
        "/x.jpg') ; background: url('https://evil.test/a.png",
        "/x.jpg)",
        "/a b.jpg",
        "/a/b.jpg",
        "//evil.test/x.jpg",
        "x.jpg",
        "javascript:alert(1)",
        "/x.jpg\n",
        "/póster.jpg"
    })
    @DisplayName("trata como ausente una ruta vacía o que no tiene la forma de TMDB")
    void shouldReturnEmptyWhenThePathIsMissingOrInvalid(String path) {
        assertThat(posters.url(path)).isEmpty();
    }

    @Test
    @DisplayName("una ruta que podría cerrar el url('…') del estilo no llega a la URL")
    void shouldNotLetAPathCloseTheCssUrl() {
        // when
        String url = posters.url("/x.jpg');color:red;('");

        // then
        assertThat(url).isEmpty();
    }
}
