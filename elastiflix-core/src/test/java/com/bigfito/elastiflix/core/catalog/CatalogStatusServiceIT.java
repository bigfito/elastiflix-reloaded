package com.bigfito.elastiflix.core.catalog;

import java.util.regex.Pattern;

import com.bigfito.elastiflix.core.TestApplication;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El catálogo configurado responde y tiene películas.
 *
 * <p>Solo lee. No crea ni borra índices. El número de documentos cambia cuando alguien importa un
 * archivo, así que la prueba no lo escribe fijo: al inicio pide {@code _count} del alias y exige
 * que el servicio devuelva ese mismo conteo, y que sea mayor que cero.</p>
 */
@SpringBootTest(classes = TestApplication.class)
class CatalogStatusServiceIT {

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
    @DisplayName("el conteo del servicio coincide con _count y es mayor que cero")
    void shouldCountTheCatalogWhenItIsReachable() {
        // given
        String alias = properties.index().alias();
        long counted = catalog.count(alias);

        // when
        CatalogStatus status = service.status();

        // then
        assertThat(counted).isPositive();
        assertThat(status.connected()).isTrue();
        assertThat(status.name()).isEqualTo(alias);
        assertThat(status.documentCount()).isEqualTo(counted);
        assertThat(status.errorCode()).isNull();
        assertThat(status.concreteIndex()).matches(Pattern.quote(alias) + "-\\d{8}-\\d{6}");
    }
}
