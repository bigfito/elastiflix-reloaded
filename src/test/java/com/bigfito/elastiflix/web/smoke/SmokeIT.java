package com.bigfito.elastiflix.web.smoke;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Pattern;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.Assumptions;
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
 * <p>La salud afirma las 6 899 películas del catálogo de referencia. Si {@code _count} ya no es ese número, esa prueba
 * se omite. La validación del CSV compara alias, índice y conteo antes y después: si alguno
 * cambia, la prueba falla.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SmokeIT {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Path SAMPLE = Path.of("elasticsearch", "fixtures", "movies-sample.csv");
    private static final String UP = "UP";
    private static final String VALID = "valido";
    private static final String RECEIVED = "recibido";
    private static final String VALIDATING = "validando";
    private static final String VECTORS_IGNORED = "EFX-ING-020";
    private static final String HIT = "class=\"hit\"";
    private static final int READY_STEP = 4;
    private static final long CATALOG_COUNT = 6_899L;
    private static final Pattern INDEX_NAME = Pattern.compile("[a-z0-9][a-z0-9-]*-\\d{8}-\\d{6}");
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
    private static final String DETAIL = "/pelicula/{id}?q={q}&modo=fulltext&tipo=match&campo=title&lang={lang}";
    private static final String TITANIC_ID = "597";
    private static final String TITANIC = "titanic";
    private static final String SPANISH_PAGE = "<html lang=\"es\">";
    private static final String ENGLISH_PAGE = "<html lang=\"en\">";

    private final RestClient http;
    private final CatalogIndex catalog;
    private final String alias;

    /**
     * Spring no inyecta el constructor de una prueba si no lleva {@code @Autowired}.
     *
     * @param port       puerto aleatorio del servidor de esta prueba
     * @param catalog    {@code _count} del alias, solo lectura
     * @param properties alias configurado, no un literal
     */
    @Autowired
    SmokeIT(@LocalServerPort int port, CatalogIndex catalog, ElastiflixProperties properties) {
        this.http = RestClient.builder().baseUrl("http://localhost:" + port).build();
        this.catalog = catalog;
        this.alias = properties.index().alias();
    }

    @Test
    @DisplayName("el catálogo responde UP con las 6899 películas del catálogo de referencia")
    void shouldReportTheLiveCatalogCount() {
        // given
        long counted = catalog.count(alias);
        Assumptions.assumeTrue(counted == CATALOG_COUNT, () ->
                "El catálogo cambió: se esperaban " + CATALOG_COUNT + " películas y hay " + counted);

        // when
        Catalog seen = readCatalog();

        // then
        assertThat(seen.alias()).isEqualTo(alias);
        // La app crea el índice con el sufijo -AAAAMMDD-HHMMSS. El prefijo puede ser otro alias: la ronda 4 de
        // calidad publicó moviendo el alias a un índice de staging (data-quality/APRENDIZAJES.md, §6).
        assertThat(seen.index()).isNotEqualTo(alias).matches(INDEX_NAME);
        assertThat(seen.documents()).isEqualTo(CATALOG_COUNT);
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
    @DisplayName("el detalle de Titanic, abierto desde una búsqueda, responde 200 en español y en inglés")
    void shouldOpenTheDetailFromASearch() {
        // when
        String spanish = text(http.get().uri(DETAIL, TITANIC_ID, TITANIC, "es").retrieve().toEntity(String.class));
        String english = text(http.get().uri(DETAIL, TITANIC_ID, TITANIC, "en").retrieve().toEntity(String.class));

        // then
        assertThat(spanish).contains(SPANISH_PAGE, "Titanic");
        assertThat(english).contains(ENGLISH_PAGE, "Titanic");
    }

    @Test
    @DisplayName("con lang=en, inicio, resultados, acerca de y carga responden 200 en inglés")
    void shouldServeThePagesInEnglish() {
        // then
        assertPage("/?lang=en", "Full-text (lexical)");
        assertPage("/acerca?lang=en", "Credits");
        assertPage("/cargar?lang=en", "Import .json");
        String results = text(http.get().uri(FULL_TEXT + "&lang=en", DARK_KNIGHT).retrieve().toEntity(String.class));
        assertThat(results).contains(ENGLISH_PAGE, HIT);
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
        // La respuesta es {job, texts}.
        return JSON.readTree(body).path("job");
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
