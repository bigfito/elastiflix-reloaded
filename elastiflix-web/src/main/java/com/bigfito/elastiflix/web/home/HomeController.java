package com.bigfito.elastiflix.web.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página de inicio. La tarea 4.2 llena la caja, los paneles y las sugerencias.
 */
@Controller
public class HomeController {

    /**
     * Muestra el cromo y el título de la demo.
     *
     * @param model marca Buscar como página actual
     * @return la plantilla {@code home}
     */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("nav", "buscar");
        return "home";
    }
}
