package com.bigfito.elastiflix.web.smoke;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Pattern;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Recorre la aplicación contra el catálogo real. Solo lee: no importa ni publica.
 *
 * <p>El conteo no está fijo, porque una carga cambia el número de películas. Al empezar se lee
 * {@code _count} del alias y se exige que la salud diga lo mismo, y que sea mayor que cero. La
 * validación del CSV compara alias, índice y conteo antes y después: si alguno cambia, la prueba
 * falla.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SmokeIT {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Path SAMPLE = Path.of("..", "elasticsearch", "fixtures", "movies-sample.csv");
    private static final String ALIAS = "elastiflix-reloaded";
    private static final String UP = "UP";
    private static final String VALID = "valido";
    private static final String RECEIVED = "recibido";
    private static final String VALIDATING = "validando";
    private static final String VECTORS_IGNORED = "EFX-ING-020";
    private static final String HIT = "class=\"hit\"";
    private static final int READY_STEP = 4;
    private static final int SAMPLE_MOVIES = 4;
    private static final int DEFAULT_K = 100;
    private static final int DEFAULT_CANDIDATES = 200;
    private static final long WAIT_SECONDS = 60L;
    private static final long POLL_MILLIS = 50L;
    private static final String DARK_KNIGHT = "dark knight";
    private static final String ICEBERG =
            "un barco de lujo choca con un iceberg en su viaje inaugural";
    private static final String TOY_STORY = "toy story juguetes que cobran vida";
    private static final String FULL_TEXT =
            "/buscar?q={q}&modo=fulltext&tipo=match&operador=or&campo=title";
    private static final String VECTOR =
            "/buscar?q={q}&modo=vectorial&vector=plot_1024&k={k}&candidatos={c}";
    private static final String HYBRID =
            "/buscar?q={q}&modo=hibrida&tipo=match&campo=title&operador=and&vector=plot_256&k={k}&candidatos={c}";

    private final RestClient http;
    private final CatalogIndex catalog;

    /**
     * Spring no inyecta el constructor de una prueba si no lleva {@code @Autowired}.
     *
     * @param port    puerto aleatorio del servidor de esta prueba
     * @param catalog {@code _count} del alias, solo lectura
     */
    @Autowired
    SmokeIT(@LocalServerPort int port, CatalogIndex catalog) {
        this.http = RestClient.builder().baseUrl("http://localhost:" + port).build();
        this.catalog = catalog;
    }

    @Test
    @DisplayName("el catálogo responde UP con el mismo conteo que _count")
    void shouldReportTheLiveCatalogCount() {
        // given
        long counted = catalog.count(ALIAS);

        // when
        Catalog seen = readCatalog();

        // then
        assertThat(counted).isPositive();
        assertThat(seen.alias()).isEqualTo(ALIAS);
        assertThat(seen.index()).matches(Pattern.quote(ALIAS) + "-\\d{8}-\\d{6}");
        assertThat(seen.documents()).isEqualTo(counted);
    }

    @Test
    @DisplayName("inicio, acerca de y carga responden 200")
    void shouldOpenHomeAboutAndIngest() {
        // then
        assertPage("/", "Full-text (léxica)");
        assertPage("/acerca", "Créditos");
        assertPage("/cargar", "Importación en un índice temporal");
    }

    @Test
    @DisplayName("una sugerencia de cada modo devuelve resultados")
    void shouldReturnHitsForOneSuggestionOfEachMode() {
        // given
        String home = text(http.get().uri("/").retrieve().toEntity(String.class));

        // then
        assertThat(home).contains("q=dark%20knight", "vector=plot_1024", "modo=hibrida");
        assertHits(FULL_TEXT, DARK_KNIGHT);
        assertHits(VECTOR, ICEBERG, DEFAULT_K, DEFAULT_CANDIDATES);
        assertHits(HYBRID, TOY_STORY, DEFAULT_K, DEFAULT_CANDIDATES);
    }

    @Test
    @DisplayName("el CSV de muestra queda listo para importar y el catálogo no cambia")
    void shouldValidateTheSampleCsvWithoutImporting() {
        // given
        Catalog before = readCatalog();

        // when
        JsonNode job = waitUntilChecked(uploadSample());

        // then
        assertReady(job);
        assertThat(readCatalog()).isEqualTo(before);
    }

    private void assertPage(String path, String text) {
        ResponseEntity<String> page = http.get().uri(path).retrieve().toEntity(String.class);
        assertThat(page.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
        assertThat(page.getBody()).contains(text);
    }

    private void assertHits(String template, Object... values) {
        String page = text(http.get().uri(template, values).retrieve().toEntity(String.class));
        assertThat(page).contains(HIT);
    }

    private Catalog readCatalog() {
        ResponseEntity<String> response = http.get().uri("/actuator/health").retrieve().toEntity(String.class);
        JsonNode root = JSON.readTree(text(response));
        JsonNode component = root.path("components").path("elastiflixCatalog");
        JsonNode details = component.path("details");
        assertThat(root.path("status").asString()).isEqualTo(UP);
        assertThat(component.path("status").asString()).isEqualTo(UP);
        return new Catalog(
                details.path("alias").asString(),
                details.path("index").asString(),
                details.path("documents").asLong());
    }

    private String uploadSample() {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("archivo", new FileSystemResource(SAMPLE));
        form.add("separador", ",");
        form.add("encabezado", "true");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        String body = text(http.post()
                .uri("/cargar/archivo")
                .headers(current -> current.addAll(headers))
                .body(form)
                .retrieve()
                .toEntity(String.class));
        return JSON.readTree(body).path("jobId").asString();
    }

    private JsonNode waitUntilChecked(String jobId) {
        long end = System.nanoTime() + Duration.ofSeconds(WAIT_SECONDS).toNanos();
        JsonNode job = job(jobId);
        while (System.nanoTime() < end && pending(job)) {
            LockSupport.parkNanos(Duration.ofMillis(POLL_MILLIS).toNanos());
            job = job(jobId);
        }
        return job;
    }

    private JsonNode job(String jobId) {
        String body = text(http.get().uri("/cargar/trabajos/{id}", jobId).retrieve().toEntity(String.class));
        return JSON.readTree(body);
    }

    private static boolean pending(JsonNode job) {
        String status = job.path("status").asString();
        return RECEIVED.equals(status) || VALIDATING.equals(status);
    }

    private static void assertReady(JsonNode job) {
        assertThat(job.path("status").asString())
                .withFailMessage("estado %s, errores %s", job.path("status"), job.path("errors"))
                .isEqualTo(VALID);
        assertThat(job.path("step").asInt()).isEqualTo(READY_STEP);
        assertThat(job.path("errors")).isEmpty();
        assertThat(job.path("fileCount").asInt()).isEqualTo(SAMPLE_MOVIES);
        assertThat(ignoredVectorFields(job)).containsExactlyInAnyOrder("plot_256", "plot_512", "plot_1024");
    }

    private static List<String> ignoredVectorFields(JsonNode job) {
        List<String> fields = new ArrayList<>();
        for (JsonNode warning : job.path("warnings")) {
            if (VECTORS_IGNORED.equals(warning.path("code").asString())) {
                fields.add(warning.path("field").asString());
            }
        }
        return fields;
    }

    private static String text(ResponseEntity<String> response) {
        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    /**
     * Lo que {@code /actuator/health} dice del catálogo.
     *
     * @param alias     nombre del alias
     * @param index     índice concreto
     * @param documents películas contadas
     */
    private record Catalog(String alias, String index, long documents) {
    }
}
