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

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
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
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/buscar?q=titanic", "/cargar", "/acerca", "/no-existe"})
    @DisplayName("Resultados, Cargar, Acerca y el 404 llevan la atribución de TMDB en el pie")
    void shouldShowTheTmdbNoticeOnEveryPage(String path) throws Exception {
        HttpResponse<String> page = get(path);

        assertThat(page.body()).contains("<footer").contains(CatalogTexts.TMDB)
                .contains("Índice: elastiflix-reloaded · 6 899 películas");
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
