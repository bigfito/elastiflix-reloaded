package com.bigfito.elastiflix.web.search;

import java.util.List;
import java.util.Locale;

import com.bigfito.elastiflix.core.text.Thousands;
import com.bigfito.elastiflix.search.page.Pagination;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.result.EmptyState;
import com.bigfito.elastiflix.search.result.SearchResult;
import com.bigfito.elastiflix.web.locale.SupportedLocale;
import com.bigfito.elastiflix.web.locale.UiMessages;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * Pone en el modelo lo que la plantilla de resultados necesita, adaptado al idioma activo.
 */
@Component
public class ResultsPage {

    private final ResultCards cards;
    private final UiMessages messages;

    /**
     * Crea la página con el armador de tarjetas y el componente de mensajes.
     *
     * @param cards    tarjetas
     * @param messages catálogo de mensajes
     */
    public ResultsPage(ResultCards cards, UiMessages messages) {
        this.cards = cards;
        this.messages = messages;
    }

    /**
     * Rellena el modelo con la consulta ya ejecutada en el idioma actual.
     *
     * @param model  vista
     * @param form   URL actual
     * @param result página, facetas y estado vacío
     * @param query  modo y k
     */
    public void fill(Model model, SearchForm form, SearchResult result, SearchQuery query) {
        fill(model, form, result, query, SupportedLocale.current());
    }

    /**
     * Rellena el modelo con la consulta ya ejecutada en el idioma indicado.
     *
     * @param model  vista
     * @param form   URL actual
     * @param result página, facetas y estado vacío
     * @param query  modo y k
     * @param locale idioma destino
     */
    public void fill(Model model, SearchForm form, SearchResult result, SearchQuery query, Locale locale) {
        heading(model, form, result, query, locale);
        filters(model, form, result, locale);
        pages(model, form, result.pagination(), locale);
    }

    private void heading(Model model, SearchForm form, SearchResult result, SearchQuery query, Locale locale) {
        model.addAttribute("copy", ModeCopies.of(query, locale, messages));
        model.addAttribute("adjustments", result.adjustments());
        model.addAttribute("countTitle", countTitle(result.total(), locale));
        model.addAttribute("cards", cards.of(result, query.mode(), form, locale));
        model.addAttribute("empty", emptyView(result.empty(), form.query(), query.mode(), locale));
    }

    private void applied(Model model, SearchForm form, Locale locale) {
        model.addAttribute("facetNote", AppliedFilters.note(form, locale, messages));
        model.addAttribute("chips", AppliedFilters.chips(form, locale, messages));
    }

    /**
     * Deja el formulario de filtros visible cuando la URL no se puede buscar.
     *
     * @param model     vista
     * @param form      URL actual
     * @param dateError si «Desde» es posterior a «Hasta»
     */
    public void rejected(Model model, SearchForm form, boolean dateError) {
        rejected(model, form, dateError, SupportedLocale.current());
    }

    /**
     * Deja el formulario de filtros visible cuando la URL no se puede buscar en el idioma indicado.
     *
     * @param model     vista
     * @param form      URL actual
     * @param dateError si «Desde» es posterior a «Hasta»
     * @param locale    idioma destino
     */
    public void rejected(Model model, SearchForm form, boolean dateError, Locale locale) {
        choices(model, FacetChoices.kept(form.genres()), FacetChoices.kept(form.languages()));
        model.addAttribute("ratings", FacetChoices.kept(form.ratings()));
        model.addAttribute("adultAll", 0);
        model.addAttribute("adultSafe", 0);
        model.addAttribute("adultYes", 0);
        adultChoice(model, form);
        model.addAttribute("dateNote", messages.getFor(locale, "results.filter.date.note.none"));
        model.addAttribute("clearHref", SearchLinks.cleared(form));
        model.addAttribute("dateError", dateError);
        applied(model, form, locale);
    }

    private void filters(Model model, SearchForm form, SearchResult result, Locale locale) {
        choices(model, FacetChoices.of(result.facets().genres(), form.genres()),
                FacetChoices.of(result.facets().spokenLanguages(), form.languages()));
        model.addAttribute("ratings", FacetChoices.of(result.facets().ratings(), form.ratings()));
        model.addAttribute("adultAll", FacetChoices.total(result.facets().adult()));
        model.addAttribute("adultSafe", FacetChoices.countOf(result.facets().adult(), "false"));
        model.addAttribute("adultYes", FacetChoices.countOf(result.facets().adult(), "true"));
        adultChoice(model, form);
        model.addAttribute("dateNote", dateNote(result, locale));
        model.addAttribute("dateError", false);
        model.addAttribute("clearHref", SearchLinks.cleared(form));
        applied(model, form, locale);
    }

    private static void choices(Model model, List<FacetChoice> genres, List<FacetChoice> languages) {
        model.addAttribute("genres", genres);
        model.addAttribute("genresExtra", FacetChoices.hasExtra(genres));
        model.addAttribute("languages", languages);
        model.addAttribute("languagesExtra", FacetChoices.hasExtra(languages));
    }

    private static void adultChoice(Model model, SearchForm form) {
        boolean safeOnly = form.adult().contains("false");
        model.addAttribute("safeOnly", safeOnly);
        model.addAttribute("adultOnly", !safeOnly && form.adult().contains("true"));
    }

    private void pages(Model model, SearchForm form, Pagination pagination, Locale locale) {
        model.addAttribute("range", rangeText(pagination, locale));
        model.addAttribute("pages", PageButtons.of(form, pagination));
        model.addAttribute("previous", previous(form, pagination));
        model.addAttribute("next", next(form, pagination));
        model.addAttribute("size25", SearchLinks.size(form, SearchQuery.PAGE_25));
        model.addAttribute("size50", SearchLinks.size(form, SearchQuery.PAGE_50));
        model.addAttribute("size100", SearchLinks.size(form, SearchQuery.PAGE_100));
    }

    private String countTitle(long total, Locale locale) {
        if (total == 0) {
            return messages.getFor(locale, "results.count.zero");
        }
        if (total == 1) {
            return messages.getFor(locale, "results.count.singular");
        }
        return messages.getFor(locale, "results.count.plural", Thousands.of(total, locale));
    }

    private String dateNote(SearchResult result, Locale locale) {
        if (result.facets().earliestRelease() == null || result.facets().latestRelease() == null) {
            return messages.getFor(locale, "results.filter.date.note.none");
        }
        return messages.getFor(
                locale,
                "results.filter.date.note.range",
                result.facets().earliestRelease(),
                result.facets().latestRelease());
    }

    private String rangeText(Pagination pagination, Locale locale) {
        String span = messages.getFor(locale, "results.pager.span",
                Thousands.of(pagination.first(), locale),
                Thousands.of(pagination.last(), locale),
                Thousands.of(pagination.total(), locale));
        return messages.getFor(locale, "results.pager.range", span);
    }

    private EmptyView emptyView(EmptyState raw, String query, SearchMode mode, Locale locale) {
        if (raw == null) {
            return null;
        }
        if (raw == EmptyState.FILTERED) {
            return new EmptyView(
                    messages.getFor(locale, "results.empty.filtered.title"),
                    messages.getFor(locale, "results.empty.filtered.text"),
                    List.of(
                            messages.getFor(locale, "results.empty.filtered.tip1"),
                            messages.getFor(locale, "results.empty.filtered.tip2")),
                    true);
        }
        List<String> tips = mode == SearchMode.FULL_TEXT
                ? List.of(
                        messages.getFor(locale, "results.empty.none.fulltext.tip1"),
                        messages.getFor(locale, "results.empty.none.fulltext.tip2"),
                        messages.getFor(locale, "results.empty.none.fulltext.tip3"))
                : List.of(
                        messages.getFor(locale, "results.empty.none.other.tip1"),
                        messages.getFor(locale, "results.empty.none.other.tip2"));
        return new EmptyView(
                messages.getFor(locale, "results.empty.none.title"),
                messages.getFor(locale, "results.empty.none.text", query),
                tips,
                false);
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

    /**
     * Modelo adaptado de estado vacío para la plantilla de resultados.
     *
     * @param title     título
     * @param text      explicación
     * @param tips      consejos
     * @param byFilters si se debe al filtrado
     */
    public record EmptyView(String title, String text, List<String> tips, boolean byFilters) {
    }
}

