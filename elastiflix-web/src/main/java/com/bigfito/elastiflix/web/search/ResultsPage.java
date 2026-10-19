package com.bigfito.elastiflix.web.search;

import java.util.List;

import com.bigfito.elastiflix.core.text.Thousands;
import com.bigfito.elastiflix.search.page.Pagination;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.result.SearchResult;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * Pone en el modelo lo que la plantilla de resultados necesita.
 */
@Component
public class ResultsPage {

    private final ResultCards cards;

    /**
     * Crea la página con el armador de tarjetas.
     *
     * @param cards tarjetas
     */
    public ResultsPage(ResultCards cards) {
        this.cards = cards;
    }

    /**
     * Rellena el modelo con la consulta ya ejecutada.
     *
     * @param model  vista
     * @param form   URL actual
     * @param result página, facetas y estado vacío
     * @param query  modo y k
     */
    public void fill(Model model, SearchForm form, SearchResult result, SearchQuery query) {
        heading(model, form, result, query);
        filters(model, form, result);
        pages(model, form, result.pagination());
    }

    private void heading(Model model, SearchForm form, SearchResult result, SearchQuery query) {
        model.addAttribute("copy", ModeCopies.of(query));
        model.addAttribute("adjustments", result.adjustments());
        model.addAttribute("countTitle", countTitle(result.total()));
        model.addAttribute("cards", cards.of(result, query.mode(), form));
        model.addAttribute("empty", result.empty());
    }

    private static void applied(Model model, SearchForm form) {
        model.addAttribute("facetNote", AppliedFilters.note(form));
        model.addAttribute("chips", AppliedFilters.chips(form));
    }

    /**
     * Deja el formulario de filtros visible cuando la URL no se puede buscar.
     *
     * @param model     vista
     * @param form      URL actual
     * @param dateError si «Desde» es posterior a «Hasta»
     */
    public void rejected(Model model, SearchForm form, boolean dateError) {
        choices(model, FacetChoices.kept(form.genres()), FacetChoices.kept(form.languages()));
        model.addAttribute("ratings", FacetChoices.kept(form.ratings()));
        model.addAttribute("adultAll", 0);
        model.addAttribute("adultSafe", 0);
        model.addAttribute("adultYes", 0);
        adultChoice(model, form);
        model.addAttribute("dateNote", "En estos resultados: sin fechas de estreno.");
        model.addAttribute("clearHref", SearchLinks.cleared(form));
        model.addAttribute("dateError", dateError);
        applied(model, form);
    }

    private static void filters(Model model, SearchForm form, SearchResult result) {
        choices(model, FacetChoices.of(result.facets().genres(), form.genres()),
                FacetChoices.of(result.facets().spokenLanguages(), form.languages()));
        model.addAttribute("ratings", FacetChoices.of(result.facets().ratings(), form.ratings()));
        model.addAttribute("adultAll", FacetChoices.total(result.facets().adult()));
        model.addAttribute("adultSafe", FacetChoices.countOf(result.facets().adult(), "false"));
        model.addAttribute("adultYes", FacetChoices.countOf(result.facets().adult(), "true"));
        adultChoice(model, form);
        model.addAttribute("dateNote", dateNote(result));
        model.addAttribute("dateError", false);
        model.addAttribute("clearHref", SearchLinks.cleared(form));
        applied(model, form);
    }

    private static void choices(Model model, List<FacetChoice> genres, List<FacetChoice> languages) {
        model.addAttribute("genres", genres);
        model.addAttribute("genresExtra", FacetChoices.hasExtra(genres));
        model.addAttribute("languages", languages);
        model.addAttribute("languagesExtra", FacetChoices.hasExtra(languages));
    }

    /**
     * Qué opción de «Contenido para adultos» queda marcada.
     *
     * <p>Como {@code mk(...)} del mockup con las facetas: si la URL trae {@code adult=true}, la opción
     * «Solo adultos» aparece marcada, para que el siguiente APLICAR no quite el filtro sin avisar. Con
     * {@code false} y {@code true} a la vez gana {@code false}, igual que en el chip.</p>
     */
    private static void adultChoice(Model model, SearchForm form) {
        boolean safeOnly = form.adult().contains("false");
        model.addAttribute("safeOnly", safeOnly);
        model.addAttribute("adultOnly", !safeOnly && form.adult().contains("true"));
    }

    private static void pages(Model model, SearchForm form, Pagination pagination) {
        model.addAttribute("range", "Mostrando " + pagination.range() + " resultados");
        model.addAttribute("pages", PageButtons.of(form, pagination));
        model.addAttribute("previous", previous(form, pagination));
        model.addAttribute("next", next(form, pagination));
        model.addAttribute("size25", SearchLinks.size(form, SearchQuery.PAGE_25));
        model.addAttribute("size50", SearchLinks.size(form, SearchQuery.PAGE_50));
        model.addAttribute("size100", SearchLinks.size(form, SearchQuery.PAGE_100));
    }

    private static String countTitle(long total) {
        if (total == 1) {
            return "1 resultado";
        }
        return Thousands.of(total) + " resultados";
    }

    private static String dateNote(SearchResult result) {
        if (result.facets().earliestRelease() == null || result.facets().latestRelease() == null) {
            return "En estos resultados: sin fechas de estreno.";
        }
        return "En estos resultados: de " + result.facets().earliestRelease()
                + " a " + result.facets().latestRelease();
    }

    private static String previous(SearchForm form, Pagination pagination) {
        if (pagination.page() <= 1) {
            return "";
        }
        return SearchLinks.page(form, pagination.page() - 1);
    }

    private static String next(SearchForm form, Pagination pagination) {
        if (pagination.page() >= pagination.pageCount()) {
            return "";
        }
        return SearchLinks.page(form, pagination.page() + 1);
    }
}
