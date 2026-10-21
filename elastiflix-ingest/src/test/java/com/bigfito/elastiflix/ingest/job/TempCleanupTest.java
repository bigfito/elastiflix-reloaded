package com.bigfito.elastiflix.ingest.job;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.catalog.CatalogName;
import com.bigfito.elastiflix.core.catalog.IndexNames;
import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.embedding.EmbeddingService;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.vector.PlotVectorizer;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La limpieza antes de la copia: un {@code create} dudoso intenta borrar el temporal, y un huérfano que no se
 * puede borrar no detiene la importación.
 */
@ExtendWith(MockitoExtension.class)
class TempCleanupTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T03:04:05Z"), ZoneOffset.UTC);
    private static final String ALIAS = "elastiflix-it-x7k2";
    private static final String LIVE = "elastiflix-it-x7k2-20261018-000000";
    private static final String ORPHAN = "elastiflix-it-x7k2-20261019-010101";
    private static final String OTHER_ORPHAN = "elastiflix-it-x7k2-20261019-020202";

    @Mock
    private CatalogIndex catalog;

    @Mock
    private EmbeddingService embeddings;

    private final String temp = new IndexNames().timestamped(ALIAS, CLOCK);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private ImportJob job;

    @BeforeEach
    void setUp() {
        job = new ImportJob(catalog, new IndexNames(), CLOCK, new PlotVectorizer(embeddings),
                new OrphanCleaner(new IndexNames()));
        logs.start();
        cleanerLogger().addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        cleanerLogger().detachAppender(logs);
    }

    @Test
    @DisplayName("un create que se corta (timeout) intenta borrar el temporal, porque pudo crearse")
    void shouldDeleteTheTempWhenCreateTimesOut() {
        // given
        aliasOnLive(List.of());
        doThrow(new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE, new IOException("timeout")))
                .when(catalog).createWithMapping(temp);

        // when
        ElastiflixException thrown = runExpectingFailure();

        // then
        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.ELASTICSEARCH_UNAVAILABLE);
        verify(catalog).deleteIndex(temp);
        verify(catalog, never()).reindex(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("un create con 5xx también intenta borrar el temporal")
    void shouldDeleteTheTempWhenCreateGetsAServerError() {
        // given
        aliasOnLive(List.of());
        doThrow(new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE, response(503)))
                .when(catalog).createWithMapping(temp);

        // when
        runExpectingFailure();

        // then
        verify(catalog).deleteIndex(temp);
    }

    @Test
    @DisplayName("un create rechazado con 4xx no creó nada y no borra ningún índice")
    void shouldNotDeleteAnythingWhenCreateIsRejected() {
        // given
        aliasOnLive(List.of());
        doThrow(new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_REJECTED, response(400)))
                .when(catalog).createWithMapping(temp);

        // when
        runExpectingFailure();

        // then
        verify(catalog, never()).deleteIndex(anyString());
    }

    @Test
    @DisplayName("si un huérfano no se puede borrar, queda un WARN, se borran los demás y la importación publica")
    void shouldWarnAndGoOnWhenAnOrphanCannotBeDeleted() {
        // given
        aliasOnLive(List.of(ORPHAN, OTHER_ORPHAN));
        doThrow(new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE))
                .when(catalog).deleteIndex(ORPHAN);
        when(catalog.count(temp)).thenReturn(1L);
        when(embeddings.embedDocuments(anyList())).thenReturn(List.of(vector()));

        // when
        job.run(ALIAS, new ListMovieSource(List.of(movie())), new ImportCounts(1, 100), notice -> { }, () -> false);

        // then
        verify(catalog).deleteIndex(OTHER_ORPHAN);
        verify(catalog).writeAll(eq(temp), anyList());
        verify(catalog).publish(ALIAS, temp, LIVE);
        assertThat(logs.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).contains("no se pudo borrar el índice huérfano " + ORPHAN);
        });
    }

    private void aliasOnLive(List<String> owned) {
        when(catalog.describe(ALIAS)).thenReturn(new CatalogName.Alias(ALIAS, LIVE));
        when(catalog.listOwnedIndexes(ALIAS)).thenReturn(owned);
    }

    private ElastiflixException runExpectingFailure() {
        ElastiflixException thrown = catchThrowableOfType(ElastiflixException.class, () -> job.run(
                ALIAS, new ListMovieSource(List.of(movie())), new ImportCounts(1, 100), notice -> { }, () -> false));
        assertThat(thrown).as("la importación tenía que parar con un código EFX").isNotNull();
        return thrown;
    }

    private static ElasticsearchException response(int status) {
        return new ElasticsearchException("indices.create", ErrorResponse.of(error -> error
                .status(status).error(cause -> cause.type("prueba").reason("respuesta simulada"))));
    }

    private static Logger cleanerLogger() {
        return (Logger) LoggerFactory.getLogger(OrphanCleaner.class);
    }

    private static Movie movie() {
        return MovieJsonMapper.create().readValue("{\"id\":\"1\",\"title\":\"T\",\"plot\":\"texto\"}", Movie.class);
    }

    private static float[] vector() {
        float[] values = new float[1024];
        values[0] = 1f;
        return values;
    }
}
