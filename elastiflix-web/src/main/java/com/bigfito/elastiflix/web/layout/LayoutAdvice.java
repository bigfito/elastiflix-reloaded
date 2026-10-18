package com.bigfito.elastiflix.web.layout;

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
        return PageShells.from(catalog.status());
    }
}
