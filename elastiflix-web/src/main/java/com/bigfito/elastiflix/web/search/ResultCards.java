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

import org.springframework.stereotype.Component;

/**
 * Pasa los hits a tarjetas con la meta, los votos y las puntuaciones del modo.
 */
@Component
public class ResultCards {

    private final PosterUrls posters;

    /**
     * Crea el armador con el CDN de pósters.
     *
     * @param posters URLs de TMDB
     */
    public ResultCards(PosterUrls posters) {
        this.posters = posters;
    }

    /**
     * Tarjetas de la página, con la posición absoluta y enlaces al detalle.
     *
     * @param result resultado de la fachada
     * @param mode   modo, para elegir el panel de puntuación
     * @param form   formulario actual con los parámetros de búsqueda
     * @return tarjetas en orden
     */
    public List<ResultCard> of(SearchResult result, SearchMode mode, SearchForm form) {
        List<ResultCard> cards = new ArrayList<>();
        int rank = start(result);
        for (SearchHit hit : result.hits()) {
            cards.add(card(hit, rank, mode, form));
            rank++;
        }
        return List.copyOf(cards);
    }

    /**
     * Tarjetas de la página, con la posición absoluta.
     *
     * @param result resultado de la fachada
     * @param mode   modo, para elegir el panel de puntuación
     * @return tarjetas en orden
     */
    public List<ResultCard> of(SearchResult result, SearchMode mode) {
        return of(result, mode, null);
    }

    private static int start(SearchResult result) {
        int page = result.pagination().page();
        int size = result.pagination().pageSize();
        return (page - 1) * size + 1;
    }

    private ResultCard card(SearchHit hit, int rank, SearchMode mode, SearchForm form) {
        return new ResultCard(
                hit.id(),
                rank,
                text(hit.title()),
                meta(hit),
                text(hit.plot()),
                votes(hit),
                posters.url(hit.posterPath()),
                scores(hit, mode),
                rrf(hit, mode),
                SearchLinks.detail(hit.id(), form));
    }

    private static List<ScoreLine> scores(SearchHit hit, SearchMode mode) {
        if (mode == SearchMode.HYBRID) {
            return List.of(lexical(hit.lexical()), vector(hit.vector()));
        }
        if (mode == SearchMode.VECTOR) {
            return List.of(new ScoreLine("Similitud k-NN", number(hit.score()), ""));
        }
        return List.of(new ScoreLine("BM25", number(hit.score()), ""));
    }

    private static ScoreLine lexical(ComponentScore score) {
        return line("BM25", score);
    }

    private static ScoreLine vector(ComponentScore score) {
        return line("Similitud k-NN", score);
    }

    private static ScoreLine line(String label, ComponentScore score) {
        if (score == null) {
            return new ScoreLine(label, "—", "");
        }
        return new ScoreLine(label, number(score.score()), hashed(score.position()));
    }

    private static String hashed(String position) {
        if (position == null || position.isBlank()) {
            return "";
        }
        if (position.chars().allMatch(Character::isDigit)) {
            return "#" + position;
        }
        return position;
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

    private static String votes(SearchHit hit) {
        return oneDecimal(hit.voteAverage()) + " · " + count(hit.voteCount()) + " votos";
    }

    private static String oneDecimal(Double value) {
        if (value == null) {
            return "—";
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private static String count(Integer value) {
        if (value == null) {
            return "0";
        }
        return Thousands.of(value);
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
