package com.bigfito.elastiflix.search.transport;

import java.io.IOException;

import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.msearch.RequestItem;
import co.elastic.clients.json.JsonpMappingException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * El transporte de búsqueda traduce cualquier falla del cliente a un código EFX.
 */
@DisplayName("ElasticsearchSearchTransport")
class ElasticsearchSearchTransportTest {

    private final ElasticsearchClient client = mock(ElasticsearchClient.class);
    private final ElasticsearchSearchTransport transport = new ElasticsearchSearchTransport(client);

    @Test
    @DisplayName("una respuesta de _search que no se puede leer da EFX-ES-001")
    void shouldGiveTheUnavailableCodeWhenTheSearchResponseCannotBeRead() throws IOException {
        // given
        SearchRequest request = SearchRequest.of(search -> search.index("elastiflix-reloaded"));
        when(client.search(any(SearchRequest.class), eq(Movie.class))).thenThrow(unreadable());

        // when
        var thrown = catchThrowableOfType(ElasticsearchAccessException.class,
                () -> transport.search(request, Movie.class));

        // then
        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.ELASTICSEARCH_UNAVAILABLE);
        assertThat(thrown).hasCauseInstanceOf(JsonpMappingException.class);
    }

    @Test
    @DisplayName("una respuesta de _msearch que no se puede leer da EFX-ES-001")
    void shouldGiveTheUnavailableCodeWhenTheMultiSearchResponseCannotBeRead() throws IOException {
        // given
        MsearchRequest request = MsearchRequest.of(search -> search.searches(RequestItem.of(item -> item
                .header(header -> header.index("elastiflix-reloaded")).body(body -> body.size(1)))));
        when(client.msearch(any(MsearchRequest.class), eq(Movie.class))).thenThrow(unreadable());

        // when
        var thrown = catchThrowableOfType(ElasticsearchAccessException.class,
                () -> transport.msearch(request, Movie.class));

        // then
        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.ELASTICSEARCH_UNAVAILABLE);
    }

    @Test
    @DisplayName("una respuesta de _count que no se puede leer da EFX-ES-001")
    void shouldGiveTheUnavailableCodeWhenTheCountResponseCannotBeRead() throws IOException {
        // given
        CountRequest request = CountRequest.of(count -> count.index("elastiflix-reloaded"));
        when(client.count(any(CountRequest.class))).thenThrow(unreadable());

        // when
        var thrown = catchThrowableOfType(ElasticsearchAccessException.class, () -> transport.count(request));

        // then
        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.ELASTICSEARCH_UNAVAILABLE);
    }

    private static JsonpMappingException unreadable() {
        return new JsonpMappingException("respuesta ilegible", null);
    }
}
