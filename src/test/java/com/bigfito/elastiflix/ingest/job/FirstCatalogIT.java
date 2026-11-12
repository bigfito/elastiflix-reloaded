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
import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.job.SaveResult.Saved;
import com.bigfito.elastiflix.ingest.service.IngestService;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La primera carga crea el catálogo, sobre un alias {@code elastiflix-it-<aleatorio>} que no existe
 * al empezar cada prueba. No toca el catálogo real y borra lo que creó.
 */
@SpringBootTest
class FirstCatalogIT {

    private static final Path FIXTURE = Path.of("elasticsearch", "fixtures", "movies-sample.json");
    private static final String ALIAS = "elastiflix-it-"
            + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    private static final String SINGLE_ID = "990000003";

    private final IngestService service;
    private final CatalogIndex catalog;
    private final ElasticsearchClient client;
    private final ElastiflixProperties properties;
    private final List<String> jobIds = new ArrayList<>();

    /**
     * Spring no inyecta el constructor de una prueba si no lleva {@code @Autowired}.
     *
     * @param service    fachada
     * @param catalog    catálogo
     * @param client     solo para leer el mapping del índice creado
     * @param properties alias que tiene que ser el de esta prueba
     */
    @Autowired
    FirstCatalogIT(IngestService service, CatalogIndex catalog, ElasticsearchClient client,
            ElastiflixProperties properties) {
        this.service = service;
        this.catalog = catalog;
        this.client = client;
        this.properties = properties;
    }

    @DynamicPropertySource
    static void alias(DynamicPropertyRegistry registry) {
        requireSafe(ALIAS);
        registry.add("elastiflix.index.alias", () -> ALIAS);
        registry.add("elastiflix.bootstrap.enabled", () -> "false");
    }

    @BeforeEach
    void startWithoutCatalog() {
        requireSafe(properties.index().alias());
        assertThat(properties.index().alias()).isEqualTo(ALIAS);
        deleteOwned();
        assertThat(catalog.describe(ALIAS)).isInstanceOf(CatalogName.Missing.class);
    }

    @AfterEach
    void deleteWhatWasCreated() {
        try {
            for (String jobId : jobIds) {
                service.cancel(jobId);
                waitFor(jobId, "publicado", "agregado", "fallido", "cancelado", "invalido");
            }
        } finally {
            deleteOwned();
        }
    }

    @Test
    @DisplayName("append sin catálogo falla con EFX-IMP-010 y no crea nada")
    void shouldBlockAppendWithoutCatalog() throws Exception {
        Path file = ImportSample.read(FIXTURE).writeImportFile();
        try {
            JobView failed = waitFor(accept(file, ImportMode.APPEND), "fallido", "valido", "invalido");
            assertThat(failed.status()).isEqualTo("fallido");
            assertThat(failed.failureCode()).isEqualTo(ErrorCode.APPEND_WITHOUT_CATALOG.code());
            assertThat(catalog.describe(ALIAS)).isInstanceOf(CatalogName.Missing.class);
            assertThat(catalog.listOwnedIndexes(ALIAS)).isEmpty();
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    @DisplayName("bulk sin catálogo crea el alias, un índice con el mapping del repo y las películas del archivo")
    void shouldCreateTheCatalogWithTheFirstBulk() throws Exception {
        ImportSample sample = ImportSample.read(FIXTURE);
        Path file = sample.writeImportFile();
        try {
            String jobId = accept(file, ImportMode.BULK);
            JobView valid = waitFor(jobId, "valido", "invalido", "fallido");
            assertThat(valid.status()).isEqualTo("valido");
            assertThat(valid.replaceCount()).isZero();
            service.importFile(jobId);
            assertThat(waitFor(jobId, "publicado", "fallido", "cancelado").status()).isEqualTo("publicado");
            assertMappedCatalog();
            assertThat(catalog.count(ALIAS)).isEqualTo(sample.fixture().size() + 1);
            assertThat(catalog.readWithVectors(ALIAS, List.of(ImportSample.EXTRA_ID))).hasSize(1);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    @DisplayName("un registro individual sin catálogo lo crea y la película se lee al momento")
    void shouldCreateTheCatalogWithTheFirstRecord() throws Exception {
        assertThat(service.saveOne(oneMovie())).isInstanceOf(Saved.class);

        assertMappedCatalog();
        List<Movie> found = catalog.readWithVectors(ALIAS, List.of(SINGLE_ID));
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().plot1024()).hasSize(1024);
    }

    /** El alias apunta a un índice propio que tiene el mapping del repo (se comprueba un campo de cada tipo). */
    private void assertMappedCatalog() throws Exception {
        CatalogName described = catalog.describe(ALIAS);
        assertThat(described).isInstanceOf(CatalogName.Alias.class);
        String current = ((CatalogName.Alias) described).concreteIndex();
        assertThat(current).startsWith(ALIAS + "-");
        assertThat(catalog.listOwnedIndexes(ALIAS)).containsExactly(current);
        var fields = client.indices().getMapping(request -> request.index(current))
                .get(current).mappings().properties();
        Property vector = fields.get("plot_1024");
        assertThat(vector.isDenseVector()).isTrue();
        assertThat(vector.denseVector().dims()).isEqualTo(1024);
        assertThat(fields.get("title").isText()).isTrue();
        assertThat(fields.get("release_date").isDate()).isTrue();
    }

    private String accept(Path file, ImportMode mode) {
        String jobId = service.acceptFile("carga.json", file, CsvSettings.withHeader(), mode);
        jobIds.add(jobId);
        return jobId;
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
                "{\"id\":\"" + SINGLE_ID + "\",\"title\":\"Primera\",\"plot\":\"la primera película del catálogo de demostración\"}",
                Movie.class);
    }

    private void deleteOwned() {
        requireSafe(ALIAS);
        for (String index : List.copyOf(catalog.listOwnedIndexes(ALIAS))) {
            if (!index.startsWith(ALIAS + "-") || index.contains("reloaded")) {
                throw new IllegalStateException("no se borra " + index);
            }
            catalog.deleteIndex(index);
        }
    }

    private static void requireSafe(String alias) {
        if (!alias.startsWith("elastiflix-it-") || alias.contains("reloaded")) {
            throw new IllegalStateException("alias de prueba inseguro");
        }
    }
}
