package com.bigfito.elastiflix.core.catalog;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El estado distingue alias, índice concreto, ausencia, ambigüedad y caída del cliente.
 */
class CatalogStatusServiceTest {

    private static final String ALIAS = "demo-alias";
    private static final String INDEX = "demo-alias-20261020-153000";
    private static final long DOCUMENTS = 12;

    @Test
    @DisplayName("un alias conectado devuelve el índice y el conteo")
    void shouldCountWhenTheNameIsAnAlias() {
        CatalogStatus status = service(new CatalogName.Alias(ALIAS, INDEX), DOCUMENTS, false).status();

        assertThat(status.connected()).isTrue();
        assertThat(status.concreteIndex()).isEqualTo(INDEX);
        assertThat(status.documentCount()).isEqualTo(DOCUMENTS);
        assertThat(status.errorCode()).isNull();
    }

    @Test
    @DisplayName("un índice concreto también cuenta")
    void shouldCountWhenTheNameIsAConcreteIndex() {
        CatalogStatus status = service(new CatalogName.ConcreteIndex(ALIAS), DOCUMENTS, false).status();

        assertThat(status.connected()).isTrue();
        assertThat(status.concreteIndex()).isEqualTo(ALIAS);
        assertThat(status.documentCount()).isEqualTo(DOCUMENTS);
    }

    @Test
    @DisplayName("si no existe, el código es EFX-ES-002")
    void shouldReportMissingWhenNothingHasThatName() {
        CatalogStatus status = service(new CatalogName.Missing(ALIAS), 0, false).status();

        assertThat(status.connected()).isFalse();
        assertThat(status.documentCount()).isNull();
        assertThat(status.errorCode()).isEqualTo("EFX-ES-002");
    }

    @Test
    @DisplayName("un alias con varios índices es EFX-ES-005")
    void shouldReportAmbiguousWhenTheAliasPointsAtSeveralIndexes() {
        CatalogStatus status = service(new CatalogName.Ambiguous(ALIAS), 0, false).status();

        assertThat(status.errorCode()).isEqualTo("EFX-ES-005");
        assertThat(status.connected()).isFalse();
    }

    @Test
    @DisplayName("si el cliente no responde, el código es EFX-ES-001")
    void shouldReportUnavailableWhenTheClientFails() {
        CatalogStatus status = service(new CatalogName.Missing(ALIAS), 0, true).status();

        assertThat(status.errorCode()).isEqualTo("EFX-ES-001");
        assertThat(status.documentCount()).isNull();
    }

    private static CatalogStatusService service(CatalogName described, long documents, boolean fail) {
        ElastiflixProperties properties = new ElastiflixProperties(
                null, new ElastiflixProperties.Index(ALIAS), null, null, null, null, null);
        return new CatalogStatusService(new FakeCatalog(described, documents, fail), properties);
    }

    private static final class FakeCatalog implements CatalogIndex {

        private final CatalogName described;
        private final long documents;
        private final boolean fail;

        private FakeCatalog(CatalogName described, long documents, boolean fail) {
            this.described = described;
            this.documents = documents;
            this.fail = fail;
        }

        @Override
        public CatalogName describe(String name) {
            if (fail) {
                throw new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE);
            }
            return described;
        }

        @Override
        public long count(String name) {
            return documents;
        }

        @Override
        public void createWithMapping(String indexName) {
            unused();
        }

        @Override
        public void reindex(String source, String target, BooleanSupplier stop) {
            unused();
        }

        @Override
        public void refresh(String name) {
            unused();
        }

        @Override
        public void publish(String alias, String newIndex, String previousIndex) {
            unused();
        }

        @Override
        public void deleteIndex(String indexName) {
            unused();
        }

        @Override
        public List<String> listOwnedIndexes(String alias) {
            unused();
            return List.of();
        }

        @Override
        public List<Movie> readWithVectors(String name, List<String> ids) {
            unused();
            return List.of();
        }

        @Override
        public List<Movie> readAllWithVectors(String name, int limit) {
            unused();
            return List.of();
        }

        @Override
        public java.util.Set<String> existingIds(String name, List<String> ids) {
            unused();
            return java.util.Set.of();
        }

        @Override
        public void writeAll(String indexName, List<Movie> movies) {
            unused();
        }

        @Override
        public void indexOne(String name, Movie movie) {
            unused();
        }

        @Override
        public Optional<Movie> findById(String name, String id) {
            unused();
            return Optional.empty();
        }

        private static void unused() {
            throw new UnsupportedOperationException("el estado no usa esta operación");
        }
    }
}
