package com.bigfito.elastiflix.search.strategy;

import java.util.List;

import com.bigfito.elastiflix.core.model.Movie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El joiner de la híbrida: puntuación y posición de cada película en las listas léxica y k-NN.
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
        assertThat(inBoth.lexical().rank()).isEqualTo(1);
        assertThat(inBoth.vector().score()).isEqualTo(0.8);
        assertThat(inBoth.vector().rank()).isEqualTo(2);
        assertThat(inBoth.score()).isEqualTo(0.032);

        RankedHit lexicalMiss = joined.get(1);
        assertThat(lexicalMiss.lexical().score()).isNull();
        assertThat(lexicalMiss.lexical().isOutside()).isTrue();
        assertThat(lexicalMiss.lexical().k()).isEqualTo(K);
        assertThat(lexicalMiss.vector().rank()).isEqualTo(1);
        assertThat(lexicalMiss.vector().score()).isEqualTo(0.9);
    }

    @Test
    @DisplayName("conserva el orden y la puntuación RRF de la lista fusionada, aunque falte en las dos listas")
    void shouldKeepTheFusedOrderWhenTheMovieIsOutsideBothLists() {
        // given
        Movie first = movie("1");
        Movie second = movie("2");
        Movie third = movie("3");
        List<RankedHit> fused = List.of(
                RankedHit.scored(third, 0.05), RankedHit.scored(first, 0.03), RankedHit.scored(second, 0.01));

        // when
        List<RankedHit> joined = joiner.join(
                fused,
                List.of(RankedHit.scored(second, 7.0), RankedHit.scored(first, 6.0)),
                List.of(RankedHit.scored(first, 0.7)),
                K);

        // then
        assertThat(joined).extracting(hit -> hit.movie().id()).containsExactly("3", "1", "2");
        assertThat(joined).extracting(RankedHit::score).containsExactly(0.05, 0.03, 0.01);
        RankedHit outside = joined.getFirst();
        assertThat(outside.lexical()).isEqualTo(ComponentScore.outside(K));
        assertThat(outside.vector()).isEqualTo(ComponentScore.outside(K));
        assertThat(joined.get(1).lexical()).isEqualTo(new ComponentScore(6.0, 2, K));
        assertThat(joined.get(1).vector()).isEqualTo(new ComponentScore(0.7, 1, K));
        assertThat(joined.get(2).lexical()).isEqualTo(new ComponentScore(7.0, 1, K));
    }

    private static Movie movie(String id) {
        return new Movie(id, null, id, null, null, null, null, null, null, null,
                null, null, null, List.of(), null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
