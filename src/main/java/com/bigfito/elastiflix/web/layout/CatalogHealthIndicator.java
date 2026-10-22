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
     * {@code UP} si el alias respondió. Un detalle nulo no se publica: Actuator fallaría con 500.
     *
     * @return el estado para {@code GET /actuator/health}
     */
    @Override
    public Health health() {
        return healthOf(catalog.status());
    }

    private static Health healthOf(CatalogStatus status) {
        Health.Builder builder = status.connected() ? Health.up() : Health.down();
        detail(builder, "alias", status.name());
        detail(builder, "index", status.concreteIndex());
        detail(builder, "documents", status.documentCount());
        detail(builder, "errorCode", status.errorCode());
        return builder.build();
    }

    private static void detail(Health.Builder builder, String key, Object value) {
        if (value != null) {
            builder.withDetail(key, value);
        }
    }
}
