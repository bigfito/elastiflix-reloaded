package com.bigfito.elastiflix.ingest.mapping;

import com.bigfito.elastiflix.core.error.ErrorCode;

import tools.jackson.databind.JsonNode;

/**
 * Comprueba textos, números, booleanos y fechas.
 */
final class ScalarValues {

    private static final String TEXT = "un texto";
    private static final String INTEGER = "un número entero";

    private ScalarValues() {
    }

    static void write(FieldKind kind, String name, JsonNode value, CheckScope scope) {
        switch (kind) {
            case TEXT -> text(name, value, scope, FieldRules.required(name));
            case KEYWORD -> keyword(name, value, scope);
            case BOOLEAN -> bool(name, value, scope);
            case INTEGER -> integer(name, value, scope);
            case DECIMAL -> decimal(name, value, scope);
            case DATE -> date(name, value, scope);
            default -> { /* las listas las revisa NestedValues */ }
        }
    }

    private static void keyword(String name, JsonNode value, CheckScope scope) {
        if (FieldNames.ID.equals(name) && value != null && value.isIntegralNumber()) {
            scope.target().put(name, Long.toString(value.longValue()));
            return;
        }
        text(name, value, scope, FieldRules.required(name));
    }

    private static void text(String name, JsonNode value, CheckScope scope, boolean required) {
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isString()) {
            scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(value), TEXT);
            return;
        }
        keepText(name, value, scope, required);
    }

    private static void keepText(String name, JsonNode value, CheckScope scope, boolean required) {
        if (!value.asString().isBlank()) {
            scope.target().set(name, value);
            return;
        }
        if (required) {
            scope.error(ErrorCode.REQUIRED_FIELD, name, FieldNames.BLANK, TEXT);
            return;
        }
        scope.warning(ErrorCode.EMPTY_TEXT, name, FieldNames.BLANK, "sin valor");
    }

    private static void bool(String name, JsonNode value, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (value.isBoolean()) {
            scope.target().set(name, value);
            return;
        }
        scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(value), "true o false");
    }

    private static void integer(String name, JsonNode value, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (value.isIntegralNumber() && fitsInt(value)) {
            scope.target().set(name, value);
            return;
        }
        scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(value), INTEGER);
    }

    private static void decimal(String name, JsonNode value, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (value.isNumber()) {
            scope.target().set(name, value);
            return;
        }
        scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(value), "un número");
    }

    private static void date(String name, JsonNode value, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (value.isString() && Dates.valid(value.asString())) {
            scope.target().set(name, value);
            return;
        }
        scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(value), "una fecha yyyy-MM-dd");
    }

    private static boolean fitsInt(JsonNode value) {
        long number = value.longValue();
        return number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE;
    }
}
