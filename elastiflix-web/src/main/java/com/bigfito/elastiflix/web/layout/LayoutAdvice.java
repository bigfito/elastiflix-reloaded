package com.bigfito.elastiflix.web.layout;

import com.bigfito.elastiflix.core.catalog.CatalogStatusService;
import com.bigfito.elastiflix.web.locale.LocaleConfig;
import com.bigfito.elastiflix.web.locale.UiMessages;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Pone la cabecera y el pie en cada vista, con el estado real del catálogo.
 */
@ControllerAdvice(assignableTypes = HtmlPage.class)
@Import(LocaleConfig.class)
public class LayoutAdvice {

    private final CatalogStatusService catalog;
    private final UiMessages messages;

    /**
     * Crea el consejo con la fachada del catálogo.
     *
     * @param catalog  alias, índice vigente y conteo
     * @param messages textos de la cabecera y el pie
     */
    public LayoutAdvice(CatalogStatusService catalog, UiMessages messages) {
        this.catalog = catalog;
        this.messages = messages;
    }

    /**
     * Datos del cromo. Thymeleaf los ve como {@code shell}.
     *
     * <p>También quedan en la petición ({@link PageShells#REQUEST_ATTRIBUTE}), para que la página de error
     * los reutilice si el controlador falla después.</p>
     *
     * @param request petición en curso
     * @return pastilla, pie y aviso de TMDB
     */
    @ModelAttribute("shell")
    public PageShell shell(HttpServletRequest request) {
        if (request != null && request.getAttribute(PageShells.REQUEST_ATTRIBUTE) instanceof PageShell ready) {
            return ready;
        }
        PageShell shell = PageShells.from(catalog.status(), request, messages);
        if (request != null) {
            request.setAttribute(PageShells.REQUEST_ATTRIBUTE, shell);
        }
        return shell;
    }
}
