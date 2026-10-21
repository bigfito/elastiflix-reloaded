package com.bigfito.elastiflix.ingest.mapping;

import com.bigfito.elastiflix.core.error.ErrorCode;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

/**
 * Comprueba las listas de textos: {@code genres}, {@code cast}, {@code production_companies},
 * {@code production_countries} y {@code spoken_languages}.
 */
final class TextListValues {

    private TextListValues() {
    }

    static void write(String name, JsonNode value, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isArray()) {
            scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(value), IngestPhrase.TEXT_LIST.text());
            return;
        }
        scope.target().set(name, texts(name, value, scope));
    }

    private static ArrayNode texts(String name, JsonNode value, CheckScope scope) {
        ArrayNode clean = MovieNodes.MAPPER.createArrayNode();
        for (JsonNode item : value) {
            addText(name, item, clean, scope);
        }
        return clean;
    }

    private static void addText(String name, JsonNode item, ArrayNode clean, CheckScope scope) {
        if (item == null || item.isNull() || item.isString() && item.asString().isBlank()) {
            scope.warning(ErrorCode.EMPTY_LIST_VALUE, name, FieldNames.BLANK, IngestPhrase.SKIPPED.text());
            return;
        }
        if (!item.isString()) {
            scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(item), IngestPhrase.TEXT.text());
            return;
        }
        keepText(name, item, clean, scope);
    }

    /** Las listas de textos son {@code keyword} sin {@code ignore_above}: cada texto tiene tope de bytes. */
    private static void keepText(String name, JsonNode item, ArrayNode clean, CheckScope scope) {
        if (KeywordBytes.exceeds(name, item)) {
            KeywordBytes.reject(name, item, scope);
            return;
        }
        clean.add(item.asString());
    }
}
