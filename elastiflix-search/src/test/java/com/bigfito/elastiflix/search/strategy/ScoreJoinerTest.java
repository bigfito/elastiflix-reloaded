package com.bigfito.elastiflix.search.strategy;

import java.util.List;

import com.bigfito.elastiflix.core.model.Movie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Posiciones de la híbrida y el sumando {@code 1 / (60 + posición)}.
 */
class ScoreJoinerTest {

    private static final int K = 10;

    private final ScoreJoiner joiner = new ScoreJoiner();

    @Test
    @DisplayName("suma las listas en las que la película aparece y deja vacía la que falta")
    void shouldDescribeAMissingListWhenTheMovieIsOutsideItsTop() {
        Movie first = movie("1");
        Movie second = movie("2");
        List<RankedHit> joined = joiner.join(
                List.of(RankedHit.scored(first, 0.032), RankedHit.scored(second, 0.016)),
                List.of(RankedHit.scored(first, 4.0)),
                List.of(RankedHit.scored(second, 0.9), RankedHit.scored(first, 0.8)),
                K);

        RankedHit inBoth = joined.getFirst();
        assertThat(inBoth.lexical().score()).isEqualTo(4.0);
        assertThat(inBoth.lexical().position()).isEqualTo("1");
        assertThat(inBoth.vector().score()).isEqualTo(0.8);
        assertThat(inBoth.vector().position()).isEqualTo("2");
        assertThat(inBoth.rrf()).isEqualTo(0.032);

        RankedHit lexicalMiss = joined.get(1);
        assertThat(lexicalMiss.lexical().score()).isNull();
        assertThat(lexicalMiss.lexical().position()).isEqualTo(Rrf.OUTSIDE_PREFIX + K);
        assertThat(lexicalMiss.vector().position()).isEqualTo("1");
        assertThat(lexicalMiss.vector().score()).isEqualTo(0.9);
    }

    private static Movie movie(String id) {
        return new Movie(id, null, id, null, null, null, null, null, null, null,
                null, null, null, List.of(), null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
