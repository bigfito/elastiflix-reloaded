package com.bigfito.elastiflix.ingest.mapping;


import com.bigfito.elastiflix.core.error.ErrorCode;

import tools.jackson.databind.JsonNode;

/**
 * Comprueba textos, números, booleanos y fechas.
 */
final class ScalarValues {

    private static final String TEXT = IngestPhrase.TEXT.text();
    private static final String INTEGER = IngestPhrase.INTEGER.text();

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
        if (KeywordBytes.exceeds(name, value)) {
            KeywordBytes.reject(name, value, scope);
            return;
        }
        if (FieldNames.ID.equals(name) && value != null && value.isIntegralNumber()) {
            scope.target().put(name, IdTexts.of(value));
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
        if (TextNormalizer.apply(name, value, scope) == TextNormalizer.Outcome.BLANK) {
            blankText(name, scope, required);
        }
    }

    private static void blankText(String name, CheckScope scope, boolean required) {
        if (required) {
            scope.error(ErrorCode.REQUIRED_FIELD, name, FieldNames.BLANK, TEXT);
            return;
        }
        scope.warning(ErrorCode.EMPTY_TEXT, name, FieldNames.BLANK, IngestPhrase.NO_VALUE.text());
    }

    private static void bool(String name, JsonNode value, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (value.isBoolean()) {
            scope.target().set(name, value);
            return;
        }
        scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(value), IngestPhrase.BOOLEAN.text());
    }

    private static void integer(String name, JsonNode value, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (!fitsInt(value)) {
            scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(value), INTEGER);
            return;
        }
        keepNumber(name, value, scope);
    }

    private static void decimal(String name, JsonNode value, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isNumber() || !Float.isFinite((float) value.doubleValue())) {
            scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(value), IngestPhrase.FINITE_FLOAT.text());
            return;
        }
        keepNumber(name, value, scope);
    }

    /** Un 0 en presupuesto, recaudación, duración o votos no es un dato: se omite. */
    private static void keepNumber(String name, JsonNode value, CheckScope scope) {
        if (FieldRules.unknownZero(name) && value.doubleValue() == 0d) {
            scope.warning(ErrorCode.ZERO_AS_NULL, name, "0", IngestPhrase.NO_VALUE.text());
            return;
        }
        scope.target().set(name, value);
    }

    private static void date(String name, JsonNode value, CheckScope scope) {
        if (value == null || value.isNull()) {
            return;
        }
        if (value.isString() && Dates.valid(value.asString())) {
            scope.target().set(name, value);
            return;
        }
        scope.error(ErrorCode.TYPE_MISMATCH, name, IssueTexts.show(value), IngestPhrase.DATE.text());
    }

    /** Un entero de verdad (ni {@code 1e3} ni {@code 12.0}) que cabe en un {@code int}, sin llamar a longValue. */
    private static boolean fitsInt(JsonNode value) {
        return value.isIntegralNumber() && value.canConvertToInt();
    }
}
