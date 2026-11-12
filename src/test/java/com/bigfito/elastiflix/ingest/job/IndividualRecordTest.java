package com.bigfito.elastiflix.ingest.job;

import java.util.List;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.catalog.CatalogName;
import com.bigfito.elastiflix.core.catalog.IndexNames;
import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.embedding.EmbeddingService;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.mapping.MappingValidator;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.vector.PlotVectorizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El registro individual valida, vectoriza y escribe una sola vez en el alias.
 */
@ExtendWith(MockitoExtension.class)
class IndividualRecordTest {

    @Mock
    private EmbeddingService embeddings;

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-22T03:04:05Z"), ZoneOffset.UTC);
    private static final String NEW_INDEX = "demo-20261022-030405";
    private static final String PLOT = "Sinopsis de prueba suficientemente larga para validar.";

    @Mock
    private CatalogIndex catalog;

    @Test
    @DisplayName("un registro válido se vectoriza y se indexa una vez")
    void shouldIndexOnceWhenTheRecordIsValid() {
        when(embeddings.embedDocuments(anyList())).thenReturn(List.of(vector()));
        when(catalog.describe("demo")).thenReturn(new CatalogName.Alias("demo", "demo-20261018-000000"));
        IndividualRecord records = records();

        SaveResult result = records.save("demo", movie("{\"id\":\"7\",\"title\":\"Una\",\"plot\":\"" + PLOT + "\"}"));

        assertThat(result).isInstanceOf(SaveResult.Saved.class);
        verify(catalog, never()).createWithMapping(anyString());
        ArgumentCaptor<Movie> written = ArgumentCaptor.forClass(Movie.class);
        verify(catalog).indexOne(eq("demo"), written.capture());
        verify(embeddings).embedDocuments(List.of(PLOT));
        assertThat(written.getValue().id()).isEqualTo("7");
        assertThat(written.getValue().plot1024()).hasSize(1024);
        assertThat(written.getValue().plot512()).hasSize(512);
        assertThat(written.getValue().plot256()).hasSize(256);
    }

    @Test
    @DisplayName("un título en blanco no escribe")
    void shouldNotIndexWhenTheTitleIsBlank() {
        IndividualRecord records = records();

        SaveResult result = records.save("demo", movie("{\"id\":\"7\",\"title\":\"   \",\"plot\":\"" + PLOT + "\"}"));

        assertThat(result).isInstanceOf(SaveResult.Rejected.class);
        assertThat(((SaveResult.Rejected) result).report().errors())
                .extracting(ValidationIssue::code)
                .contains(ErrorCode.REQUIRED_FIELD.code());
        verify(catalog, never()).indexOne(anyString(), any(Movie.class));
        verify(embeddings, never()).embedDocuments(anyList());
        verify(catalog, never()).describe(anyString());
        verify(catalog, never()).createWithMapping(anyString());
    }

    @Test
    @DisplayName("sin catálogo, crea el índice con el mapping y el alias antes de guardar")
    void shouldCreateTheCatalogBeforeTheFirstRecord() {
        when(embeddings.embedDocuments(anyList())).thenReturn(List.of(vector()));
        when(catalog.describe("demo")).thenReturn(new CatalogName.Missing("demo"));
        IndividualRecord records = records();

        SaveResult result = records.save("demo", movie("{\"id\":\"7\",\"title\":\"Una\",\"plot\":\"" + PLOT + "\"}"));

        assertThat(result).isInstanceOf(SaveResult.Saved.class);
        InOrder order = inOrder(catalog);
        order.verify(catalog).createWithMapping(NEW_INDEX);
        order.verify(catalog).publish("demo", NEW_INDEX, null);
        order.verify(catalog).indexOne(eq("demo"), any(Movie.class));
    }

    private IndividualRecord records() {
        return new IndividualRecord(new MappingValidator(), new PlotVectorizer(embeddings), catalog,
                new FirstCatalog(catalog, new IndexNames(), CLOCK));
    }

    private static Movie movie(String json) {
        return MovieJsonMapper.create().readValue(json, Movie.class);
    }

    private static float[] vector() {
        float[] values = new float[1024];
        values[0] = 1f;
        return values;
    }
}
