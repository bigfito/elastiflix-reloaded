package com.bigfito.elastiflix.web.layout;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.core.catalog.CatalogStatusService;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Pone la cabecera y el pie en cada vista, con el estado real del catálogo.
 */
@ControllerAdvice
public class LayoutAdvice {

    private final CatalogStatusService catalog;

    /**
     * Crea el consejo con la fachada del catálogo.
     *
     * @param catalog alias, índice vigente y conteo
     */
    public LayoutAdvice(CatalogStatusService catalog) {
        this.catalog = catalog;
    }

    /**
     * Datos del cromo. Thymeleaf los ve como {@code shell}.
     *
     * @return pastilla, pie y aviso de TMDB
     */
    @ModelAttribute("shell")
    public PageShell shell() {
        return from(catalog.status());
    }

    private static PageShell from(CatalogStatus status) {
        String connection = CatalogTexts.CONNECTED;
        if (!status.connected()) {
            connection = CatalogTexts.disconnected(status.errorCode());
        }
        String index = CatalogTexts.indexLine(status.name(), status.documentCount());
        return new PageShell(connection, index, status.connected(), CatalogTexts.TMDB);
    }
}
