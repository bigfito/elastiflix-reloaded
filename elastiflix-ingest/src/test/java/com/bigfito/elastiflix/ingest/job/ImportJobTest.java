package com.bigfito.elastiflix.ingest.job;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.catalog.CatalogName;
import com.bigfito.elastiflix.core.catalog.IndexNames;
import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.embedding.EmbeddingService;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.EmbeddingException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.vector.PlotVectorizer;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
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
 * El índice anterior se borra solo en la publicación. Un fallo anterior borra el temporal.
 * Si publicar no confirma, se lee el alias antes de borrar.
 */
@ExtendWith(MockitoExtension.class)
class ImportJobTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T03:04:05Z"), ZoneOffset.UTC);
    private static final String ALIAS = "elastiflix-it-x7k2";
    private static final String LIVE = "elastiflix-it-x7k2-20261018-000000";
    private static final String ORPHAN = "elastiflix-it-x7k2-20261019-010101";
    private static final String FOREIGN = "elastiflix-reloaded-20261020-153000";
    private static final ImportCounts ONE_NEW = new ImportCounts(1, 100);

    @Mock
    private CatalogIndex catalog;

    @Mock
    private EmbeddingService embeddings;

    private final String temp = new IndexNames().timestamped(ALIAS, CLOCK);
    private final List<ImportProgress> progress = new ArrayList<>();
    private ImportJob job;

    @BeforeEach
    void setUp() {
        job = new ImportJob(
                catalog,
                new IndexNames(),
                CLOCK,
                new PlotVectorizer(embeddings),
                new OrphanCleaner(new IndexNames()));
    }

    @Test
    @DisplayName("publica en una llamada y no borra el catálogo real ni el índice vigente")
    void shouldPublishAndDeleteOnlyTheOrphan() {
        when(catalog.describe(ALIAS)).thenReturn(new CatalogName.Alias(ALIAS, LIVE));
        when(catalog.listOwnedIndexes(ALIAS)).thenReturn(List.of(FOREIGN, LIVE, ORPHAN));
        when(catalog.count(temp)).thenReturn(4L, 5L);
        when(catalog.count(LIVE)).thenReturn(4L);
        when(embeddings.embedDocuments(anyList())).thenReturn(List.of(vector()));

        job.run(ALIAS, new ListMovieSource(List.of(movie("1"))), ONE_NEW, progress::add, () -> false);

        verify(catalog).createWithMapping(temp);
        verify(catalog).writeAll(eq(temp), anyList());
        verify(catalog).publish(ALIAS, temp, LIVE);
        verify(catalog).deleteIndex(ORPHAN);
        verify(catalog, never()).deleteIndex(FOREIGN);
        verify(catalog, never()).deleteIndex(LIVE);
        verify(catalog, never()).deleteIndex(temp);
        assertThat(progress).extracting(ImportProgress::step).containsExactly(4, 4, 4, 5);
        assertThat(progress).extracting(ImportProgress::tempIndex).containsOnly(temp);
    }

    @Test
    @DisplayName("el reemplazo se cuenta en la copia y el aviso sale antes de escribir")
    void shouldCountReplacementsOnTheCopyBeforeWriting() {
        when(catalog.describe(ALIAS)).thenReturn(new CatalogName.Alias(ALIAS, LIVE));
        when(catalog.listOwnedIndexes(ALIAS)).thenReturn(List.of());
        when(catalog.existingIds(eq(temp), anyList())).thenReturn(Set.of("1"));
        when(catalog.count(temp)).thenReturn(4L, 4L);
        when(catalog.count(LIVE)).thenReturn(4L);
        when(embeddings.embedDocuments(anyList())).thenReturn(List.of(vector()));
        doAnswer(invocation -> {
            // Al escribir el lote ya se avisó que está vectorizado, pero todavía no que está escrito.
            assertThat(progress).last()
                    .extracting(ImportProgress::vectorized, ImportProgress::written)
                    .containsExactly(1, 0);
            return null;
        }).when(catalog).writeAll(eq(temp), anyList());

        job.run(ALIAS, new ListMovieSource(List.of(movie("1"))), ONE_NEW, progress::add, () -> false);

        verify(catalog).publish(ALIAS, temp, LIVE);
        assertThat(progress).filteredOn(notice -> notice.step() == 4).last()
                .extracting(ImportProgress::vectorized, ImportProgress::written)
                .containsExactly(1, 1);
    }

    @Test
    @DisplayName("un lote rechazado borra el temporal y no publica")
    void shouldRollBackWhenABatchIsRejected() {
        readyToCopy();
        when(embeddings.embedDocuments(anyList())).thenReturn(List.of(vector()));
        doThrow(new ImportException(ErrorCode.BULK_REJECTED)).when(catalog).writeAll(anyString(), anyList());

        assertThatThrownBy(() -> runOne()).isInstanceOf(ImportException.class)
                .extracting(error -> ((ImportException) error).errorCode())
                .isEqualTo(ErrorCode.BULK_REJECTED);
        verify(catalog).deleteIndex(temp);
        verify(catalog, never()).publish(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("un conteo que no cuadra no publica")
    void shouldRollBackWhenTheCountDoesNotMatch() {
        when(catalog.describe(ALIAS)).thenReturn(new CatalogName.Alias(ALIAS, LIVE));
        when(catalog.listOwnedIndexes(ALIAS)).thenReturn(List.of());
        when(catalog.count(temp)).thenReturn(4L, 99L);
        when(catalog.count(LIVE)).thenReturn(4L);
        when(embeddings.embedDocuments(anyList())).thenReturn(List.of(vector()));

        assertThatThrownBy(() -> runOne()).isInstanceOf(ImportException.class)
                .extracting(error -> ((ImportException) error).errorCode())
                .isEqualTo(ErrorCode.IMPORT_COUNT_MISMATCH);
        verify(catalog).deleteIndex(temp);
        verify(catalog, never()).publish(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("un fallo de inferencia no publica")
    void shouldRollBackWhenInferenceFails() {
        readyToCopy();
        when(embeddings.embedDocuments(anyList()))
                .thenThrow(new EmbeddingException(ErrorCode.INFERENCE_UNAVAILABLE));

        assertThatThrownBy(() -> runOne()).isInstanceOf(EmbeddingException.class);
        verify(catalog).deleteIndex(temp);
        verify(catalog, never()).publish(anyString(), anyString(), anyString());
        verify(catalog, never()).writeAll(anyString(), anyList());
    }

    @Test
    @DisplayName("cancelar al volver de la copia borra el temporal")
    void shouldRollBackWhenCancelledAfterReindex() {
        when(catalog.describe(ALIAS)).thenReturn(new CatalogName.Alias(ALIAS, LIVE));
        when(catalog.listOwnedIndexes(ALIAS)).thenReturn(List.of());
        AtomicBoolean cancel = new AtomicBoolean();
        doAnswer(invocation -> {
            cancel.set(true);
            return null;
        }).when(catalog).reindex(anyString(), anyString(), any());

        assertThatThrownBy(() -> job.run(
                ALIAS, new ListMovieSource(List.of(movie("1"))), ONE_NEW, progress::add, cancel::get))
                .isInstanceOf(ImportException.class)
                .extracting(error -> ((ImportException) error).errorCode())
                .isEqualTo(ErrorCode.IMPORT_CANCELLED);
        verify(catalog).deleteIndex(temp);
        verify(catalog, never()).publish(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("si la copia falla (EFX-ES-006), el temporal se borra una sola vez y después de reindex")
    void shouldCancelTheCopyBeforeDeletingTheTemp() {
        // given
        when(catalog.describe(ALIAS)).thenReturn(new CatalogName.Alias(ALIAS, LIVE));
        when(catalog.listOwnedIndexes(ALIAS)).thenReturn(List.of());
        // ElasticsearchCatalogIndex cancela la tarea de _reindex antes de lanzar (ver ReindexTasksTest);
        // aquí se comprueba que ImportJob borra el temporal solo cuando reindex ya volvió.
        doThrow(new ElasticsearchAccessException(ErrorCode.REINDEX_FAILED))
                .when(catalog).reindex(eq(LIVE), eq(temp), any());

        // when
        Throwable thrown = catchThrowable(() -> runOne());

        // then
        assertThat(thrown).isInstanceOf(ElasticsearchAccessException.class)
                .extracting(error -> ((ElasticsearchAccessException) error).errorCode())
                .isEqualTo(ErrorCode.REINDEX_FAILED);
        InOrder order = inOrder(catalog);
        order.verify(catalog).reindex(eq(LIVE), eq(temp), any());
        order.verify(catalog).deleteIndex(temp);
        verify(catalog, times(1)).deleteIndex(anyString());
        verify(catalog, never()).publish(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("cancelar antes de crear el temporal no borra nada")
    void shouldLeaveTheClusterUntouchedWhenCancelledFirst() {
        assertThatThrownBy(() -> job.run(ALIAS, new ListMovieSource(List.of()), ONE_NEW, progress::add, () -> true))
                .isInstanceOf(ImportException.class);
        verifyNoInteractions(catalog);
    }

    @Test
    @DisplayName("si publicar falla y el alias ya apunta al temporal, queda publicado")
    void shouldTreatThePublishAsDoneWhenTheAliasPointsAtTheTemp() {
        reachPublish(new CatalogName.Alias(ALIAS, temp), new IOException("corte"));

        job.run(ALIAS, new ListMovieSource(List.of(movie("1"))), ONE_NEW, progress::add, () -> false);

        verify(catalog, never()).deleteIndex(temp);
        assertThat(progress).extracting(ImportProgress::step).containsExactly(4, 4, 4, 5);
    }

    @Test
    @DisplayName("si publicar se corta (IOException) y el alias sigue en el anterior, no borra y da EFX-IMP-006")
    void shouldKeepTheTempWhenPublishIsCutOff() {
        reachPublish(new CatalogName.Alias(ALIAS, LIVE), new IOException("socket timeout"));

        assertThatThrownBy(() -> runOne()).isInstanceOf(ImportException.class)
                .extracting(error -> ((ImportException) error).errorCode())
                .isEqualTo(ErrorCode.PUBLISH_UNCERTAIN);
        verify(catalog, never()).deleteIndex(temp);
    }

    @Test
    @DisplayName("un listener que lanza en el paso 5 no borra el temporal y la importación termina publicada")
    void shouldKeepThePublishedTempWhenTheListenerFails() {
        when(catalog.describe(ALIAS)).thenReturn(new CatalogName.Alias(ALIAS, LIVE));
        when(catalog.listOwnedIndexes(ALIAS)).thenReturn(List.of());
        when(catalog.count(temp)).thenReturn(4L, 5L);
        when(catalog.count(LIVE)).thenReturn(4L);
        when(embeddings.embedDocuments(anyList())).thenReturn(List.of(vector()));
        ImportListener failsOnPublish = notice -> {
            if (notice.step() == 5) {
                throw new ImportException(ErrorCode.JOB_FAILED);
            }
        };

        // Sin excepción: ImportRunner deja el trabajo publicado (ver ImportRunnerTest).
        job.run(ALIAS, new ListMovieSource(List.of(movie("1"))), ONE_NEW, failsOnPublish, () -> false);

        verify(catalog).publish(ALIAS, temp, LIVE);
        verify(catalog, never()).deleteIndex(temp);
    }

    @Test
    @DisplayName("una excepción ajena antes de publicar también borra el temporal")
    void shouldDeleteTheTempOnAnUnexpectedFailure() {
        readyToCopy();
        when(embeddings.embedDocuments(anyList())).thenReturn(List.of(vector()));
        doThrow(new IllegalStateException("inesperada")).when(catalog).writeAll(anyString(), anyList());

        assertThatThrownBy(() -> runOne()).isInstanceOf(IllegalStateException.class);
        verify(catalog).deleteIndex(temp);
        verify(catalog, never()).publish(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("si _aliases responde 4xx y el alias sigue en el anterior, borra el temporal")
    void shouldDeleteTheTempWhenTheAliasStaysOnThePreviousIndex() {
        reachPublish(new CatalogName.Alias(ALIAS, LIVE), rejected());

        assertThatThrownBy(() -> runOne()).isInstanceOf(ElasticsearchAccessException.class)
                .extracting(error -> ((ElasticsearchAccessException) error).errorCode())
                .isEqualTo(ErrorCode.ALIAS_PUBLISH_FAILED);
        verify(catalog).deleteIndex(temp);
        assertThat(progress).extracting(ImportProgress::step).containsExactly(4, 4, 4);
    }

    @Test
    @DisplayName("si publicar falla y describe también, no borra el temporal")
    void shouldLeaveTheTempWhenTheAliasCannotBeRead() {
        when(catalog.describe(ALIAS))
                .thenReturn(new CatalogName.Alias(ALIAS, LIVE))
                .thenThrow(new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE));
        when(catalog.listOwnedIndexes(ALIAS)).thenReturn(List.of());
        when(catalog.count(temp)).thenReturn(4L, 5L);
        when(catalog.count(LIVE)).thenReturn(4L);
        when(embeddings.embedDocuments(anyList())).thenReturn(List.of(vector()));
        doThrow(new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE, new IOException("corte")))
                .when(catalog).publish(ALIAS, temp, LIVE);

        assertThatThrownBy(() -> runOne()).isInstanceOf(ImportException.class)
                .extracting(error -> ((ImportException) error).errorCode())
                .isEqualTo(ErrorCode.PUBLISH_UNCERTAIN);
        verify(catalog, never()).deleteIndex(anyString());
    }

    @Test
    @DisplayName("si publicar falla y el alias ya no existe, no borra el temporal")
    void shouldLeaveTheTempWhenTheAliasIsMissing() {
        reachPublish(new CatalogName.Missing(ALIAS), new IOException("corte"));

        assertThatThrownBy(() -> runOne()).isInstanceOf(ImportException.class)
                .extracting(error -> ((ImportException) error).errorCode())
                .isEqualTo(ErrorCode.PUBLISH_UNCERTAIN);
        verify(catalog, never()).deleteIndex(anyString());
    }

    @Test
    @DisplayName("un índice concreto no se publica ni se borra")
    void shouldRefuseAConcreteIndex() {
        when(catalog.describe(ALIAS)).thenReturn(new CatalogName.ConcreteIndex(ALIAS));

        assertThatThrownBy(() -> runOne()).isInstanceOf(ImportException.class)
                .extracting(error -> ((ImportException) error).errorCode())
                .isEqualTo(ErrorCode.CATALOG_NOT_ALIAS);
        verify(catalog, never()).createWithMapping(anyString());
        verify(catalog, never()).deleteIndex(anyString());
    }

    private void readyToCopy() {
        when(catalog.describe(ALIAS)).thenReturn(new CatalogName.Alias(ALIAS, LIVE));
        when(catalog.listOwnedIndexes(ALIAS)).thenReturn(List.of());
        when(catalog.count(temp)).thenReturn(4L);
        when(catalog.count(LIVE)).thenReturn(4L);
    }

    private void reachPublish(CatalogName afterPublish, Throwable cause) {
        when(catalog.describe(ALIAS)).thenReturn(new CatalogName.Alias(ALIAS, LIVE), afterPublish);
        when(catalog.listOwnedIndexes(ALIAS)).thenReturn(List.of());
        when(catalog.count(temp)).thenReturn(4L, 5L);
        when(catalog.count(LIVE)).thenReturn(4L);
        when(embeddings.embedDocuments(anyList())).thenReturn(List.of(vector()));
        doThrow(new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE, cause))
                .when(catalog).publish(ALIAS, temp, LIVE);
    }

    private static ElasticsearchException rejected() {
        return new ElasticsearchException("indices.update_aliases", ErrorResponse.of(response -> response
                .status(400).error(cause -> cause.type("illegal_argument_exception").reason("rechazo"))));
    }

    private void runOne() {
        job.run(ALIAS, new ListMovieSource(List.of(movie("1"))), ONE_NEW, progress::add, () -> false);
    }

    private static Movie movie(String id) {
        String json = "{\"id\":\"" + id + "\",\"title\":\"T\",\"plot\":\"texto\"}";
        return MovieJsonMapper.create().readValue(json, Movie.class);
    }

    private static float[] vector() {
        float[] values = new float[1024];
        values[0] = 1f;
        return values;
    }
}
