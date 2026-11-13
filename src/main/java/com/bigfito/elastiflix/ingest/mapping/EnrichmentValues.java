package com.bigfito.elastiflix.ingest.mapping;

import com.bigfito.elastiflix.core.error.ErrorCode;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Revisa {@code enrichment}: un objeto opcional de etiquetas keyword.
 *
 * <p>{@code null} y un objeto vacío no se copian. Una clave que el mapping no declara es
 * {@code EFX-ING-007}; un valor que no es texto, {@code EFX-ING-012}; un texto en blanco se omite
 * con {@code EFX-ING-022}.</p>
 */
final class EnrichmentValues {

    private static final String UNKNOWN = IngestPhrase.NOT_IN_MAPPING.text();

    private EnrichmentValues() {
    }

    static void write(String name, JsonNode value, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isObject()) {
            scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(value), IngestPhrase.OBJECT_OR_NULL.text());
            return;
        }
        ObjectNode clean = MovieNodes.MAPPER.createObjectNode();
        value.forEachEntry((key, child) -> copy(name, key, child, clean, scope));
        if (!clean.isEmpty()) {
            scope.target().set(name, clean);
        }
    }

    private static void copy(String parent, String key, JsonNode child, ObjectNode clean, CheckScope scope) {
        String path = parent + "." + key;
        if (!FieldRules.ENRICHMENT.contains(key)) {
            scope.error(ErrorCode.UNKNOWN_FIELD, path, IngestPhrase.FIELD_NAMED.text(path), UNKNOWN);
            return;
        }
        if (child != null && !child.isNull()) {
            keep(path, key, child, clean, scope);
        }
    }

    private static void keep(String path, String key, JsonNode child, ObjectNode clean, CheckScope scope) {
        if (!child.isString()) {
            scope.error(ErrorCode.TYPE_MISMATCH, path, IssueTexts.show(child), IngestPhrase.TEXT.text());
            return;
        }
        storeText(path, key, child, clean, scope);
    }

    private static void storeText(String path, String key, JsonNode child, ObjectNode clean, CheckScope scope) {
        String text = TextNormalizer.keep(path, child.asString(), scope);
        if (text.isBlank()) {
            scope.warning(ErrorCode.EMPTY_TEXT, path, FieldNames.BLANK, IngestPhrase.NO_VALUE.text());
            return;
        }
        if (KeywordBytes.exceeds(path, text)) {
            KeywordBytes.reject(path, child, scope);
            return;
        }
        clean.set(key, TextNormalizer.node(text));
    }
}
