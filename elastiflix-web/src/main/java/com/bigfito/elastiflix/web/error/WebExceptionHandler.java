package com.bigfito.elastiflix.web.error;

import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.web.layout.HtmlPage;
import com.bigfito.elastiflix.web.layout.PageShells;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Muestra el código {@code EFX-*} cuando una página HTML no captura la excepción.
 */
@ControllerAdvice(assignableTypes = HtmlPage.class)
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

    private void shell(Model model) {
        model.addAttribute("shell", PageShells.from(catalog.status()));
    }
}
