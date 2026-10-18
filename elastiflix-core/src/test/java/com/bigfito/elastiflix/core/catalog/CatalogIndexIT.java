package com.bigfito.elastiflix.core.catalog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bigfito.elastiflix.core.TestApplication;
import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.embedding.CosineSimilarity;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Publica un alias de prueba en una sola llamada y no toca el catálogo real.
 */
@SpringBootTest(classes = TestApplication.class)
class CatalogIndexIT {

    private static final Path FIXTURE = Path.of("..", "elasticsearch", "fixtures", "movies-sample.json");
    private static final double MINIMUM_COSINE = 0.9999;
    private static final int NOT_FOUND = 404;
    private static final int READ_LIMIT = 10;

    private final CatalogIndex catalog;
    private final ElasticsearchClient client;
    private final String indexA;
    private final String indexB;
    private final String alias;
    private final List<String> created = new ArrayList<>();

    /**
     * Spring no inyecta el constructor de una prueba si no lleva {@code @Autowired}.
     *
     * @param catalog puerta del catálogo
     * @param client  cliente, solo para escribir las 4 películas y limpiar
     */
    @Autowired
    CatalogIndexIT(CatalogIndex catalog, ElasticsearchClient client) {
        this.catalog = catalog;
        this.client = client;
        String token = UUID.randomUUID().toString().substring(0, 8);
        indexA = "elastiflix-it-" + token + "-a";
        indexB = "elastiflix-it-" + token + "-b";
        alias = "elastiflix-it-" + token;
    }

    @AfterEach
    void deleteWhatTheTestCreated() {
        for (String name : created) {
            deleteIfPresent(name);
        }
    }

    @Test
    @DisplayName("reindexa, publica el alias en -b, borra -a y conserva los vectores")
    void shouldPublishOneAliasWhenTheCopyKeepsTheVectors() throws Exception {
        List<Movie> fixture = readFixture();
        create(indexA);
        writeMovies(indexA, fixture);
        catalog.refresh(indexA);
        long count = catalog.count(indexA);
        assertThat(catalog.describe(indexA)).isInstanceOf(CatalogName.ConcreteIndex.class);

        create(indexB);
        catalog.reindex(indexA, indexB, () -> false);
        catalog.refresh(indexB);
        assertThat(catalog.count(indexB)).isEqualTo(count);

        catalog.publish(alias, indexB, indexA);
        CatalogName published = catalog.describe(alias);
        assertThat(published).isInstanceOf(CatalogName.Alias.class);
        assertThat(((CatalogName.Alias) published).concreteIndex()).isEqualTo(indexB);
        assertThat(catalog.describe(indexA)).isInstanceOf(CatalogName.Missing.class);
        assertThat(catalog.listOwnedIndexes(alias)).isEmpty();

        List<Movie> copied = catalog.readAllWithVectors(alias, READ_LIMIT);
        assertThat(copied).hasSize(fixture.size());
        for (Movie original : fixture) {
            Movie again = movie(copied, original.id());
            assertThat(CosineSimilarity.between(original.plot1024(), again.plot1024()))
                    .isGreaterThanOrEqualTo(MINIMUM_COSINE);
            assertThat(CosineSimilarity.between(original.plot512(), again.plot512()))
                    .isGreaterThanOrEqualTo(MINIMUM_COSINE);
            assertThat(CosineSimilarity.between(original.plot256(), again.plot256()))
                    .isGreaterThanOrEqualTo(MINIMUM_COSINE);
        }

        catalog.deleteIndex(indexB);
        created.remove(indexB);
        assertThat(catalog.describe(indexB)).isInstanceOf(CatalogName.Missing.class);
    }

    @Test
    @DisplayName("listOwnedIndexes trae solo los índices <alias>-<AAAAMMDD-HHMMSS> de ese alias")
    void shouldListOnlyTheDatedIndexesOfTheAlias() {
        String dated = alias + "-20261018-030405";
        String undated = alias + "-copia";
        String foreign = alias + "x-20261018-030405";
        create(dated);
        create(undated);
        create(foreign);

        assertThat(catalog.listOwnedIndexes(alias)).containsExactly(dated);
    }

    @Test
    @DisplayName("indexOne con require_alias no crea un índice concreto si el alias no existe")
    void shouldNotCreateAnIndexWhenTheAliasIsMissing() throws Exception {
        Movie movie = readFixture().getFirst();
        created.add(alias);

        assertThatThrownBy(() -> catalog.indexOne(alias, movie)).isInstanceOf(ElasticsearchAccessException.class);
        assertThat(catalog.describe(alias)).isInstanceOf(CatalogName.Missing.class);
    }

    private void create(String indexName) {
        catalog.createWithMapping(indexName);
        created.add(indexName);
    }

    private void writeMovies(String indexName, List<Movie> movies) throws Exception {
        for (Movie movie : movies) {
            client.index(request -> request.index(indexName).id(movie.id()).document(movie));
        }
    }

    private void deleteIfPresent(String indexName) {
        try {
            client.indices().delete(request -> request.index(indexName));
        } catch (ElasticsearchException exception) {
            if (exception.status() != NOT_FOUND) {
                throw new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE, exception);
            }
        } catch (IOException exception) {
            throw new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE, exception);
        }
    }

    private static List<Movie> readFixture() {
        JsonMapper mapper = MovieJsonMapper.create();
        JsonNode documents = mapper.readTree(FIXTURE.toFile()).get("documents");
        List<Movie> movies = new ArrayList<>();
        for (JsonNode document : documents) {
            movies.add(mapper.treeToValue(document, Movie.class));
        }
        return movies;
    }

    private static Movie movie(List<Movie> movies, String id) {
        return movies.stream().filter(movie -> id.equals(movie.id())).findFirst().orElseThrow();
    }
}
