package com.bigfito.elastiflix.core.catalog;

import java.util.regex.Pattern;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El catálogo configurado responde y tiene películas.
 *
 * <p>Solo lee. No crea ni borra índices. Afirma las 6 899 películas del catálogo de referencia. Si
 * {@code _count} ya no es ese número, la prueba se omite y no pasa en falso.</p>
 */
@SpringBootTest
class CatalogStatusServiceIT {

    private static final long CATALOG_COUNT = 6_899L;
    private static final Pattern INDEX_NAME = Pattern.compile("[a-z0-9][a-z0-9-]*-\\d{8}-\\d{6}");

    private final CatalogStatusService service;
    private final CatalogIndex catalog;
    private final ElastiflixProperties properties;

    /**
     * Spring no inyecta el constructor de una prueba si no lleva {@code @Autowired}.
     *
     * @param service    estado del catálogo
     * @param catalog    {@code _count} del alias
     * @param properties alias configurado
     */
    @Autowired
    CatalogStatusServiceIT(
            CatalogStatusService service, CatalogIndex catalog, ElastiflixProperties properties) {
        this.service = service;
        this.catalog = catalog;
        this.properties = properties;
    }

    @Test
    @DisplayName("el conteo del servicio es el de referencia cuando el catálogo no cambió")
    void shouldCountTheCatalogWhenItIsReachable() {
        // given
        String alias = properties.index().alias();
        long counted = catalog.count(alias);
        Assumptions.assumeTrue(counted == CATALOG_COUNT, () ->
                "El catálogo cambió: se esperaban " + CATALOG_COUNT + " películas y hay " + counted);

        // when
        CatalogStatus status = service.status();

        // then
        assertThat(status.connected()).isTrue();
        assertThat(status.name()).isEqualTo(alias);
        assertThat(status.documentCount()).isEqualTo(CATALOG_COUNT);
        assertThat(status.errorCode()).isNull();
        // La app crea el índice con el sufijo -AAAAMMDD-HHMMSS. El prefijo puede ser otro alias: la ronda 4 de
        // calidad publicó moviendo el alias a un índice de staging (data-quality/APRENDIZAJES.md, §6).
        assertThat(status.concreteIndex()).isNotEqualTo(alias).matches(INDEX_NAME);
    }
}
