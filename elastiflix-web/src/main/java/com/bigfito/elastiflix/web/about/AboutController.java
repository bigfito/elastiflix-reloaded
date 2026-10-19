package com.bigfito.elastiflix.web.about;

import com.bigfito.elastiflix.web.ingest.IngestLimits;
import com.bigfito.elastiflix.web.layout.HtmlPage;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página Acerca de: cómo funciona la demo, con el alias y el conteo del catálogo.
 *
 * <p>El tamaño máximo del archivo y el lote de escritura salen de {@link IngestLimits}, el mismo origen que
 * usa «Cargar datos», para que las dos páginas nunca muestren cifras distintas.</p>
 */
@Controller
public class AboutController implements HtmlPage {

    private final IngestLimits limits;

    /**
     * Crea el controlador con los límites de carga configurados.
     *
     * @param limits tamaño máximo del archivo y películas por lote
     */
    public AboutController(IngestLimits limits) {
        this.limits = limits;
    }

    /**
     * Muestra Acerca de. El alias y el conteo llegan en {@code shell}.
     *
     * @param model la navegación marca esta página; lleva el tamaño máximo y el lote
     * @return la plantilla {@code about}
     */
    @GetMapping("/acerca")
    public String about(Model model) {
        model.addAttribute("nav", "acerca");
        model.addAttribute("maxFileLabel", limits.maxFileLabel());
        model.addAttribute("batchSize", limits.batchSize());
        return "about";
    }
}
