package com.bigfito.elastiflix.ingest.job;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.catalog.CatalogName;
import com.bigfito.elastiflix.core.catalog.ElasticsearchCatalogIndex;
import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.embedding.CosineSimilarity;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.IngestTestApplication;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.job.SaveResult.Saved;
import com.bigfito.elastiflix.ingest.service.IngestService;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.context.TestConfiguration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Importa y cancela sobre un alias {@code elastiflix-it-<aleatorio>}. No toca el catálogo real.
 */
@SpringBootTest(classes = {IngestTestApplication.class, IngestIT.GateConfig.class})
class IngestIT {

    private static final Path FIXTURE = Path.of("..", "elasticsearch", "fixtures", "movies-sample.json");
    private static final Path QUERY = Path.of("..", "elasticsearch", "fixtures", "query-vector-titanic.json");
    private static final String ALIAS = "elastiflix-it-"
            + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    private static final String PREVIOUS = ALIAS + "-20200101-000000";
    private static final String SINGLE_ID = "990000002";
    private static final double COSINE_1024 = 0.999;
    private static final double COSINE_SHORT = 0.995;
    private static final int K = 10;
    private static final int CANDIDATES = 20;

    private final IngestService service;
    private final CatalogIndex catalog;
    private final ElasticsearchClient client;
    private final ElastiflixProperties properties;
    private final CancelGate gate;
    private final List<String> jobIds = new ArrayList<>();

    /**
     * Spring no inyecta el constructor de una prueba si no lleva {@code @Autowired}.
     *
     * @param service    fachada
     * @param catalog    catálogo, con el decorador de cancelación
     * @param client     solo para apuntar el alias y hacer la búsqueda vectorial
     * @param properties alias que tiene que ser el de esta prueba
     * @param gate       se arma solo en la prueba de cancelación
     */
    @Autowired
    IngestIT(IngestService service, CatalogIndex catalog, ElasticsearchClient client,
            ElastiflixProperties properties, CancelGate gate) {
        this.service = service;
        this.catalog = catalog;
        this.client = client;
        this.properties = properties;
        this.gate = gate;
    }

    @DynamicPropertySource
    static void alias(DynamicPropertyRegistry registry) {
        requireSafe(ALIAS);
        registry.add("elastiflix.index.alias", () -> ALIAS);
        registry.add("elastiflix.bootstrap.enabled", () -> "false");
    }

    @BeforeEach
    void prepareCatalog() throws Exception {
        requireSafe(properties.index().alias());
        assertThat(properties.index().alias()).isEqualTo(ALIAS);
        gate.clear();
        deleteOwned();
        ImportSample sample = ImportSample.read(FIXTURE);
        catalog.createWithMapping(PREVIOUS);
        catalog.writeAll(PREVIOUS, sample.fixture());
        catalog.refresh(PREVIOUS);
        client.indices().updateAliases(request -> request.actions(
                Action.of(action -> action.add(add -> add.alias(ALIAS).index(PREVIOUS)))));
        assertThat(catalog.count(ALIAS)).isEqualTo(sample.fixture().size());
    }

    @AfterEach
    void deleteTheTestAlias() {
        stopJobs();
        deleteOwned();
    }

    @Test
    @DisplayName("importa sin vectores, reemplaza el título y el registro se ve al momento")
    void shouldPublishTheFileAndSaveOneRecord() throws Exception {
        ImportSample sample = ImportSample.read(FIXTURE);
        List<String> order = relativeOrder(knn());
        Path file = sample.writeImportFile();
        try {
            String jobId = accept("carga.json", file);
            assertThat(waitFor(jobId, "valido", "invalido", "fallido").status()).isEqualTo("valido");
            service.importFile(jobId);
            assertThat(waitFor(jobId, "publicado", "fallido", "cancelado").status()).isEqualTo("publicado");
            assertPublished(sample, order);
            assertSaved(oneMovie());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    @DisplayName("cancelar al volver de reindex deja el alias y el índice anterior")
    void shouldKeepThePreviousIndexWhenCancelledAfterReindex() throws Exception {
        ImportSample sample = ImportSample.read(FIXTURE);
        Path file = sample.writeImportFile();
        try {
            String jobId = accept("carga.json", file);
            assertThat(waitFor(jobId, "valido", "invalido", "fallido").status()).isEqualTo("valido");
            gate.arm(() -> service.cancel(jobId));
            service.importFile(jobId);
            assertThat(waitFor(jobId, "cancelado", "publicado", "fallido").status()).isEqualTo("cancelado");
            CatalogName described = catalog.describe(ALIAS);
            assertThat(described).isInstanceOf(CatalogName.Alias.class);
            assertThat(((CatalogName.Alias) described).concreteIndex()).isEqualTo(PREVIOUS);
            assertThat(catalog.listOwnedIndexes(ALIAS)).containsExactly(PREVIOUS);
            assertThat(catalog.count(ALIAS)).isEqualTo(sample.fixture().size());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private void assertPublished(ImportSample sample, List<String> order) throws Exception {
        assertThat(catalog.count(ALIAS)).isEqualTo(sample.fixture().size() + 1);
        assertThat(movie(ImportSample.TITANIC).title()).isEqualTo(ImportSample.REPLACED_TITLE);
        assertThat(movie(ImportSample.EXTRA_ID).id()).isEqualTo(ImportSample.EXTRA_ID);
        CatalogName described = catalog.describe(ALIAS);
        assertThat(described).isInstanceOf(CatalogName.Alias.class);
        String current = ((CatalogName.Alias) described).concreteIndex();
        assertThat(current).startsWith(ALIAS + "-").isNotEqualTo(PREVIOUS);
        assertThat(catalog.describe(PREVIOUS)).isInstanceOf(CatalogName.Missing.class);
        assertCosine(sample);
        assertThat(relativeOrder(knn())).containsExactlyElementsOf(order);
    }

    private void assertSaved(Movie movie) {
        assertThat(service.saveOne(movie)).isInstanceOf(Saved.class);
        List<Movie> found = catalog.readWithVectors(ALIAS, List.of(SINGLE_ID));
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().title()).isEqualTo("Suelta");
    }

    private void assertCosine(ImportSample sample) {
        for (Movie original : sample.fixture()) {
            Movie again = movie(original.id());
            assertThat(CosineSimilarity.between(original.plot1024(), again.plot1024()))
                    .isGreaterThanOrEqualTo(COSINE_1024);
            assertThat(CosineSimilarity.between(original.plot512(), again.plot512()))
                    .isGreaterThanOrEqualTo(COSINE_SHORT);
            assertThat(CosineSimilarity.between(original.plot256(), again.plot256()))
                    .isGreaterThanOrEqualTo(COSINE_SHORT);
        }
    }

    private Movie movie(String id) {
        return catalog.readWithVectors(ALIAS, List.of(id)).getFirst();
    }

    private List<String> knn() throws Exception {
        requireSafe(ALIAS);
        List<Float> vector = queryVector();
        return client.search(request -> request.index(ALIAS).size(K)
                .source(source -> source.fetch(false))
                .retriever(retriever -> retriever.knn(knn -> knn
                        .field("plot_1024")
                        .queryVector(vector)
                        .k(K)
                        .numCandidates(CANDIDATES))), Void.class)
                .hits().hits().stream().map(Hit::id).toList();
    }

    private static List<Float> queryVector() {
        JsonMapper mapper = MovieJsonMapper.create();
        List<Float> values = new ArrayList<>();
        for (JsonNode number : mapper.readTree(QUERY.toFile()).get("vector_1024")) {
            values.add(number.floatValue());
        }
        return values;
    }

    private List<String> relativeOrder(List<String> hits) {
        List<String> original = ImportSample.read(FIXTURE).originalIds();
        List<String> order = new ArrayList<>();
        for (String id : hits) {
            if (original.contains(id)) {
                order.add(id);
            }
        }
        return order;
    }

    private String accept(String name, Path file) {
        String jobId = service.acceptFile(name, file, CsvSettings.withHeader());
        jobIds.add(jobId);
        return jobId;
    }

    private void stopJobs() {
        for (String jobId : jobIds) {
            service.cancel(jobId);
            JobView view = waitFor(jobId, "publicado", "fallido", "cancelado", "invalido");
            if (!terminal(view.status())) {
                throw new IllegalStateException("el trabajo sigue en " + view.status());
            }
        }
    }

    private static boolean terminal(String status) {
        return "publicado".equals(status)
                || "fallido".equals(status)
                || "cancelado".equals(status)
                || "invalido".equals(status);
    }

    private JobView waitFor(String jobId, String... done) {
        List<String> expected = List.of(done);
        long end = System.nanoTime() + Duration.ofMinutes(4).toNanos();
        JobView view = service.job(jobId);
        while (System.nanoTime() < end && !expected.contains(view.status())) {
            LockSupport.parkNanos(Duration.ofMillis(20).toNanos());
            view = service.job(jobId);
        }
        return view;
    }

    private static Movie oneMovie() {
        return MovieJsonMapper.create().readValue(
                "{\"id\":\"" + SINGLE_ID + "\",\"title\":\"Suelta\",\"plot\":\"una historia corta para la prueba\"}",
                Movie.class);
    }

    private void deleteOwned() {
        requireSafe(ALIAS);
        for (String index : List.copyOf(catalog.listOwnedIndexes(ALIAS))) {
            deleteOne(index);
        }
    }

    private void deleteOne(String index) {
        if (!index.startsWith(ALIAS + "-") || index.contains("reloaded")) {
            throw new IllegalStateException("no se borra " + index);
        }
        catalog.deleteIndex(index);
    }

    private static void requireSafe(String alias) {
        if (!alias.startsWith("elastiflix-it-") || alias.contains("reloaded")) {
            throw new IllegalStateException("alias de prueba inseguro");
        }
    }

    /**
     * Envuelve el catálogo para cancelar justo cuando {@code reindex} vuelve.
     */
    @TestConfiguration
    static class GateConfig {

        @Bean
        CancelGate cancelGate() {
            return new CancelGate();
        }

        @Bean
        @Primary
        CatalogIndex catalogIndex(ElasticsearchCatalogIndex delegate, CancelGate cancelGate) {
            return new CancelAfterReindex(delegate, cancelGate);
        }
    }
}
