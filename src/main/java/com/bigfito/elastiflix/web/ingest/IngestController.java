package com.bigfito.elastiflix.web.ingest;

import java.util.List;
import java.util.Locale;

import com.bigfito.elastiflix.core.text.Thousands;
import com.bigfito.elastiflix.ingest.job.SaveResult;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
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

    private static final String REJECTED = "rejected";

    private final IngestService ingest;
    private final MovieForms forms;
    private final IngestLimits limits;
    private final IngestTranslator translator;

    /**
     * Crea la página con la fachada de carga.
     *
     * @param ingest fachada
     * @param forms  formulario a película
     * @param limits     tamaño máximo y lote configurados
     * @param translator rechazos del registro individual en el idioma de la persona
     */
    public IngestController(
            IngestService ingest, MovieForms forms, IngestLimits limits, IngestTranslator translator) {
        this.ingest = ingest;
        this.forms = forms;
        this.limits = limits;
        this.translator = translator;
    }

    /**
     * Muestra las pestañas. Un guardado anterior llega por flash; sus rechazos se muestran en el idioma de
     * la página, aunque se hayan producido en otro.
     *
     * @param model  navegación, tamaño máximo y lote (este, ya como texto agrupado por miles)
     * @param locale idioma de la petición
     * @return plantilla {@code ingest}
     */
    @GetMapping("/cargar")
    public String page(Model model, Locale locale) {
        if (model.getAttribute(REJECTED) instanceof List<?> rejected) {
            List<ValidationIssue> issues = rejected.stream()
                    .filter(ValidationIssue.class::isInstance)
                    .map(ValidationIssue.class::cast)
                    .toList();
            model.addAttribute(REJECTED, translator.issues(issues, locale));
        }
        model.addAttribute("nav", "cargar");
        model.addAttribute("maxFileLabel", limits.maxFileLabel());
        model.addAttribute("maxFileBytes", limits.maxFileSize().toBytes());
        // Texto ya agrupado por miles: con MessageFormat, un int saldría «1.000» en español (U-12).
        model.addAttribute("batchSize", Thousands.of(limits.batchSize(), locale));
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
            case MovieForm.Invalid invalid -> redirect.addFlashAttribute(REJECTED, List.of(invalid.issue()));
        }
        return "redirect:/cargar";
    }

    private static void remember(RedirectAttributes redirect, SaveResult result) {
        if (result instanceof SaveResult.Saved saved) {
            redirect.addFlashAttribute("savedTitle", saved.movie().title());
            return;
        }
        if (result instanceof SaveResult.Rejected rejected) {
            redirect.addFlashAttribute(REJECTED, rejected.report().issues());
        }
    }
}
