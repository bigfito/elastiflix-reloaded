package com.bigfito.elastiflix.core.catalog;

import java.io.IOException;
import java.time.Clock;
import java.util.Optional;
import java.util.function.Function;

import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ErrorCode;
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
import static org.assertj.core.api.Assertions.catchThrowable;
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
    @DisplayName("findById con un alias o índice inexistente (404 index_not_found_exception) falla con EFX-ES-007")
    // Mockito entrega la función del cliente sin el tipo genérico.
    @SuppressWarnings("unchecked")
    void shouldFailWhenTheIndexDoesNotExist() throws Exception {
        // given
        when(client.get(any(Function.class), eq(Movie.class)))
                .thenThrow(elasticsearchError(404, "index_not_found_exception"));

        // when
        Throwable thrown = catchThrowable(() -> catalog.findById("demo", "603"));

        // then
        assertThat(thrown).isInstanceOf(ElasticsearchAccessException.class);
        assertThat(((ElasticsearchAccessException) thrown).errorCode()).isEqualTo(ErrorCode.ELASTICSEARCH_REJECTED);
        assertThat(thrown.getCause()).isInstanceOf(ElasticsearchException.class);
    }

    @Test
    @DisplayName("findById con un 5xx de Elasticsearch falla con EFX-ES-001")
    // Mockito entrega la función del cliente sin el tipo genérico.
    @SuppressWarnings("unchecked")
    void shouldFailWhenElasticsearchAnswersAServerError() throws Exception {
        // given
        when(client.get(any(Function.class), eq(Movie.class)))
                .thenThrow(elasticsearchError(503, "unavailable_shards_exception"));

        // when
        Throwable thrown = catchThrowable(() -> catalog.findById("demo", "603"));

        // then
        assertThat(thrown).isInstanceOf(ElasticsearchAccessException.class);
        assertThat(((ElasticsearchAccessException) thrown).errorCode())
                .isEqualTo(ErrorCode.ELASTICSEARCH_UNAVAILABLE);
    }

    @Test
    @DisplayName("findById sin respuesta (IOException) falla con EFX-ES-001 y conserva la causa")
    // Mockito entrega la función del cliente sin el tipo genérico.
    @SuppressWarnings("unchecked")
    void shouldFailWhenElasticsearchDoesNotAnswer() throws Exception {
        // given
        IOException cut = new IOException("conexión cerrada");
        when(client.get(any(Function.class), eq(Movie.class))).thenThrow(cut);

        // when
        Throwable thrown = catchThrowable(() -> catalog.findById("demo", "603"));

        // then
        assertThat(thrown).isInstanceOf(ElasticsearchAccessException.class);
        assertThat(((ElasticsearchAccessException) thrown).errorCode())
                .isEqualTo(ErrorCode.ELASTICSEARCH_UNAVAILABLE);
        assertThat(thrown.getCause()).isSameAs(cut);
    }

    private static ElasticsearchException elasticsearchError(int status, String type) {
        return new ElasticsearchException("get", ErrorResponse.of(response -> response
                .status(status).error(cause -> cause.type(type).reason("prueba"))));
    }
}
