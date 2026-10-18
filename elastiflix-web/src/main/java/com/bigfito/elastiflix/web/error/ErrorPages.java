package com.bigfito.elastiflix.web.error;

import com.bigfito.elastiflix.core.error.ElastiflixException;

import org.springframework.ui.Model;

/**
 * Arma el modelo de la página de error a partir de una excepción de la demo.
 */
final class ErrorPages {

    private ErrorPages() {
    }

    /**
     * Pone el código y el mensaje del catálogo en el modelo.
     *
     * @param model     vista de error
     * @param exception falla de la demo
     * @return nombre de la plantilla {@code error}
     */
    static String filled(Model model, ElastiflixException exception) {
        model.addAttribute("nav", "");
        model.addAttribute("code", exception.errorCode().code());
        model.addAttribute("message", exception.errorCode().userMessage());
        return "error";
    }
}
