package com.bigfito.elastiflix.web.error;

import com.bigfito.elastiflix.core.error.ElastiflixException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bigfito.elastiflix.web.layout.HtmlPage;
import com.bigfito.elastiflix.web.locale.UiMessages;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Atiende el reenvío {@code /error} del servidor: 404 con navegación, o el código {@code EFX-*}.
 */
@Controller
public class ErrorPageController implements ErrorController, HtmlPage {

    /**
     * Elige la plantilla según el estado que reenvió el servidor.
     *
     * @param request petición con el estado y, si la hay, la excepción
     * @param model   código, mensaje y navegación
     * @return {@code not-found} o {@code error}
     */
    @RequestMapping("/error")
    public String error(HttpServletRequest request, Model model) {
        model.addAttribute("nav", "");
        if (status(request) == HttpServletResponse.SC_NOT_FOUND) {
            return "not-found";
        }
        return fromException(request, model);
    }

    private static String fromException(HttpServletRequest request, Model model) {
        ElastiflixException elastiflix = elastiflix(request);
        if (elastiflix != null) {
            return ErrorPages.filled(model, elastiflix);
        }
        // Sin código EFX (un 400 o un 405 del contenedor), el título es el estado HTTP: el h1 nunca queda vacío.
        UiMessages messages = UiMessages.defaultCatalog();
        model.addAttribute("code", messages.get("error.http.code", status(request)));
        model.addAttribute("message", messages.get("error.http.unexpected"));
        return "error";
    }

    private static int status(HttpServletRequest request) {
        Object value = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (value instanceof Integer code) {
            return code;
        }
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    private static ElastiflixException elastiflix(HttpServletRequest request) {
        Object error = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        if (error instanceof ElastiflixException found) {
            return found;
        }
        return cause(error);
    }

    private static ElastiflixException cause(Object error) {
        if (error instanceof Exception exception && exception.getCause() instanceof ElastiflixException found) {
            return found;
        }
        return null;
    }
}
