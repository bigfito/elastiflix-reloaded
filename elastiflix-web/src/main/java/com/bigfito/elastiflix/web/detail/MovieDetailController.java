package com.bigfito.elastiflix.web.detail;

import java.util.Optional;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.SearchValidationException;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.query.SearchQueryValidator;
import com.bigfito.elastiflix.search.result.SearchService;
import com.bigfito.elastiflix.web.layout.HtmlPage;
import com.bigfito.elastiflix.web.search.SearchForm;
import com.bigfito.elastiflix.web.search.SearchForms;
import com.bigfito.elastiflix.web.search.SearchQueryMapper;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controlador de la página de detalle de película ({@code GET /pelicula/{id}}).
 *
 * <p>Exige que la persona provenga de una búsqueda válida con el parámetro {@code q}.
 * Si el acceso es directo o con una consulta inválida, responde error controlado 400
 * con el código {@code EFX-SRCH-012}. Si la película no existe en el catálogo, responde 404.</p>
 */
@Controller
public class MovieDetailController implements HtmlPage {

    private static final Logger LOG = LoggerFactory.getLogger(MovieDetailController.class);

    private final SearchService searchService;
    private final SearchQueryMapper mapper;
    private final SearchQueryValidator validator;
    private final MovieDetailPage detailPage;

    /**
     * Crea el controlador de detalle.
     *
     * @param searchService búsqueda y lectura por id
     * @param mapper        traductor de formulario a consulta
     * @param detailPage    ensamblador del modelo de vista
     */
    public MovieDetailController(SearchService searchService, SearchQueryMapper mapper, MovieDetailPage detailPage) {
        this.searchService = searchService;
        this.mapper = mapper;
        this.validator = new SearchQueryValidator();
        this.detailPage = detailPage;
    }

    /**
     * Muestra la ficha técnica completa de una película conservando el estado de búsqueda previo.
     *
     * @param id       identificador de la película
     * @param params   parámetros de consulta en la URL
     * @param response respuesta HTTP para fijar el estado
     * @param model    modelo de la vista
     * @return plantilla {@code detail}, {@code not-found} o {@code error}
     */
    @GetMapping("/pelicula/{id}")
    public String detail(
            @PathVariable("id") String id,
            @RequestParam MultiValueMap<String, String> params,
            HttpServletResponse response,
            Model model) {

        SearchForm form = validForm(id, params);
        if (form == null) {
            return directDetailError(response, model);
        }

        Optional<Movie> movie = searchService.findById(id);
        if (movie.isEmpty()) {
            LOG.info("Película no encontrada con id: {}", id);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("nav", "");
            return "not-found";
        }

        model.addAttribute("nav", "buscar");
        model.addAttribute("detail", detailPage.build(movie.get(), form));
        return "detail";
    }

    private SearchForm validForm(String id, MultiValueMap<String, String> params) {
        String queryParam = params.getFirst("q");
        if (queryParam == null || queryParam.isBlank()) {
            LOG.warn("{}: intento de acceso al detalle de la película {} sin búsqueda previa",
                    ErrorCode.DIRECT_DETAIL_WITHOUT_QUERY.code(), id);
            return null;
        }

        SearchForm form = SearchForms.from(params);
        try {
            SearchQuery query = mapper.toQuery(form);
            validator.validate(query);
            return form;
        } catch (SearchValidationException exception) {
            LOG.warn("{}: parámetros de búsqueda inválidos al acceder al detalle de la película {}",
                    ErrorCode.DIRECT_DETAIL_WITHOUT_QUERY.code(), id);
            return null;
        }
    }

    private static String directDetailError(HttpServletResponse response, Model model) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        model.addAttribute("nav", "");
        model.addAttribute("code", ErrorCode.DIRECT_DETAIL_WITHOUT_QUERY.code());
        model.addAttribute("message", ErrorCode.DIRECT_DETAIL_WITHOUT_QUERY.summary() + ".");
        return "error";
    }
}
