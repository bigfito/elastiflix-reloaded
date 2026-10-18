package com.bigfito.elastiflix.web.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página de inicio: caja de búsqueda, paneles de modo y sugerencias.
 */
@Controller
public class HomeController {

    private final SuggestionCatalog suggestions;

    /**
     * Crea el inicio con las sugerencias fijas del mockup.
     *
     * @param suggestions enlaces a {@code /buscar}
     */
    public HomeController(SuggestionCatalog suggestions) {
        this.suggestions = suggestions;
    }

    /**
     * Muestra Inicio. Buscar queda marcado en la navegación.
     *
     * @param model sugerencias y página actual
     * @return la plantilla {@code home}
     */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("nav", "buscar");
        model.addAttribute("suggestions", suggestions.all());
        return "home";
    }
}
