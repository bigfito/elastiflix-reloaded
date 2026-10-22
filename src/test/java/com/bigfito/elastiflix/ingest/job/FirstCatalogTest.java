package com.bigfito.elastiflix.ingest.job;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.catalog.CatalogName;
import com.bigfito.elastiflix.core.catalog.IndexNames;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El registro individual crea el catálogo solo si el alias no existe, y no deja un índice suelto si el alias no se
 * puede poner.
 */
@ExtendWith(MockitoExtension.class)
class FirstCatalogTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-22T03:04:05Z"), ZoneOffset.UTC);
    private static final String ALIAS = "elastiflix-it-x7k2";
    private static final String NEW_INDEX = "elastiflix-it-x7k2-20261022-030405";

    @Mock
    private CatalogIndex catalog;

    @Test
    @DisplayName("con el alias en un índice, no crea nada")
    void shouldDoNothingWhenTheCatalogExists() {
        when(catalog.describe(ALIAS)).thenReturn(new CatalogName.Alias(ALIAS, "elastiflix-it-x7k2-20261018-000000"));

        first().ensure(ALIAS);

        verify(catalog, never()).createWithMapping(anyString());
        verify(catalog, never()).publish(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("sin alias, crea el índice con el mapping y le pone el alias solo con add")
    void shouldCreateAndPublishWhenTheAliasIsMissing() {
        when(catalog.describe(ALIAS)).thenReturn(new CatalogName.Missing(ALIAS));

        first().ensure(ALIAS);

        verify(catalog).createWithMapping(NEW_INDEX);
        verify(catalog).publish(ALIAS, NEW_INDEX, null);
        verify(catalog, never()).deleteIndex(anyString());
    }

    @Test
    @DisplayName("si _aliases responde 4xx y el alias sigue sin existir, borra el índice nuevo y da EFX-ES-004")
    void shouldDeleteTheNewIndexWhenThePublishIsRejected() {
        CatalogName.Missing missing = new CatalogName.Missing(ALIAS);
        when(catalog.describe(ALIAS)).thenReturn(missing, missing);
        doThrow(new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE, rejected()))
                .when(catalog).publish(ALIAS, NEW_INDEX, null);

        ElastiflixException thrown = catchThrowableOfType(ElastiflixException.class, () -> first().ensure(ALIAS));

        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.ALIAS_PUBLISH_FAILED);
        verify(catalog).deleteIndex(NEW_INDEX);
    }

    @Test
    @DisplayName("si publicar se corta y el alias ya apunta al índice nuevo, queda creado")
    void shouldKeepTheCatalogWhenThePublishWasAppliedAnyway() {
        when(catalog.describe(ALIAS)).thenReturn(new CatalogName.Missing(ALIAS), new CatalogName.Alias(ALIAS, NEW_INDEX));
        doThrow(new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE, new IOException("corte")))
                .when(catalog).publish(ALIAS, NEW_INDEX, null);

        first().ensure(ALIAS);

        verify(catalog, never()).deleteIndex(anyString());
    }

    @Test
    @DisplayName("si publicar se corta y el alias sigue sin existir, da EFX-IMP-006 y no borra el índice")
    void shouldLeaveTheIndexWhenThePublishIsUncertain() {
        CatalogName.Missing missing = new CatalogName.Missing(ALIAS);
        when(catalog.describe(ALIAS)).thenReturn(missing, missing);
        doThrow(new ElasticsearchAccessException(ErrorCode.ELASTICSEARCH_UNAVAILABLE, new IOException("corte")))
                .when(catalog).publish(ALIAS, NEW_INDEX, null);

        ElastiflixException thrown = catchThrowableOfType(ElastiflixException.class, () -> first().ensure(ALIAS));

        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.PUBLISH_UNCERTAIN);
        verify(catalog, never()).deleteIndex(anyString());
    }

    @Test
    @DisplayName("un alias en varios índices da EFX-ES-005 sin crear nada")
    void shouldRefuseAnAmbiguousAlias() {
        when(catalog.describe(ALIAS)).thenReturn(new CatalogName.Ambiguous(ALIAS));

        ElastiflixException thrown = catchThrowableOfType(ElastiflixException.class, () -> first().ensure(ALIAS));

        assertThat(thrown.errorCode()).isEqualTo(ErrorCode.ALIAS_AMBIGUOUS);
        verify(catalog, never()).createWithMapping(anyString());
    }

    private FirstCatalog first() {
        return new FirstCatalog(catalog, new IndexNames(), CLOCK);
    }

    private static ElasticsearchException rejected() {
        return new ElasticsearchException("indices.update_aliases", ErrorResponse.of(response -> response
                .status(400).error(cause -> cause.type("illegal_argument_exception").reason("rechazo"))));
    }
}
