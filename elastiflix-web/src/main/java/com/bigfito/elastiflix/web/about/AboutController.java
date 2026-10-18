package com.bigfito.elastiflix.web.about;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página Acerca de: cómo funciona la demo, con el alias y el conteo del catálogo.
 */
@Controller
public class AboutController {

    /**
     * Muestra Acerca de. El alias y el conteo llegan en {@code shell}.
     *
     * @param model la navegación marca esta página
     * @return la plantilla {@code about}
     */
    @GetMapping("/acerca")
    public String about(Model model) {
        model.addAttribute("nav", "acerca");
        return "about";
    }
}
