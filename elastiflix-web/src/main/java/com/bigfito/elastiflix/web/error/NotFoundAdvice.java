package com.bigfito.elastiflix.web.error;

import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.web.layout.PageShells;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 404 de una ruta que no tiene controlador.
 *
 * <p>No puede limitarse a las páginas HTML: la excepción sale antes de elegir un controlador.</p>
 */
@ControllerAdvice
public class NotFoundAdvice {

    private static final Logger LOG = LoggerFactory.getLogger(NotFoundAdvice.class);

    private final CatalogStatusService catalog;

    /**
     * Crea el consejo con el estado del catálogo para la cabecera del 404.
     *
     * @param catalog alias y conteo del pie
     */
    public NotFoundAdvice(CatalogStatusService catalog) {
        this.catalog = catalog;
    }

    /**
     * Ruta que no corresponde a ninguna pantalla.
     *
     * @param exception ruta ausente; no se muestra
     * @param model     navegación y cabecera
     * @return la plantilla {@code not-found}
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String missing(NoResourceFoundException exception, Model model) {
        LOG.info("Ruta no encontrada: {}", exception.getResourcePath());
        model.addAttribute("nav", "");
        model.addAttribute("shell", PageShells.from(catalog.status()));
        return "not-found";
    }
}
