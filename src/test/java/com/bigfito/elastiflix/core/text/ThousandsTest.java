package com.bigfito.elastiflix.core.text;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Agrupado de miles con espacio en español y coma en inglés, como en los mockups.
 */
class ThousandsTest {

    @Test
    @DisplayName("agrupa de a tres desde la derecha")
    void shouldGroupDigitsByThreeFromTheRight() {
        assertThat(Thousands.of(4322)).isEqualTo("4 322");
        assertThat(Thousands.of(1_234_567)).isEqualTo("1 234 567");
    }

    @Test
    @DisplayName("las cifras de hasta tres dígitos y los negativos quedan bien")
    void shouldLeaveShortNumbersAloneAndKeepTheSign() {
        assertThat(Thousands.of(0)).isEqualTo("0");
        assertThat(Thousands.of(999)).isEqualTo("999");
        assertThat(Thousands.of(-6899)).isEqualTo("-6 899");
    }

    @Test
    @DisplayName("en inglés usa coma entre miles y en español usa espacio")
    void shouldUseCommaInEnglishAndSpaceInSpanish() {
        assertThat(Thousands.of(6899, Locale.ENGLISH)).isEqualTo("6,899");
        assertThat(Thousands.of(160_000_000, Locale.US)).isEqualTo("160,000,000");
        assertThat(Thousands.of(6899, Locale.of("es"))).isEqualTo("6 899");
        assertThat(Thousands.of(6899, null)).isEqualTo("6 899");
    }
}
