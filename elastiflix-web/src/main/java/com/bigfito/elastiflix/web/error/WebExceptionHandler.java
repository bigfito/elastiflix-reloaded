package com.bigfito.elastiflix.web.error;

import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.core.error.ElastiflixException;
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
 * Muestra el código {@code EFX-*} cuando una página no captura la excepción, y un 404 con navegación.
 */
@ControllerAdvice
public class WebExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(WebExceptionHandler.class);

    private final CatalogStatusService catalog;

    /**
     * Crea el consejo con el estado del catálogo, para que la cabecera también salga en el error.
     *
     * @param catalog alias y conteo del pie
     */
    public WebExceptionHandler(CatalogStatusService catalog) {
        this.catalog = catalog;
    }

    /**
     * Página de error con el código y el mensaje del catálogo.
     *
     * @param exception falla de la demo
     * @param model     código, mensaje y cabecera
     * @return la plantilla {@code error}
     */
    @ExceptionHandler(ElastiflixException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String elastiflix(ElastiflixException exception, Model model) {
        LOG.info("Página de error {}", exception.errorCode().code());
        shell(model);
        return ErrorPages.filled(model, exception);
    }

    /**
     * Ruta que no corresponde a ninguna pantalla.
     *
     * @param model navegación y cabecera
     * @return la plantilla {@code not-found}
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String missing(Model model) {
        model.addAttribute("nav", "");
        shell(model);
        return "not-found";
    }

    private void shell(Model model) {
        model.addAttribute("shell", PageShells.from(catalog.status()));
    }
}
