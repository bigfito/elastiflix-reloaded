package com.bigfito.elastiflix.web.search;

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
        model.addAttribute("cards", cards.of(result, query.mode()));
        model.addAttribute("empty", result.empty());
        model.addAttribute("clearHref", SearchLinks.cleared(form));
        model.addAttribute("changeHref", "/");
    }

    private static void filters(Model model, SearchForm form, SearchResult result) {
        model.addAttribute("genres", FacetChoices.of(result.facets().genres(), form.genres()));
        model.addAttribute("languages", FacetChoices.of(result.facets().spokenLanguages(), form.languages()));
        model.addAttribute("ratings", FacetChoices.of(result.facets().ratings(), form.ratings()));
        model.addAttribute("adultAll", FacetChoices.total(result.facets().adult()));
        model.addAttribute("adultSafe", FacetChoices.countOf(result.facets().adult(), "false"));
        model.addAttribute("safeOnly", form.adult().contains("false"));
        model.addAttribute("dateNote", dateNote(result));
    }

    private static void pages(Model model, SearchForm form, Pagination pagination) {
        model.addAttribute("range", pagination.range());
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
        return total + " resultados";
    }

    private static String dateNote(SearchResult result) {
        if (result.facets().earliestRelease() == null || result.facets().latestRelease() == null) {
            return "En estos resultados: sin fechas de estreno.";
        }
        return "En estos resultados: de " + result.facets().earliestRelease()
                + " a " + result.facets().latestRelease() + ".";
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
