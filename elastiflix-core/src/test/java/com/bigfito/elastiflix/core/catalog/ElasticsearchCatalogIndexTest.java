package com.bigfito.elastiflix.core.catalog;

import java.time.Clock;
import java.util.function.Function;

import com.bigfito.elastiflix.core.model.Movie;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import co.elastic.clients.util.ObjectBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
}
