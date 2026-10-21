package com.bigfito.elastiflix.ingest.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import com.bigfito.elastiflix.core.catalog.AppendResult;
import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.catalog.CatalogName;
import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.embedding.EmbeddingService;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.vector.PlotVectorizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Append escribe por el alias, sin índice temporal, y lo escrito se queda aunque la carga pare.
 */
@ExtendWith(MockitoExtension.class)
class AppendJobTest {

    private static final String ALIAS = "elastiflix-it-x7k2";
    private static final String LIVE = "elastiflix-it-x7k2-20261018-000000";

    @Mock
    private CatalogIndex catalog;

    @Mock
    private EmbeddingService embeddings;

    private final List<ImportProgress> progress = new ArrayList<>();
    private AppendJob job;

    @BeforeEach
    void setUp() {
        job = new AppendJob(catalog, new PlotVectorizer(embeddings));
    }

    @Test
    @DisplayName("5 películas en lotes de 2 van por el alias en 3 lotes, cuenta reemplazos y hace refresh al final")
    void shouldAppendEveryBatchThroughTheAlias() {
        // given
        aliasOnLive();
        vectorsForEveryMovie();
        when(catalog.existingIds(eq(ALIAS), anyList())).thenReturn(Set.of("1"), Set.of(), Set.of());
        when(catalog.appendAll(eq(ALIAS), anyList()))
                .thenAnswer(invocation -> new AppendResult(invocation.<List<Movie>>getArgument(1).size(), 0));

        // when
        job.run(ALIAS, new ListMovieSource(movies(5)), new ImportCounts(5, 2), progress::add, () -> false);

        // then
        verify(catalog, times(3)).appendAll(eq(ALIAS), anyList());
        InOrder order = inOrder(catalog);
        order.verify(catalog, times(3)).appendAll(eq(ALIAS), anyList());
        order.verify(catalog).refresh(ALIAS);
        verify(catalog, never()).createWithMapping(anyString());
        verify(catalog, never()).writeAll(anyString(), anyList());
        verify(catalog, never()).publish(anyString(), anyString(), anyString());
        verify(catalog, never()).deleteIndex(anyString());
        assertThat(progress).last()
                .extracting(ImportProgress::step, ImportProgress::batch, ImportProgress::written,
                        ImportProgress::tempIndex)
                .containsExactly(4, 3, 5, "");
    }

    @Test
    @DisplayName("un lote con rechazos da EFX-IMP-008, cuenta los aceptados de ese lote y no sigue")
    void shouldStopWithTheAcceptedCountWhenABatchIsRejected() {
        // given
        aliasOnLive();
        vectorsForEveryMovie();
        when(catalog.appendAll(eq(ALIAS), anyList()))
                .thenReturn(new AppendResult(2, 0))
                .thenReturn(new AppendResult(1, 1));

        // when
        ElastiflixException thrown = run(movies(5), 2, () -> false);

        // then
        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.APPEND_BATCH_REJECTED);
        verify(catalog, times(2)).appendAll(eq(ALIAS), anyList());
        verify(catalog).refresh(ALIAS);
        verify(catalog, never()).deleteIndex(anyString());
        assertThat(progress).last().extracting(ImportProgress::written).isEqualTo(3);
    }

    @Test
    @DisplayName("cancelar después del primer lote da EFX-IMP-009, deja lo escrito y no escribe el segundo")
    void shouldKeepWhatWasWrittenWhenCancelledBetweenBatches() {
        // given
        aliasOnLive();
        vectorsForEveryMovie();
        AtomicBoolean cancel = new AtomicBoolean();
        doAnswer(invocation -> {
            cancel.set(true);
            return new AppendResult(2, 0);
        }).when(catalog).appendAll(eq(ALIAS), anyList());

        // when
        ElastiflixException thrown = run(movies(5), 2, cancel::get);

        // then
        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.APPEND_CANCELLED);
        verify(catalog, times(1)).appendAll(eq(ALIAS), anyList());
        verify(catalog).refresh(ALIAS);
        verify(catalog, never()).deleteIndex(anyString());
        assertThat(progress).last().extracting(ImportProgress::written).isEqualTo(2);
    }

    @Test
    @DisplayName("si Elasticsearch falla en un lote, sale su código y lo escrito antes se queda")
    void shouldKeepTheEarlierBatchesWhenElasticsearchFails() {
        // given
        aliasOnLive();
        vectorsForEveryMovie();
        when(catalog.appendAll(eq(ALIAS), anyList()))
                .thenReturn(new AppendResult(2, 0))
                .thenThrow(new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE));

        // when
        ElastiflixException thrown = run(movies(5), 2, () -> false);

        // then
        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.ELASTICSEARCH_UNAVAILABLE);
        verify(catalog).refresh(ALIAS);
        assertThat(progress).last().extracting(ImportProgress::written).isEqualTo(2);
    }

    @Test
    @DisplayName("si el refresh final falla, la carga igual termina: los lotes ya quedaron escritos")
    void shouldFinishWhenTheFinalRefreshFails() {
        // given
        aliasOnLive();
        vectorsForEveryMovie();
        when(catalog.appendAll(eq(ALIAS), anyList())).thenReturn(new AppendResult(1, 0));
        doThrow(new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE)).when(catalog).refresh(ALIAS);

        // when
        job.run(ALIAS, new ListMovieSource(movies(1)), new ImportCounts(1, 100), progress::add, () -> false);

        // then
        assertThat(progress).last().extracting(ImportProgress::written).isEqualTo(1);
    }

    @Test
    @DisplayName("un alias que no existe, en varios índices o un índice concreto dan su código sin escribir nada")
    void shouldRefuseWhatIsNotAnAliasOfOneIndex() {
        assertCatalogRefused(new CatalogName.Missing(ALIAS), ErrorCode.CATALOG_NOT_FOUND);
        assertCatalogRefused(new CatalogName.Ambiguous(ALIAS), ErrorCode.ALIAS_AMBIGUOUS);
        assertCatalogRefused(new CatalogName.ConcreteIndex(ALIAS), ErrorCode.CATALOG_NOT_ALIAS);
    }

    @Test
    @DisplayName("cancelar antes de empezar no toca el catálogo")
    void shouldLeaveTheCatalogUntouchedWhenCancelledFirst() {
        ElastiflixException thrown = run(movies(1), 100, () -> true);

        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.APPEND_CANCELLED);
        verifyNoInteractions(catalog);
    }

    private void assertCatalogRefused(CatalogName described, ErrorCode expected) {
        when(catalog.describe(ALIAS)).thenReturn(described);

        ElastiflixException thrown = run(movies(1), 100, () -> false);

        assertThat(thrown.errorCode()).isEqualTo(expected);
        verify(catalog, never()).appendAll(anyString(), anyList());
        verify(catalog, never()).refresh(anyString());
    }

    private ElastiflixException run(List<Movie> movies, int batchSize, BooleanSupplier cancelled) {
        return catchThrowableOfType(ElastiflixException.class, () -> job.run(ALIAS, new ListMovieSource(movies),
                new ImportCounts(movies.size(), batchSize), progress::add, cancelled));
    }

    private void aliasOnLive() {
        when(catalog.describe(ALIAS)).thenReturn(new CatalogName.Alias(ALIAS, LIVE));
    }

    private void vectorsForEveryMovie() {
        when(embeddings.embedDocuments(anyList())).thenAnswer(invocation -> {
            List<float[]> vectors = new ArrayList<>();
            for (int index = 0; index < invocation.<List<String>>getArgument(0).size(); index++) {
                float[] values = new float[1024];
                values[0] = 1f;
                vectors.add(values);
            }
            return vectors;
        });
    }

    private static List<Movie> movies(int count) {
        List<Movie> movies = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            String json = "{\"id\":\"" + index + "\",\"title\":\"T\",\"plot\":\"texto\"}";
            movies.add(MovieJsonMapper.create().readValue(json, Movie.class));
        }
        return movies;
    }
}
