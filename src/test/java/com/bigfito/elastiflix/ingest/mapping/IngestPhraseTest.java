package com.bigfito.elastiflix.ingest.mapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El vocabulario de la carga reconoce cada frase que escribe, sin confundirla con otra ni con un dato.
 */
class IngestPhraseTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(IngestPhrase.class)
    @DisplayName("cada frase, con textos en sus marcadores, se reconoce como ella misma")
    void shouldRecognizeEveryPhraseAsItself(IngestPhrase phrase) {
        List<Object> args = new ArrayList<>();
        for (int index = 0; phrase.template().contains("{" + index + "}"); index++) {
            args.add("dato" + index);
        }

        Optional<PhraseMatch> match = IngestPhrase.match(phrase.text(args.toArray()));

        assertThat(match).isPresent();
        assertThat(match.get().phrase()).isEqualTo(phrase);
        assertThat(match.get().args()).isEqualTo(args);
    }

    @Test
    @DisplayName("no hay dos frases con la misma plantilla")
    void shouldNotRepeatATemplate() {
        Set<String> templates = new HashSet<>();
        for (IngestPhrase phrase : IngestPhrase.values()) {
            assertThat(templates.add(phrase.template())).as(phrase.name()).isTrue();
        }
    }

    @Test
    @DisplayName("un número vuelve como Long aunque la frase lo agrupe por miles")
    void shouldReturnNumbersAsLongs() {
        String text = IngestPhrase.EMPTY_IN_ROWS.text(IngestPhrase.KIND_TEXT.text(), 1234);

        assertThat(text).isEqualTo("Texto · vacía en 1 234 filas");
        PhraseMatch match = IngestPhrase.match(text).orElseThrow();
        assertThat(match.phrase()).isEqualTo(IngestPhrase.EMPTY_IN_ROWS);
        assertThat(match.args()).containsExactly("Texto", 1234L);
        assertThat(IngestPhrase.match(IngestPhrase.CELLS.text(12)).orElseThrow().args()).containsExactly(12L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Titanic", "1997-12-19", "{\"id\":1}", "plot", "42", "un texto más"})
    @DisplayName("un dato del archivo no es una frase")
    void shouldNotRecognizeData(String data) {
        assertThat(IngestPhrase.match(data)).isEmpty();
    }

    @Test
    @DisplayName("la clave es el nombre en minúsculas con puntos")
    void shouldBuildTheKeyFromTheName() {
        assertThat(IngestPhrase.EMPTY_IN_ROWS.key()).isEqualTo("empty.in.rows");
        assertThat(IngestPhrase.TEXT.key()).isEqualTo("text");
    }
}
