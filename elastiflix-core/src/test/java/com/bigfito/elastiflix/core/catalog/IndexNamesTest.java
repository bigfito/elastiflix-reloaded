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
}
