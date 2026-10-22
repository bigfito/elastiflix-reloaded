package com.bigfito.elastiflix.web.search;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.SearchValidationException;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.result.SearchService;
import com.bigfito.elastiflix.web.layout.HtmlPage;
import com.bigfito.elastiflix.web.locale.UiMessages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Página de resultados.
 *
 * <p>Un error de validación se muestra aquí, junto al campo, solo con su «qué pasó» ({@code summary()}) y
 * punto final. El código {@code EFX-*} va al log en {@code WARN}, no a la página.</p>
 */
@Controller
public class SearchController implements HtmlPage {

    private static final Logger LOG = LoggerFactory.getLogger(SearchController.class);

    private final SearchQueryMapper mapper;
    private final SearchService searches;
    private final ResultsPage pages;
    private final UiMessages messages;

    /**
     * Crea el controlador con la fachada de búsqueda y el catálogo de mensajes.
     *
     * @param mapper   URL a consulta
     * @param searches fachada
     * @param pages    modelo de la vista
     * @param messages catálogo de mensajes
     */
    public SearchController(
            SearchQueryMapper mapper,
            SearchService searches,
            ResultsPage pages,
            UiMessages messages) {
        this.mapper = mapper;
        this.searches = searches;
        this.pages = pages;
        this.messages = messages;
    }

    /**
     * Ejecuta la búsqueda o, si la URL no es válida, muestra qué pasó junto al campo y registra el código.
     *
     * @param params parámetros de §5
     * @param model  vista
     * @return plantilla {@code results}
     */
    @GetMapping("/buscar")
    public String search(@RequestParam MultiValueMap<String, String> params, Model model) {
        SearchForm form = SearchForms.from(params);
        model.addAttribute("nav", "buscar");
        model.addAttribute("form", form);
        show(model, form);
        return "results";
    }

    private void show(Model model, SearchForm form) {
        try {
            found(model, form);
        } catch (SearchValidationException exception) {
            LOG.warn("{}: la consulta no pasó la validación", exception.errorCode().code());
            pages.rejected(model, form, exception.errorCode() == ErrorCode.DATE_RANGE_REVERSED);
            // Junto al campo va solo el «qué pasó», con su punto final como en el mockup y en PLAN §5.
            model.addAttribute("errorSummary", messages.errorSummary(exception.errorCode()) + ".");
            model.addAttribute("queryError", exception.errorCode() == ErrorCode.EMPTY_SEARCH_TEXT);
        }
    }

    private void found(Model model, SearchForm form) {
        SearchQuery query = mapper.toQuery(form);
        pages.fill(model, form, searches.search(query), query);
    }
}

