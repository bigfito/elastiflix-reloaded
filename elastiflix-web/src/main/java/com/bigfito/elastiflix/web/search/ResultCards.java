package com.bigfito.elastiflix.web.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bigfito.elastiflix.core.text.Thousands;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.result.SearchHit;
import com.bigfito.elastiflix.search.result.SearchResult;
import com.bigfito.elastiflix.search.strategy.ComponentScore;
import com.bigfito.elastiflix.web.layout.PosterUrls;
import com.bigfito.elastiflix.web.locale.SupportedLocale;
import com.bigfito.elastiflix.web.locale.UiMessages;

import org.springframework.stereotype.Component;

/**
 * Pasa los hits a tarjetas con la meta, los votos y las puntuaciones del modo según el idioma activo.
 */
@Component
public class ResultCards {

    private final PosterUrls posters;
    private final UiMessages messages;

    /**
     * Crea el armador con el CDN de pósters y el componente de mensajes.
     *
     * @param posters  URLs de TMDB
     * @param messages mensajes de interfaz
     */
    public ResultCards(PosterUrls posters, UiMessages messages) {
        this.posters = posters;
        this.messages = messages;
    }

    /**
     * Tarjetas de la página, con la posición absoluta y enlaces al detalle en el idioma actual.
     *
     * @param result resultado de la fachada
     * @param mode   modo, para elegir el panel de puntuación
     * @param form   formulario actual con los parámetros de búsqueda
     * @return tarjetas en orden
     */
    public List<ResultCard> of(SearchResult result, SearchMode mode, SearchForm form) {
        return of(result, mode, form, SupportedLocale.current());
    }

    /**
     * Tarjetas de la página, con la posición absoluta y enlaces al detalle en el idioma indicado.
     *
     * @param result resultado de la fachada
     * @param mode   modo, para elegir el panel de puntuación
     * @param form   formulario actual con los parámetros de búsqueda
     * @param locale idioma destino
     * @return tarjetas en orden
     */
    public List<ResultCard> of(SearchResult result, SearchMode mode, SearchForm form, Locale locale) {
        List<ResultCard> cards = new ArrayList<>();
        int rank = start(result);
        for (SearchHit hit : result.hits()) {
            cards.add(card(hit, rank, mode, form, locale));
            rank++;
        }
        return List.copyOf(cards);
    }

    /**
     * Tarjetas de la página, con la posición absoluta en el idioma actual.
     *
     * @param result resultado de la fachada
     * @param mode   modo, para elegir el panel de puntuación
     * @return tarjetas en orden
     */
    public List<ResultCard> of(SearchResult result, SearchMode mode) {
        return of(result, mode, null, SupportedLocale.current());
    }

    private static int start(SearchResult result) {
        int page = result.pagination().page();
        int size = result.pagination().pageSize();
        return (page - 1) * size + 1;
    }

    private ResultCard card(SearchHit hit, int rank, SearchMode mode, SearchForm form, Locale locale) {
        return new ResultCard(
                hit.id(),
                rank,
                text(hit.title()),
                meta(hit),
                text(hit.plot()),
                votes(hit, locale),
                posters.url(hit.posterPath()),
                scores(hit, mode, locale),
                rrf(hit, mode),
                SearchLinks.detail(hit.id(), form));
    }

    private List<ScoreLine> scores(SearchHit hit, SearchMode mode, Locale locale) {
        if (mode == SearchMode.HYBRID) {
            return List.of(lexical(hit.lexical(), locale), vector(hit.vector(), locale));
        }
        if (mode == SearchMode.VECTOR) {
            return List.of(new ScoreLine(messages.getFor(locale, "results.card.score.knn"), number(hit.score()), ""));
        }
        return List.of(new ScoreLine(messages.getFor(locale, "results.card.score.bm25"), number(hit.score()), ""));
    }

    private ScoreLine lexical(ComponentScore score, Locale locale) {
        return line(messages.getFor(locale, "results.card.score.bm25"), score, locale);
    }

    private ScoreLine vector(ComponentScore score, Locale locale) {
        return line(messages.getFor(locale, "results.card.score.knn"), score, locale);
    }

    private ScoreLine line(String label, ComponentScore score, Locale locale) {
        if (score == null) {
            return new ScoreLine(label, "—", "");
        }
        return new ScoreLine(label, number(score.score()), hashed(score.position(), locale));
    }

    private String hashed(String position, Locale locale) {
        if (position == null || position.isBlank()) {
            return "";
        }
        if (position.chars().allMatch(Character::isDigit)) {
            return "#" + position;
        }
        String prefix = "fuera de su top ";
        return position.startsWith(prefix)
                ? messages.getFor(locale, "results.card.score.outsideTop", position.substring(prefix.length()))
                : position;
    }

    private static String rrf(SearchHit hit, SearchMode mode) {
        if (mode != SearchMode.HYBRID) {
            return "";
        }
        return number(hit.rrf());
    }

    private static String meta(SearchHit hit) {
        List<String> parts = new ArrayList<>();
        add(parts, hit.originalTitle());
        add(parts, hit.releaseDate() == null ? "" : hit.releaseDate().toString());
        add(parts, hit.runtime() == null ? "" : hit.runtime() + " min");
        add(parts, String.join(", ", hit.genres()));
        return String.join(" · ", parts);
    }

    private static void add(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value);
        }
    }

    private String votes(SearchHit hit, Locale locale) {
        return oneDecimal(hit.voteAverage()) + " · " + count(hit.voteCount(), locale) + " "
                + messages.getFor(locale, "results.card.votes");
    }

    private static String oneDecimal(Double value) {
        if (value == null) {
            return "—";
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private static String count(Integer value, Locale locale) {
        if (value == null) {
            return "0";
        }
        return Thousands.of(value, locale);
    }

    private static String number(Double value) {
        if (value == null) {
            return "—";
        }
        return String.format(Locale.US, "%.4f", value);
    }

    private static String text(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }
}

