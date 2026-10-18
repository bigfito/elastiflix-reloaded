package com.bigfito.elastiflix.web.layout;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Salud del catálogo para Actuator. El componente se llama {@code elastiflixCatalog}.
 */
@Component("elastiflixCatalogHealthIndicator")
public class CatalogHealthIndicator implements HealthIndicator {

    private final CatalogStatusService catalog;

    /**
     * Crea el indicador con la misma lectura que usa el pie.
     *
     * @param catalog alias, índice vigente y conteo
     */
    public CatalogHealthIndicator(CatalogStatusService catalog) {
        this.catalog = catalog;
    }

    /**
     * {@code UP} si el alias respondió. Los detalles son {@code alias}, {@code index} y
     * {@code documents}.
     *
     * @return el estado para {@code GET /actuator/health}
     */
    @Override
    public Health health() {
        return healthOf(catalog.status());
    }

    private static Health healthOf(CatalogStatus status) {
        Health.Builder builder = Health.down();
        if (status.connected()) {
            builder = Health.up();
        }
        return builder
                .withDetail("alias", status.name())
                .withDetail("index", status.concreteIndex())
                .withDetail("documents", status.documentCount())
                .build();
    }
}
