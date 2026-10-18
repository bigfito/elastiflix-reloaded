package com.bigfito.elastiflix.web.ingest;

import com.bigfito.elastiflix.ingest.job.SaveResult;
import com.bigfito.elastiflix.ingest.service.IngestService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Página de carga y el registro individual, que se escribe directo en el catálogo.
 */
@Controller
public class IngestController {

    private final IngestService ingest;
    private final MovieForms forms;

    /**
     * Crea la página con la fachada de carga.
     *
     * @param ingest fachada
     * @param forms  formulario a película
     */
    public IngestController(IngestService ingest, MovieForms forms) {
        this.ingest = ingest;
        this.forms = forms;
    }

    /**
     * Muestra las pestañas. Un guardado anterior llega por flash.
     *
     * @param model navegación
     * @return plantilla {@code ingest}
     */
    @GetMapping("/cargar")
    public String page(Model model) {
        model.addAttribute("nav", "cargar");
        return "ingest";
    }

    /**
     * Guarda una película y vuelve a la página con el resultado.
     *
     * @param form     campos del mockup
     * @param redirect título guardado o errores
     * @return redirección a {@code /cargar}
     */
    @PostMapping("/cargar/registro")
    public String save(
            @RequestParam MultiValueMap<String, String> form, RedirectAttributes redirect) {
        remember(redirect, ingest.saveOne(forms.toMovie(form)));
        return "redirect:/cargar";
    }

    private static void remember(RedirectAttributes redirect, SaveResult result) {
        if (result instanceof SaveResult.Saved saved) {
            redirect.addFlashAttribute("savedTitle", saved.movie().title());
            return;
        }
        if (result instanceof SaveResult.Rejected rejected) {
            redirect.addFlashAttribute("rejected", rejected.report().issues());
        }
    }
}
