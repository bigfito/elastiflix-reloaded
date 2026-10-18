package com.bigfito.elastiflix.core.catalog;

import java.util.regex.Pattern;

import com.bigfito.elastiflix.core.TestApplication;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El catálogo configurado responde y tiene las películas de la demo.
 *
 * <p>Solo lee. No crea ni borra índices.</p>
 */
@SpringBootTest(classes = TestApplication.class)
class CatalogStatusServiceIT {

    private static final long CATALOG_DOCUMENTS = 4322;

    private final CatalogStatusService service;

    /**
     * Spring no inyecta el constructor de una prueba si no lleva {@code @Autowired}.
     *
     * @param service estado del catálogo
     */
    @Autowired
    CatalogStatusServiceIT(CatalogStatusService service) {
        this.service = service;
    }

    @Test
    @DisplayName("el catálogo configurado tiene 4322 documentos")
    void shouldCountTheCatalogWhenItIsReachable() {
        CatalogStatus status = service.status();

        assertThat(status.connected()).isTrue();
        assertThat(status.documentCount()).isEqualTo(CATALOG_DOCUMENTS);
        assertThat(status.errorCode()).isNull();
        assertThat(status.concreteIndex()).matches(Pattern.quote(status.name()) + "-\\d{8}-\\d{6}");
    }
}
