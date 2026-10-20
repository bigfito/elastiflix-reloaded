package com.bigfito.elastiflix.web.error;

import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.web.locale.UiMessages;

import org.springframework.ui.Model;

/**
 * Arma el modelo de la página de error a partir de una excepción de la demo.
 */
final class ErrorPages {

    private ErrorPages() {
    }

    /**
     * Pone el código y el «qué pasó» del catálogo en el modelo según el idioma activo.
     *
     * <p>{@code summary()} no trae punto final; la página lo agrega para que el texto visible sea una frase
     * completa, como el resto de la interfaz.</p>
     *
     * @param model     vista de error
     * @param exception falla de la demo
     * @return nombre de la plantilla {@code error}
     */
    static String filled(Model model, ElastiflixException exception) {
        model.addAttribute("nav", "");
        model.addAttribute("code", exception.errorCode().code());
        model.addAttribute("message", UiMessages.defaultCatalog().errorSummary(exception.errorCode()) + ".");
        return "error";
    }
}
