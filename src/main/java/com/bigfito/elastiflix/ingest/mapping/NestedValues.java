package com.bigfito.elastiflix.ingest.mapping;

import java.util.LinkedHashSet;
import java.util.Set;

import com.bigfito.elastiflix.core.error.ErrorCode;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Reparte las listas de textos y comprueba la colección y las palabras clave.
 *
 * <p>Un error de un subcampo lleva la ruta completa en el campo, por ejemplo {@code keywords.id} o
 * {@code belongs_to_collection.title}. Así no se confunde con {@code id}, {@code title} o {@code plot} del
 * documento, y la falta de uno de esos tres se sigue informando.</p>
 */
final class NestedValues {

    private static final String UNKNOWN = IngestPhrase.NOT_IN_MAPPING.text();
    private static final String INTEGER = IngestPhrase.INTEGER.text();

    private NestedValues() {
    }

    static void write(FieldKind kind, String name, JsonNode value, CheckScope scope) {
        switch (kind) {
            case TEXT_LIST -> TextListValues.write(name, value, scope);
            case COLLECTION -> collection(name, value, scope);
            case KEYWORDS -> keywords(name, value, scope);
            case TAGS -> EnrichmentValues.write(name, value, scope);
            default -> { /* los escalares los revisa ScalarValues; un vector no se copia: se genera desde plot */ }
        }
    }

    private static void collection(String name, JsonNode value, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isObject()) {
            scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(value), IngestPhrase.OBJECT_OR_NULL.text());
            return;
        }
        scope.target().set(name, object(value, new NestedObject(name, FieldRules.COLLECTION), scope));
    }

    private static void keywords(String name, JsonNode value, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isArray()) {
            scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(value), IngestPhrase.ID_NAME_LIST.text());
            return;
        }
        scope.target().set(name, keywordList(name, value, scope));
    }

    private static ArrayNode keywordList(String name, JsonNode value, CheckScope scope) {
        ArrayNode clean = MovieNodes.MAPPER.createArrayNode();
        Set<String> seen = new LinkedHashSet<>();
        for (JsonNode item : value) {
            addKeyword(name, item, clean, seen, scope);
        }
        return clean;
    }

    private static void addKeyword(
            String name, JsonNode item, ArrayNode clean, Set<String> seen, CheckScope scope) {
        if (item == null || item.isNull()) {
            scope.warning(ErrorCode.EMPTY_LIST_VALUE, name, FieldNames.BLANK, IngestPhrase.SKIPPED.text());
            return;
        }
        if (!item.isObject()) {
            scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(item), IngestPhrase.ID_NAME_OBJECT.text());
            return;
        }
        keepKeyword(name, item, clean, seen, scope);
    }

    /** El duplicado se decide por {@code id}. Sin id no hay con qué comparar y se conserva. */
    private static void keepKeyword(
            String name, JsonNode item, ArrayNode clean, Set<String> seen, CheckScope scope) {
        ObjectNode built = object(item, new NestedObject(name, FieldRules.KEYWORD), scope);
        JsonNode id = built.get(FieldNames.ID);
        if (id != null && !seen.add(id.asString())) {
            scope.warning(ErrorCode.DUPLICATE_LIST_VALUE, name, id.asString(), IngestPhrase.SKIPPED.text());
            return;
        }
        clean.add(built);
    }

    private static ObjectNode object(JsonNode value, NestedObject target, CheckScope scope) {
        value.forEachEntry((name, child) -> copyChild(target, name, child, scope));
        return target.clean();
    }

    private static void copyChild(NestedObject target, String name, JsonNode child, CheckScope scope) {
        if (!target.allowed().contains(name)) {
            String path = target.path(name);
            scope.error(ErrorCode.UNKNOWN_FIELD, path, IngestPhrase.FIELD_NAMED.text(path), UNKNOWN);
            return;
        }
        if (FieldNames.ID.equals(name)) {
            writeId(target, child, scope);
            return;
        }
        optionalText(target, name, child, scope);
    }

    private static void writeId(NestedObject target, JsonNode value, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isIntegralNumber() || !value.canConvertToLong()) {
            scope.error(ErrorCode.TYPE_MISMATCH, target.path(FieldNames.ID), IssueTexts.show(value), INTEGER);
            return;
        }
        target.clean().set(FieldNames.ID, value);
    }

    private static void optionalText(NestedObject target, String name, JsonNode value, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isString()) {
            scope.error(ErrorCode.TYPE_MISMATCH, target.path(name), IssueTexts.show(value), IngestPhrase.TEXT.text());
            return;
        }
        keepOptional(target, name, value, scope);
    }

    private static void keepOptional(NestedObject target, String name, JsonNode value, CheckScope scope) {
        if (value.asString().isBlank()) {
            scope.warning(ErrorCode.EMPTY_TEXT, target.path(name), FieldNames.BLANK, IngestPhrase.NO_VALUE.text());
            return;
        }
        target.clean().set(name, value);
    }

    /**
     * Un objeto anidado mientras se revisa.
     *
     * @param parent  campo del documento del que cuelga, por ejemplo {@code keywords}
     * @param allowed subcampos que admite el mapping
     * @param clean   objeto nuevo, solo con lo que pasa la revisión
     */
    private record NestedObject(String parent, Set<String> allowed, ObjectNode clean) {

        NestedObject(String parent, Set<String> allowed) {
            this(parent, allowed, MovieNodes.MAPPER.createObjectNode());
        }

        /** Ruta del subcampo para el informe, por ejemplo {@code keywords.id}. */
        String path(String name) {
            return parent + "." + name;
        }
    }
}
