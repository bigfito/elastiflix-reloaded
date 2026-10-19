package com.bigfito.elastiflix.core.catalog;

import java.time.Clock;
import java.util.Optional;
import java.util.function.Function;

import com.bigfito.elastiflix.core.model.Movie;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import co.elastic.clients.util.ObjectBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Las peticiones que arma la puerta del catálogo, vistas con el cliente simulado y sin red.
 */
class ElasticsearchCatalogIndexTest {

    private final ElasticsearchClient client = mock(ElasticsearchClient.class);
    private final ElasticsearchIndicesClient indices = mock(ElasticsearchIndicesClient.class);
    private final ElasticsearchCatalogIndex catalog =
            new ElasticsearchCatalogIndex(client, null, new IndexNames(), Clock.systemUTC());

    @Test
    @DisplayName("publish manda timeout 10s y las dos acciones en una sola llamada a _aliases")
    // Mockito entrega la función del cliente sin el tipo genérico.
    @SuppressWarnings("unchecked")
    void shouldPublishWithATenSecondTimeoutInOneCall() throws Exception {
        // given
        when(client.indices()).thenReturn(indices);
        ArgumentCaptor<Function<UpdateAliasesRequest.Builder, ObjectBuilder<UpdateAliasesRequest>>> captor =
                ArgumentCaptor.forClass(Function.class);

        // when
        catalog.publish("demo", "demo-20261020-153000", "demo-20261019-120000");

        // then
        verify(indices).updateAliases(captor.capture());
        UpdateAliasesRequest sent = captor.getValue().apply(new UpdateAliasesRequest.Builder()).build();
        assertThat(sent.timeout().time()).isEqualTo("10s");
        assertThat(sent.masterTimeout()).isNull();
        assertThat(sent.actions()).hasSize(2);
        assertThat(sent.actions().get(0).add().alias()).isEqualTo("demo");
        assertThat(sent.actions().get(0).add().index()).isEqualTo("demo-20261020-153000");
        assertThat(sent.actions().get(1).removeIndex().index()).isEqualTo("demo-20261019-120000");
    }

    @Test
    @DisplayName("indexOne escribe por el alias con require_alias=true y refresh=wait_for")
    // Mockito entrega la función del cliente sin el tipo genérico.
    @SuppressWarnings("unchecked")
    void shouldIndexOneMovieOnlyThroughTheAlias() throws Exception {
        // given
        ArgumentCaptor<Function<IndexRequest.Builder<Movie>, ObjectBuilder<IndexRequest<Movie>>>> captor =
                ArgumentCaptor.forClass(Function.class);
        when(client.index(any(Function.class))).thenReturn(null);

        // when
        catalog.indexOne("demo", Movie.identified("603"));

        // then
        verify(client).index(captor.capture());
        IndexRequest<Movie> sent = captor.getValue().apply(new IndexRequest.Builder<>()).build();
        assertThat(sent.index()).isEqualTo("demo");
        assertThat(sent.id()).isEqualTo("603");
        assertThat(sent.requireAlias()).isTrue();
        assertThat(sent.refresh()).isEqualTo(Refresh.WaitFor);
    }

    @Test
    @DisplayName("findById pide el documento excluyendo los tres vectores")
    // Mockito entrega la función del cliente sin el tipo genérico.
    @SuppressWarnings("unchecked")
    void shouldFindMovieByIdExcludingVectors() throws Exception {
        // given
        GetResponse<Movie> response = mock(GetResponse.class);
        when(response.found()).thenReturn(true);
        when(response.source()).thenReturn(Movie.identified("603"));
        when(client.get(any(Function.class), eq(Movie.class))).thenReturn(response);
        ArgumentCaptor<Function<GetRequest.Builder, ObjectBuilder<GetRequest>>> captor =
                ArgumentCaptor.forClass(Function.class);

        // when
        Optional<Movie> found = catalog.findById("demo", "603");

        // then
        assertThat(found).contains(Movie.identified("603"));
        verify(client).get(captor.capture(), eq(Movie.class));
        GetRequest sent = captor.getValue().apply(new GetRequest.Builder()).build();
        assertThat(sent.index()).isEqualTo("demo");
        assertThat(sent.id()).isEqualTo("603");
        assertThat(sent.sourceExcludes()).containsExactly("plot_256", "plot_512", "plot_1024");
    }

    @Test
    @DisplayName("findById devuelve vacío si el documento no existe")
    // Mockito entrega la función del cliente sin el tipo genérico.
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyWhenDocumentDoesNotExist() throws Exception {
        // given
        GetResponse<Movie> response = mock(GetResponse.class);
        when(response.found()).thenReturn(false);
        when(client.get(any(Function.class), eq(Movie.class))).thenReturn(response);

        // when
        Optional<Movie> found = catalog.findById("demo", "999999");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findById devuelve vacío sin llamar a Elasticsearch si el id viene en blanco")
    void shouldReturnEmptyWhenIdIsBlank() {
        assertThat(catalog.findById("demo", null)).isEmpty();
        assertThat(catalog.findById("demo", "")).isEmpty();
        assertThat(catalog.findById("demo", "   ")).isEmpty();
    }

    @Test
    @DisplayName("findById devuelve vacío si Elasticsearch responde 404")
    // Mockito entrega la función del cliente sin el tipo genérico.
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyWhenElasticsearchThrows404() throws Exception {
        // given
        ElasticsearchException notFound = new ElasticsearchException("get", ErrorResponse.of(response -> response
                .status(404).error(cause -> cause.type("index_not_found_exception").reason("no existe"))));
        when(client.get(any(Function.class), eq(Movie.class))).thenThrow(notFound);

        // when
        Optional<Movie> found = catalog.findById("demo", "603");

        // then
        assertThat(found).isEmpty();
    }
}
