package com.bigfito.elastiflix.web.ingest;

import java.util.List;

import com.bigfito.elastiflix.ingest.job.SaveResult;
import com.bigfito.elastiflix.ingest.service.IngestService;

import com.bigfito.elastiflix.web.layout.HtmlPage;

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
public class IngestController implements HtmlPage {

    private final IngestService ingest;
    private final MovieForms forms;
    private final IngestLimits limits;

    /**
     * Crea la página con la fachada de carga.
     *
     * @param ingest fachada
     * @param forms  formulario a película
     * @param limits tamaño máximo y lote configurados
     */
    public IngestController(IngestService ingest, MovieForms forms, IngestLimits limits) {
        this.ingest = ingest;
        this.forms = forms;
        this.limits = limits;
    }

    /**
     * Muestra las pestañas. Un guardado anterior llega por flash.
     *
     * @param model navegación, tamaño máximo y lote
     * @return plantilla {@code ingest}
     */
    @GetMapping("/cargar")
    public String page(Model model) {
        model.addAttribute("nav", "cargar");
        model.addAttribute("maxFileLabel", limits.maxFileLabel());
        model.addAttribute("maxFileBytes", limits.maxFileSize().toBytes());
        model.addAttribute("batchSize", limits.batchSize());
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
        switch (forms.read(form)) {
            case MovieForm.Read read -> remember(redirect, ingest.saveOne(read.movie()));
            case MovieForm.Invalid invalid -> redirect.addFlashAttribute("rejected", List.of(invalid.issue()));
        }
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
