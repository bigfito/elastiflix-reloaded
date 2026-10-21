package com.bigfito.elastiflix.ingest.job;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.catalog.CatalogName;
import com.bigfito.elastiflix.core.catalog.IndexNames;
import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.embedding.EmbeddingService;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.vector.PlotVectorizer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ramas de {@link ImportJob} que {@code ImportJobTest} no cubre: la copia que no cuadra, un nombre que no
 * es un alias de un índice, varios lotes, cancelar entre lotes o justo antes de publicar, y un temporal
 * que no se puede borrar.
 */
@ExtendWith(MockitoExtension.class)
class ImportJobEdgeTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T03:04:05Z"), ZoneOffset.UTC);
    private static final String ALIAS = "elastiflix-it-x7k2";
    private static final String LIVE = "elastiflix-it-x7k2-20261018-000000";

    @Mock
    private CatalogIndex catalog;

    @Mock
    private EmbeddingService embeddings;

    private final String temp = new IndexNames().timestamped(ALIAS, CLOCK);
    private final List<ImportProgress> progress = new ArrayList<>();
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private ImportJob job;

    @BeforeEach
    void setUp() {
        job = new ImportJob(catalog, new IndexNames(), CLOCK, new PlotVectorizer(embeddings),
                new OrphanCleaner(new IndexNames()));
        logs.start();
        jobLogger().addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        jobLogger().detachAppender(logs);
    }

    @Test
    @DisplayName("un alias que no existe da EFX-ES-002 sin crear ni borrar índices")
    void shouldRefuseAMissingCatalog() {
        // given
        when(catalog.describe(ALIAS)).thenReturn(new CatalogName.Missing(ALIAS));

        // when
        ElastiflixException thrown = run(movies(1), 100, () -> false);

        // then
        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.CATALOG_NOT_FOUND);
        verify(catalog, never()).createWithMapping(anyString());
        verify(catalog, never()).deleteIndex(anyString());
    }

    @Test
    @DisplayName("un alias en varios índices da EFX-ES-005 sin crear ni borrar índices")
    void shouldRefuseAnAmbiguousAlias() {
        // given
        when(catalog.describe(ALIAS)).thenReturn(new CatalogName.Ambiguous(ALIAS));

        // when
        ElastiflixException thrown = run(movies(1), 100, () -> false);

        // then
        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.ALIAS_AMBIGUOUS);
        verify(catalog, never()).createWithMapping(anyString());
        verify(catalog, never()).deleteIndex(anyString());
    }

    @Test
    @DisplayName("5 películas en lotes de 2 se escriben en 3 lotes y se publican")
    void shouldWriteSeveralBatches() {
        // given
        aliasOnLive();
        when(catalog.count(temp)).thenReturn(5L);
        when(embeddings.embedDocuments(anyList())).thenAnswer(invocation -> vectors(invocation.getArgument(0)));

        // when
        job.run(ALIAS, new ListMovieSource(movies(5)), new ImportCounts(5, 2), progress::add, () -> false);

        // then
        verify(catalog, times(3)).writeAll(eq(temp), anyList());
        verify(catalog).publish(ALIAS, temp, LIVE);
        assertThat(progress).last().extracting(ImportProgress::step, ImportProgress::batch, ImportProgress::written)
                .containsExactly(5, 3, 5);
    }

    @Test
    @DisplayName("cancelar entre dos lotes para antes del segundo, borra el temporal y no publica")
    void shouldStopBetweenBatchesWhenCancelled() {
        // given
        aliasOnLive();
        when(embeddings.embedDocuments(anyList())).thenAnswer(invocation -> vectors(invocation.getArgument(0)));
        AtomicBoolean cancel = new AtomicBoolean();
        doAnswer(invocation -> {
            cancel.set(true);
            return null;
        }).when(catalog).writeAll(eq(temp), anyList());

        // when
        ElastiflixException thrown = run(movies(5), 2, cancel::get);

        // then
        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.IMPORT_CANCELLED);
        verify(catalog, times(1)).writeAll(eq(temp), anyList());
        verify(catalog).deleteIndex(temp);
        verify(catalog, never()).publish(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("cancelar después de contar y justo antes de publicar borra el temporal y no publica")
    void shouldStopRightBeforeTheSwapWhenCancelled() {
        // given
        aliasOnLive();
        AtomicBoolean cancel = new AtomicBoolean();
        when(catalog.count(temp)).thenAnswer(invocation -> {
            cancel.set(true);
            return 1L;
        });
        when(embeddings.embedDocuments(anyList())).thenAnswer(invocation -> vectors(invocation.getArgument(0)));

        // when
        ElastiflixException thrown = run(movies(1), 100, cancel::get);

        // then
        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.IMPORT_CANCELLED);
        verify(catalog).writeAll(eq(temp), anyList());
        verify(catalog).deleteIndex(temp);
        verify(catalog, never()).publish(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("si el temporal no se puede borrar, sale el error original y un WARN con el nombre del temporal")
    void shouldWarnAndKeepTheOriginalErrorWhenTheTempCannotBeDeleted() {
        // given
        aliasOnLive();
        when(embeddings.embedDocuments(anyList())).thenAnswer(invocation -> vectors(invocation.getArgument(0)));
        doThrow(new ImportException(ErrorCode.BULK_REJECTED)).when(catalog).writeAll(eq(temp), anyList());
        doThrow(new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE))
                .when(catalog).deleteIndex(temp);

        // when
        ElastiflixException thrown = run(movies(1), 100, () -> false);

        // then
        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.BULK_REJECTED);
        verify(catalog).deleteIndex(temp);
        assertThat(logs.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).contains("No se pudo borrar el índice temporal " + temp);
        });
    }

    private void aliasOnLive() {
        when(catalog.describe(ALIAS)).thenReturn(new CatalogName.Alias(ALIAS, LIVE));
        when(catalog.listOwnedIndexes(ALIAS)).thenReturn(List.of());
    }

    private ElastiflixException run(List<Movie> movies, int batchSize, BooleanSupplier stop) {
        ImportCounts counts = new ImportCounts(movies.size(), batchSize);
        ElastiflixException thrown = catchThrowableOfType(ElastiflixException.class,
                () -> job.run(ALIAS, new ListMovieSource(movies), counts, progress::add, stop));
        assertThat(thrown).as("la importación tenía que parar con un código EFX").isNotNull();
        return thrown;
    }

    private static Logger jobLogger() {
        return (Logger) LoggerFactory.getLogger(ImportJob.class);
    }

    private static List<Movie> movies(int count) {
        List<Movie> movies = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            String json = "{\"id\":\"" + index + "\",\"title\":\"T\",\"plot\":\"texto\"}";
            movies.add(MovieJsonMapper.create().readValue(json, Movie.class));
        }
        return movies;
    }

    private static List<float[]> vectors(List<String> texts) {
        float[] values = new float[1024];
        values[0] = 1f;
        return Collections.nCopies(texts.size(), values);
    }
}
