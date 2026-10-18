package com.bigfito.elastiflix.core.catalog;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Los tres caminos de la migración, sin hablar con Elasticsearch.
 */
class CatalogBootstrapTest {

    private static final String ALIAS = "demo";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T15:30:00Z"), ZoneOffset.UTC);
    private static final String NEW_INDEX = "demo-20261020-153000";
    private static final long DOCUMENTS = 4;

    @Test
    @DisplayName("si ya es alias, no crea ni publica")
    void shouldDoNothingWhenTheAliasAlreadyExists() {
        FakeCatalog catalog = new FakeCatalog(new CatalogName.Alias(ALIAS, NEW_INDEX), false, false);

        bootstrap(catalog).migrate();

        assertThat(catalog.calls).containsExactly("describe", "count");
    }

    @Test
    @DisplayName("si el conteo del log falla, el alias que ya existe no tumba la migración")
    void shouldKeepGoingWhenTheLogCountFails() {
        FakeCatalog catalog = new FakeCatalog(new CatalogName.Alias(ALIAS, NEW_INDEX), false, false);
        catalog.failCount = true;

        bootstrap(catalog).migrate();

        assertThat(catalog.calls).containsExactly("describe", "count");
    }

    @Test
    @DisplayName("si no existe, se detiene con EFX-ES-002")
    void shouldStopWhenTheCatalogIsMissing() {
        FakeCatalog catalog = new FakeCatalog(new CatalogName.Missing(ALIAS), false, false);

        assertThatThrownBy(() -> bootstrap(catalog).migrate())
                .isInstanceOf(ElasticsearchAccessException.class)
                .extracting(thrown -> ((ElasticsearchAccessException) thrown).errorCode())
                .isEqualTo(ErrorCode.CATALOG_NOT_FOUND);
        assertThat(catalog.calls).containsExactly("describe");
    }

    @Test
    @DisplayName("un índice concreto se copia y se publica en una sola llamada")
    void shouldPublishWhenTheCountsMatch() {
        FakeCatalog catalog = new FakeCatalog(new CatalogName.ConcreteIndex(ALIAS), false, false);

        bootstrap(catalog).migrate();

        assertThat(catalog.calls).containsExactly(
                "describe", "create", "reindex", "refresh", "count", "count", "publish");
        assertThat(catalog.created).isEqualTo(NEW_INDEX);
        assertThat(catalog.publishedPrevious).isEqualTo(ALIAS);
        assertThat(catalog.deleted).isNull();
    }

    @Test
    @DisplayName("si el conteo no coincide, borra el índice nuevo y lanza EFX-ES-003")
    void shouldDeleteTheNewIndexWhenTheCountDiffers() {
        FakeCatalog catalog = new FakeCatalog(new CatalogName.ConcreteIndex(ALIAS), true, false);

        assertThatThrownBy(() -> bootstrap(catalog).migrate())
                .isInstanceOf(ElasticsearchAccessException.class)
                .extracting(thrown -> ((ElasticsearchAccessException) thrown).errorCode())
                .isEqualTo(ErrorCode.REINDEX_COUNT_MISMATCH);
        assertThat(catalog.deleted).isEqualTo(NEW_INDEX);
        assertThat(catalog.publishedPrevious).isNull();
    }

    @Test
    @DisplayName("si la publicación falla y el original sigue, borra solo el índice nuevo")
    void shouldDeleteTheNewIndexWhenPublishFails() {
        FakeCatalog catalog = new FakeCatalog(new CatalogName.ConcreteIndex(ALIAS), false, true);

        assertThatThrownBy(() -> bootstrap(catalog).migrate())
                .isInstanceOf(ElasticsearchAccessException.class)
                .extracting(thrown -> ((ElasticsearchAccessException) thrown).errorCode())
                .isEqualTo(ErrorCode.ALIAS_PUBLISH_FAILED);
        assertThat(catalog.deleted).isEqualTo(NEW_INDEX);
    }

    @Test
    @DisplayName("si publicar falla y el alias ya apunta al índice nuevo, no lo borra")
    void shouldTreatTheMoveAsPublishedWhenTheAliasPointsToTheNewIndex() {
        // given
        FakeCatalog catalog = new FakeCatalog(new CatalogName.ConcreteIndex(ALIAS), false, true);
        catalog.afterPublish = new CatalogName.Alias(ALIAS, NEW_INDEX);

        // when
        bootstrap(catalog).migrate();

        // then
        assertThat(catalog.deleted).isNull();
        assertThat(catalog.calls).contains("publish", "describe");
    }

    @Test
    @DisplayName("si publicar falla y el alias no se puede leer, deja el índice nuevo con EFX-IMP-006")
    void shouldKeepTheNewIndexWhenDescribeFailsAfterPublish() {
        // given
        FakeCatalog catalog = new FakeCatalog(new CatalogName.ConcreteIndex(ALIAS), false, true);
        catalog.failDescribeAfterPublish = true;

        // when / then
        assertThatThrownBy(() -> bootstrap(catalog).migrate())
                .isInstanceOf(ElasticsearchAccessException.class)
                .extracting(thrown -> ((ElasticsearchAccessException) thrown).errorCode())
                .isEqualTo(ErrorCode.PUBLISH_UNCERTAIN);
        assertThat(catalog.deleted).isNull();
    }

    @Test
    @DisplayName("si publicar falla y el nombre ya no está, deja el índice nuevo con EFX-IMP-006")
    void shouldKeepTheNewIndexWhenTheAliasIsMissingAfterPublish() {
        // given
        FakeCatalog catalog = new FakeCatalog(new CatalogName.ConcreteIndex(ALIAS), false, true);
        catalog.afterPublish = new CatalogName.Missing(ALIAS);

        // when / then
        assertThatThrownBy(() -> bootstrap(catalog).migrate())
                .isInstanceOf(ElasticsearchAccessException.class)
                .extracting(thrown -> ((ElasticsearchAccessException) thrown).errorCode())
                .isEqualTo(ErrorCode.PUBLISH_UNCERTAIN);
        assertThat(catalog.deleted).isNull();
    }

    private static CatalogBootstrap bootstrap(FakeCatalog catalog) {
        ElastiflixProperties properties = new ElastiflixProperties(
                null, new ElastiflixProperties.Index(ALIAS), null, null, null, null, null);
        return new CatalogBootstrap(catalog, properties, new IndexNames(), CLOCK);
    }

    private static final class FakeCatalog implements CatalogIndex {

        private final boolean mismatch;
        private final boolean failPublish;
        private boolean failCount;
        private boolean failDescribeAfterPublish;
        private CatalogName afterPublish;
        private final List<String> calls = new ArrayList<>();
        private CatalogName described;
        private String created;
        private String publishedPrevious;
        private String deleted;

        private FakeCatalog(CatalogName described, boolean mismatch, boolean failPublish) {
            this.described = described;
            this.mismatch = mismatch;
            this.failPublish = failPublish;
        }

        @Override
        public CatalogName describe(String name) {
            calls.add("describe");
            if (publishedPrevious != null && failDescribeAfterPublish) {
                throw new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE);
            }
            if (publishedPrevious != null && afterPublish != null) {
                return afterPublish;
            }
            return described;
        }

        @Override
        public void createWithMapping(String indexName) {
            calls.add("create");
            created = indexName;
        }

        @Override
        public void reindex(String source, String target) {
            calls.add("reindex");
        }

        @Override
        public void refresh(String name) {
            calls.add("refresh");
        }

        @Override
        public long count(String name) {
            calls.add("count");
            if (failCount) {
                throw new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE);
            }
            if (mismatch && name.equals(created)) {
                return DOCUMENTS - 1;
            }
            return DOCUMENTS;
        }

        @Override
        public void publish(String alias, String newIndex, String previousIndex) {
            calls.add("publish");
            publishedPrevious = previousIndex;
            if (failPublish) {
                throw new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE);
            }
        }

        @Override
        public void deleteIndex(String indexName) {
            calls.add("delete");
            deleted = indexName;
        }

        @Override
        public List<String> listOwnedIndexes(String alias) {
            return List.of();
        }

        @Override
        public List<Movie> readWithVectors(String name, List<String> ids) {
            return List.of();
        }

        @Override
        public List<Movie> readAllWithVectors(String name, int limit) {
            return List.of();
        }

        @Override
        public java.util.Set<String> existingIds(String name, List<String> ids) {
            return java.util.Set.of();
        }

        @Override
        public void writeAll(String indexName, List<Movie> movies) {
            calls.add("write");
        }

        @Override
        public void indexOne(String name, Movie movie) {
            calls.add("indexOne");
        }
    }
}
