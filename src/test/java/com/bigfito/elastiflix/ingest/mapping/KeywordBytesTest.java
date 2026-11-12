package com.bigfito.elastiflix.ingest.mapping;

import java.util.List;
import java.util.Set;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.error.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Un valor que Elasticsearch rechazaría al escribir se rechaza ya en la validación, con {@code EFX-ING-012}.
 */
class KeywordBytesTest {

    private static final JsonMapper MAPPER = MovieJsonMapper.create();

    private final MappingValidator validator = new MappingValidator();

    @Test
    @DisplayName("un id de 513 bytes se rechaza y uno de 512 pasa")
    void shouldRejectAnIdLongerThan512Bytes() {
        // when
        ValidatedMovies tooLong = validate(movie("a".repeat(513)));
        ValidatedMovies longest = validate(movie("a".repeat(512)));

        // then
        assertSingleError(tooLong, "id", "un texto de máximo 512 bytes en UTF-8");
        assertThat(longest.report().errors()).isEmpty();
    }

    @Test
    @DisplayName("el tope del id cuenta bytes UTF-8, no caracteres")
    void shouldCountTheIdInUtf8Bytes() {
        // when: «ñ» ocupa 2 bytes
        ValidatedMovies tooLong = validate(movie("ñ".repeat(257)));
        ValidatedMovies longest = validate(movie("ñ".repeat(256)));

        // then
        assertSingleError(tooLong, "id", "un texto de máximo 512 bytes en UTF-8");
        assertThat(longest.report().errors()).isEmpty();
    }

    @Test
    @DisplayName("un keyword sin ignore_above de más de 32 766 bytes se rechaza")
    void shouldRejectAKeywordLongerThanALuceneTerm() {
        // given
        ObjectNode tooLong = movie("1").put("tagline", "a".repeat(KeywordBytes.MAX_TERM + 1));
        ObjectNode longest = movie("2").put("tagline", "a".repeat(KeywordBytes.MAX_TERM));

        // when
        ValidatedMovies checked = validator.validate(List.of(tooLong, longest), Set.of());

        // then
        assertThat(checked.report().errors()).singleElement().satisfies(issue -> {
            assertThat(issue.code()).isEqualTo(ErrorCode.TYPE_MISMATCH.code());
            assertThat(issue.id()).isEqualTo("1");
            assertThat(issue.field()).isEqualTo("tagline");
            assertThat(issue.expected()).isEqualTo("un texto de máximo 32 766 bytes en UTF-8");
            assertThat(issue.received()).hasSizeLessThanOrEqualTo(80);
        });
    }

    @Test
    @DisplayName("un elemento demasiado largo de una lista keyword se rechaza")
    void shouldRejectALongItemOfAKeywordList() {
        // given
        ObjectNode document = movie("1");
        document.putArray("genres").add("Drama").add("é".repeat(KeywordBytes.MAX_TERM / 2 + 1));

        // when
        ValidatedMovies checked = validate(document);

        // then
        assertSingleError(checked, "genres", "un texto de máximo 32 766 bytes en UTF-8");
    }

    @Test
    @DisplayName("un campo text, como plot, no tiene ese tope")
    void shouldAcceptALongTextField() {
        // given
        ObjectNode document = movie("1").put("plot", "a".repeat(KeywordBytes.MAX_TERM * 2));

        // then
        assertThat(validate(document).report().errors()).isEmpty();
    }

    @Test
    @DisplayName("la frase nueva es parte del vocabulario y se reconoce con su número")
    void shouldRecognizeTheNewPhrase() {
        PhraseMatch match = IngestPhrase.match("un texto de máximo 32 766 bytes en UTF-8").orElseThrow();

        assertThat(match.phrase()).isEqualTo(IngestPhrase.TEXT_UP_TO_BYTES);
        assertThat(match.args()).containsExactly(32_766L);
    }

    private ValidatedMovies validate(JsonNode document) {
        return validator.validate(List.of(document), Set.of());
    }

    private static ObjectNode movie(String id) {
        return MAPPER.createObjectNode().put("id", id).put("title", "T")
                .put("plot", "Sinopsis de prueba suficientemente larga para validar.");
    }

    private static void assertSingleError(ValidatedMovies checked, String field, String expected) {
        assertThat(checked.movies()).isEmpty();
        assertThat(checked.report().errors()).singleElement().satisfies(issue -> {
            assertThat(issue.code()).isEqualTo(ErrorCode.TYPE_MISMATCH.code());
            assertThat(issue.field()).isEqualTo(field);
            assertThat(issue.expected()).isEqualTo(expected);
        });
    }
}
