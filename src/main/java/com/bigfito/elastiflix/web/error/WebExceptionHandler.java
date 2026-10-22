package com.bigfito.elastiflix.web.error;

import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.web.layout.HtmlPage;
import com.bigfito.elastiflix.web.layout.PageShell;
import com.bigfito.elastiflix.web.layout.PageShells;
import com.bigfito.elastiflix.web.locale.UiMessages;

import jakarta.servlet.http.HttpServletRequest;

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
    private final UiMessages messages;

    /**
     * Crea el consejo con el estado del catálogo, para que la cabecera también salga en el error.
     *
     * <p>Solo lo lee si la petición todavía no trae el cromo de {@code LayoutAdvice}.</p>
     *
     * @param catalog  alias y conteo del pie
     * @param messages textos de la cabecera y el pie
     */
    public WebExceptionHandler(CatalogStatusService catalog, UiMessages messages) {
        this.catalog = catalog;
        this.messages = messages;
    }

    /**
     * Página de error con el código y el mensaje del catálogo.
     *
     * @param exception falla de la demo
     * @param model     código, mensaje y cabecera
     * @param request   petición; trae el cromo si {@code LayoutAdvice} ya lo armó
     * @return la plantilla {@code error}
     */
    @ExceptionHandler(ElastiflixException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String elastiflix(ElastiflixException exception, Model model, HttpServletRequest request) {
        LOG.info("Página de error {}", exception.errorCode().code());
        model.addAttribute("shell", shell(request));
        return ErrorPages.filled(model, exception, messages);
    }

    private PageShell shell(HttpServletRequest request) {
        if (request.getAttribute(PageShells.REQUEST_ATTRIBUTE) instanceof PageShell ready) {
            return ready;
        }
        return PageShells.from(catalog.status(), request, messages);
    }
}
