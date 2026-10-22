package com.bigfito.elastiflix.core.catalog;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El nombre de un índice nuevo lleva la fecha UTC y solo reconoce los de su alias.
 */
class IndexNamesTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T15:30:00Z"), ZoneOffset.UTC);
    private static final String ALIAS = "elastiflix-reloaded";

    private final IndexNames names = new IndexNames();

    @Test
    @DisplayName("el nombre usa la hora UTC del reloj")
    void shouldStampTheClockWhenTheIndexIsNamed() {
        assertThat(names.timestamped(ALIAS, CLOCK)).isEqualTo("elastiflix-reloaded-20261020-153000");
    }

    @Test
    @DisplayName("solo acepta el patrón del propio alias")
    void shouldRejectAForeignNameWhenCheckingOwnership() {
        assertThat(names.isOwnedBy(ALIAS, "elastiflix-reloaded-20261020-153000")).isTrue();
        assertThat(names.isOwnedBy(ALIAS, "otro-20261020-153000")).isFalse();
        assertThat(names.isOwnedBy(ALIAS, ALIAS)).isFalse();
        assertThat(names.isOwnedBy(ALIAS, "elastiflix-reloaded-20261020-153000-extra")).isFalse();
    }

    @Test
    @DisplayName("un índice fechado de otro alias que empieza igual no es del alias corto")
    void shouldRejectTheIndexOfAnotherAliasWhenTheyShareAPrefix() {
        // given: los dos nombres empiezan por <alias>- y el comodín <alias>-* los devuelve
        String itIndexOfTheCatalog = "elastiflix-reloaded-it-20261020-153000";
        String longerItAlias = "elastiflix-it-x-y-20261020-153000";

        // when / then
        assertThat(names.isOwnedBy(ALIAS, itIndexOfTheCatalog)).isFalse();
        assertThat(names.isOwnedBy("elastiflix-it-x", longerItAlias)).isFalse();
        assertThat(names.isOwnedBy("elastiflix-it-x-y", longerItAlias)).isTrue();
    }
}
