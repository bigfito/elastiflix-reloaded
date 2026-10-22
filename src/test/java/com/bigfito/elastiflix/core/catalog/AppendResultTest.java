package com.bigfito.elastiflix.core.catalog;

import java.util.List;

import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.OperationType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Un lote de append cuenta sus ítems aceptados y rechazados, porque los aceptados ya quedaron escritos.
 */
class AppendResultTest {

    private static final String ALIAS = "elastiflix-it-x7k2";

    @Test
    @DisplayName("un lote sin errores: todos aceptados, ninguno rechazado")
    void shouldAcceptEveryItemWhenThereAreNoErrors() {
        BulkResponse response = response(false, List.of(accepted("1"), accepted("2")));

        AppendResult result = CatalogRequests.appendResult(ALIAS, response);

        assertThat(result).isEqualTo(new AppendResult(2, 0));
        assertThat(result.hasRejections()).isFalse();
    }

    @Test
    @DisplayName("un lote a medias cuenta aparte los aceptados y los rechazados")
    void shouldCountBothSidesWhenTheBatchIsPartial() {
        BulkResponse response = response(true, List.of(accepted("1"), rejected("2"), accepted("3")));

        AppendResult result = CatalogRequests.appendResult(ALIAS, response);

        assertThat(result).isEqualTo(new AppendResult(2, 1));
        assertThat(result.hasRejections()).isTrue();
    }

    private static BulkResponse response(boolean errors, List<BulkResponseItem> items) {
        return BulkResponse.of(response -> response.errors(errors).took(1).items(items));
    }

    private static BulkResponseItem accepted(String id) {
        return BulkResponseItem.of(item -> item.operationType(OperationType.Index).index(ALIAS).id(id).status(201));
    }

    private static BulkResponseItem rejected(String id) {
        return BulkResponseItem.of(item -> item.operationType(OperationType.Index).index(ALIAS).id(id).status(400)
                .error(error -> error.type("document_parsing_exception").reason("campo inválido")));
    }
}
