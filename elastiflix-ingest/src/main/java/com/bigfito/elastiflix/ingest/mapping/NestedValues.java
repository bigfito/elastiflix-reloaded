package com.bigfito.elastiflix.ingest.mapping;

import java.util.Set;

import com.bigfito.elastiflix.core.error.ErrorCode;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Comprueba listas de textos, la colección y las palabras clave.
 */
final class NestedValues {

    private static final String UNKNOWN = "Ese campo no existe en el mapping del índice";
    private static final String INTEGER = "un número entero";

    private NestedValues() {
    }

    static void write(FieldKind kind, String name, JsonNode value, CheckScope scope) {
        switch (kind) {
            case TEXT_LIST -> textList(name, value, scope);
            case COLLECTION -> collection(name, value, scope);
            case KEYWORDS -> keywords(name, value, scope);
            default -> { /* los escalares los revisa ScalarValues */ }
        }
    }

    private static void textList(String name, JsonNode value, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isArray()) {
            scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(value), "una lista de textos");
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
            scope.warning(ErrorCode.EMPTY_LIST_VALUE, name, FieldNames.BLANK, "se omite");
            return;
        }
        if (!item.isString()) {
            scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(item), "un texto");
            return;
        }
        clean.add(item.asString());
    }

    private static void collection(String name, JsonNode value, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isObject()) {
            scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(value), "un objeto o null");
            return;
        }
        scope.target().set(name, object(value, FieldRules.COLLECTION, scope));
    }

    private static void keywords(String name, JsonNode value, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isArray()) {
            scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(value), "una lista de {id, name}");
            return;
        }
        scope.target().set(name, keywordList(value, scope));
    }

    private static ArrayNode keywordList(JsonNode value, CheckScope scope) {
        ArrayNode clean = MovieNodes.MAPPER.createArrayNode();
        for (JsonNode item : value) {
            addKeyword(item, clean, scope);
        }
        return clean;
    }

    private static void addKeyword(JsonNode item, ArrayNode clean, CheckScope scope) {
        if (item == null || item.isNull()) {
            scope.warning(ErrorCode.EMPTY_LIST_VALUE, "keywords", FieldNames.BLANK, "se omite");
            return;
        }
        if (!item.isObject()) {
            scope.error(ErrorCode.TYPE_MISMATCH, "keywords", IssueTexts.show(item), "un objeto {id, name}");
            return;
        }
        clean.add(object(item, FieldRules.KEYWORD, scope));
    }

    private static ObjectNode object(JsonNode value, Set<String> allowed, CheckScope scope) {
        ObjectNode clean = MovieNodes.MAPPER.createObjectNode();
        value.forEachEntry((name, child) -> copyChild(name, child, allowed, clean, scope));
        return clean;
    }

    private static void copyChild(
            String name, JsonNode child, Set<String> allowed, ObjectNode clean, CheckScope scope) {
        if (!allowed.contains(name)) {
            scope.error(ErrorCode.UNKNOWN_FIELD, name, "Un campo llamado " + name, UNKNOWN);
            return;
        }
        if (FieldNames.ID.equals(name)) {
            writeId(child, clean, scope);
            return;
        }
        optionalText(name, child, clean, scope);
    }

    private static void writeId(JsonNode value, ObjectNode clean, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isIntegralNumber()) {
            scope.error(ErrorCode.TYPE_MISMATCH, FieldNames.ID, IssueTexts.show(value), INTEGER);
            return;
        }
        clean.set(FieldNames.ID, value);
    }

    private static void optionalText(String name, JsonNode value, ObjectNode clean, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isString()) {
            scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(value), "un texto");
            return;
        }
        keepOptional(name, value, clean, scope);
    }

    private static void keepOptional(String name, JsonNode value, ObjectNode clean, CheckScope scope) {
        if (value.asString().isBlank()) {
            scope.warning(ErrorCode.EMPTY_TEXT, name, FieldNames.BLANK, "sin valor");
            return;
        }
        clean.set(name, value);
    }
}
