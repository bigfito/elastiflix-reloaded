package com.bigfito.elastiflix.ingest.mapping;

import java.util.List;
import java.util.Set;

import com.bigfito.elastiflix.core.client.MovieJsonMapper;
import com.bigfito.elastiflix.core.error.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Un error de un subcampo lleva la ruta completa y no oculta la falta de {@code id}, {@code title} o
 * {@code plot}.
 */
class NestedPathTest {

    private static final JsonMapper MAPPER = MovieJsonMapper.create();

    private final MappingValidator validator = new MappingValidator();

    @Test
    @DisplayName("un id malo dentro de keywords no oculta que falta el id de la película")
    void shouldReportTheMissingIdWhenAKeywordIdIsWrong() {
        // when
        ValidationReport report = validate("""
                {"title":"T","plot":"P","keywords":[{"id":"x"}]}
                """);

        // then
        assertThat(report.errors())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .containsExactlyInAnyOrder(
                        tuple(ErrorCode.TYPE_MISMATCH.code(), "keywords.id"),
                        tuple(ErrorCode.REQUIRED_FIELD.code(), "id"));
    }

    @Test
    @DisplayName("un subcampo title en la colección no oculta que falta el title de la película")
    void shouldReportTheMissingTitleWhenTheCollectionHasATitle() {
        // when
        ValidationReport report = validate("""
                {"id":"1","plot":"P","belongs_to_collection":{"id":1,"title":"Saga"}}
                """);

        // then
        assertThat(report.errors())
                .extracting(ValidationIssue::code, ValidationIssue::field, ValidationIssue::received)
                .containsExactlyInAnyOrder(
                        tuple(ErrorCode.UNKNOWN_FIELD.code(), "belongs_to_collection.title",
                                "Un campo llamado belongs_to_collection.title"),
                        tuple(ErrorCode.REQUIRED_FIELD.code(), "title", "no vino"));
    }

    @Test
    @DisplayName("las advertencias de un subcampo también llevan la ruta")
    void shouldUseThePathInNestedWarnings() {
        // when
        ValidationReport report = validate("""
                {"id":"1","title":"T","plot":"P","keywords":[{"id":7,"name":"  "}]}
                """);

        // then
        assertThat(report.errors()).isEmpty();
        assertThat(report.warnings())
                .extracting(ValidationIssue::code, ValidationIssue::field)
                .containsExactly(tuple(ErrorCode.EMPTY_TEXT.code(), "keywords.name"));
    }

    private ValidationReport validate(String json) {
        return validator.validate(List.of(MAPPER.readTree(json)), Set.of()).report();
    }
}
