package com.bigfito.elastiflix.web.layout;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * El indicador de Actuator publica alias, índice y documentos sin escribir en el catálogo.
 */
class CatalogHealthIndicatorTest {

    @Test
    @DisplayName("un catálogo que responde queda UP con alias, index y documents")
    void shouldReportUpWhenTheCatalogAnswers() {
        CatalogStatusService catalog = mock(CatalogStatusService.class);
        when(catalog.status()).thenReturn(new CatalogStatus(
                "elastiflix-reloaded", "elastiflix-reloaded-20261018-000000", 4322L, true, null));

        Health health = new CatalogHealthIndicator(catalog).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("alias", "elastiflix-reloaded")
                .containsEntry("index", "elastiflix-reloaded-20261018-000000")
                .containsEntry("documents", 4322L);
    }

    @Test
    @DisplayName("sin Elasticsearch el estado es DOWN, con errorCode y sin detalles nulos")
    void shouldReportDownWithoutNullDetails() {
        CatalogStatusService catalog = mock(CatalogStatusService.class);
        when(catalog.status()).thenReturn(new CatalogStatus(
                "elastiflix-reloaded", null, null, false, "EFX-ES-001"));

        Health health = new CatalogHealthIndicator(catalog).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("errorCode", "EFX-ES-001");
        assertThat(health.getDetails().values()).doesNotContainNull();
    }
}
