package com.bigfito.elastiflix.web;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.service.IngestService;
import com.bigfito.elastiflix.search.facet.Facets;
import com.bigfito.elastiflix.search.page.Pagination;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.result.SearchHit;
import com.bigfito.elastiflix.search.result.SearchResult;
import com.bigfito.elastiflix.search.result.SearchService;
import com.bigfito.elastiflix.web.layout.CatalogTexts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Las páginas, servidas por el Tomcat de verdad, llevan el pie con TMDB. El {@code /error} real titula un 400
 * y un 405 sin código {@code EFX}. Las fachadas son dobles: no hace falta Elasticsearch.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "elastiflix.elasticsearch.url=http://localhost:9200", "elastiflix.elasticsearch.api-key=test"})
class ServedPagesTest {

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @MockitoBean
    private CatalogStatusService catalog;

    @MockitoBean
    private SearchService searches;

    @MockitoBean
    private IngestService ingest;

    @BeforeEach
    void facades() {
        when(catalog.status()).thenReturn(new CatalogStatus("elastiflix-reloaded", "idx", 6899L, true, null));
        SearchHit hit = new SearchHit("1", "Titanic", "Titanic", "Un barco", "", LocalDate.of(1997, 12, 19), 194,
                List.of("Drama"), 7.9, 1000, "PG-13", 12.5, null, null, null);
        when(searches.search(any(SearchQuery.class))).thenReturn(new SearchResult(List.of(hit), 1,
                new Facets(List.of(), List.of(), List.of(), List.of(), null, null),
                new Pagination(1, 1, 1, 25, 1, 1), List.of("match · title"), null));
        when(searches.findById("1")).thenReturn(Optional.of(movie("1", "Titanic")));
        when(searches.findById("café")).thenReturn(Optional.of(movie("café", "Café de la Paix")));
        when(searches.findById("a/b")).thenReturn(Optional.of(movie("a/b", "Barra en el id")));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/buscar?q=titanic", "/pelicula/1?q=titanic", "/cargar", "/acerca", "/no-existe"})
    @DisplayName("Resultados, Detalle, Cargar, Acerca y el 404 llevan la atribución de TMDB en el pie")
    void shouldShowTheTmdbNoticeOnEveryPage(String path) throws Exception {
        HttpResponse<String> page = get(path);

        assertThat(page.body()).contains("<footer").contains(CatalogTexts.TMDB)
                .contains("Índice: elastiflix-reloaded · 6 899 películas");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/?lang=en", "/buscar?q=titanic&lang=en", "/pelicula/1?q=titanic&lang=en",
        "/cargar?lang=en", "/acerca?lang=en", "/no-existe?lang=en"})
    @DisplayName("con lang=en, cada página sale en inglés, con la cookie y con el pie en inglés")
    void shouldServeEveryPageInEnglish(String path) throws Exception {
        HttpResponse<String> page = get(path);

        assertThat(page.headers().firstValue("Set-Cookie")).hasValueSatisfying(cookie ->
                assertThat(cookie).startsWith("elastiflix-lang=en").contains("HttpOnly"));
        assertThat(page.body()).contains("<html lang=\"en\">").contains(CatalogTexts.TMDB)
                .contains("Index: elastiflix-reloaded · 6,899 movies");
    }

    @Test
    @DisplayName("el detalle de un id no ASCII responde en inglés y el selector conserva la ruta y la búsqueda")
    void shouldServeTheDetailOfANonAsciiIdInEnglish() throws Exception {
        HttpResponse<String> page = get("/pelicula/caf%C3%A9?q=caf%C3%A9&lang=en");

        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("<html lang=\"en\">").contains("Café de la Paix")
                .contains("href=\"/pelicula/caf%C3%A9?q=caf%C3%A9&amp;lang=es\"");
    }

    @Test
    @DisplayName("un id con barra se abre como un solo segmento codificado, y el selector conserva la barra")
    void shouldServeTheDetailOfAnIdWithASlash() throws Exception {
        HttpResponse<String> page = get("/pelicula/a%2Fb?q=titanic");

        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("Barra en el id")
                .contains("href=\"/pelicula/a%2Fb?q=titanic&amp;lang=en\"");
    }

    @Test
    @DisplayName("en /cargar?job=, el selector conserva el trabajo para reanudarlo en el otro idioma")
    void shouldKeepTheJobInTheLanguageSwitch() throws Exception {
        String job = "3f2b8c1e-0d4a-4b6f-9a51-7c2e1f0b9d33";

        HttpResponse<String> page = get("/cargar?job=" + job + "&lang=en");

        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("<html lang=\"en\">")
                .contains("href=\"/cargar?job=" + job + "&amp;lang=es\"");
    }

    @Test
    @DisplayName("un 405 pasa por el /error real: título «Error 405», pie con TMDB y sin código EFX")
    void shouldTitleAMethodNotAllowed() throws Exception {
        HttpResponse<String> page = http.send(HttpRequest.newBuilder(uri("/acerca"))
                .header("Accept", "text/html").DELETE().build(), HttpResponse.BodyHandlers.ofString());

        assertThat(page.statusCode()).isEqualTo(405);
        assertThat(page.body()).contains("<h1>Error 405</h1>").contains(CatalogTexts.TMDB).doesNotContain("EFX-");
    }

    @Test
    @DisplayName("la página de error pide el estado del catálogo una sola vez")
    void shouldReadTheCatalogOnceForAnErrorPage() throws Exception {
        clearInvocations(catalog);

        http.send(HttpRequest.newBuilder(uri("/acerca")).header("Accept", "text/html").DELETE().build(),
                HttpResponse.BodyHandlers.ofString());

        verify(catalog, times(1)).status();
    }

    @Test
    @DisplayName("un 400 del contenedor pasa por el /error real con su título")
    void shouldTitleABadRequest() throws Exception {
        // java.net.URI rechaza «%zz»: se manda la línea de petición tal cual por un socket.
        String response = raw("GET /buscar?q=%zz HTTP/1.1\r\nHost: localhost\r\nAccept: text/html\r\n"
                + "Connection: close\r\n\r\n");

        assertThat(response).startsWith("HTTP/1.1 400").contains("<h1>Error 400</h1>").contains(CatalogTexts.TMDB);
    }

    @Test
    @DisplayName("en el /error real, el selector usa la página original: GET conserva la URL y otro método va a Inicio")
    void shouldLinkTheLanguageSwitchToTheOriginalPageOnTheRealErrorDispatch() throws Exception {
        // when
        String badRequest = raw("GET /buscar?q=%zz&modo=fulltext HTTP/1.1\r\nHost: localhost\r\n"
                + "Accept: text/html\r\nConnection: close\r\n\r\n");
        HttpResponse<String> notAllowed = http.send(HttpRequest.newBuilder(uri("/acerca?x=1"))
                .header("Accept", "text/html").DELETE().build(), HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(badRequest).contains("href=\"/buscar?modo=fulltext&amp;lang=en\"");
        assertThat(notAllowed.body()).contains("href=\"/?lang=en\"").doesNotContain("x=1");
    }

    private static Movie movie(String id, String title) {
        return new Movie(
                id, null, title, title, "fr", LocalDate.of(1990, 1, 1), 100, "Released",
                false, false, null, null, "Una sinopsis", List.of("Drama"), List.of(), List.of(), null,
                null, null, null, 7.0, 10, "PG", null, null, null,
                List.of(), List.of(), List.of(), null, null, null, null);
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(uri(path)).header("Accept", "text/html").GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String raw(String request) throws IOException {
        try (Socket socket = new Socket("localhost", port)) {
            socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
