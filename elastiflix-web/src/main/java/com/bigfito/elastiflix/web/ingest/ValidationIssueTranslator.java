package com.bigfito.elastiflix.web.ingest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;

/**
 * Traduce incidencias y errores de validación de archivos al inglés.
 */
final class ValidationIssueTranslator {

    private static final String ROW_PREFIX_ES = "Fila ";
    private static final String RECORD_PREFIX_ES = "Registro ";

    private ValidationIssueTranslator() {
    }

    /**
     * Traduce los errores de validación.
     *
     * @param errors errores originales
     * @return lista de errores con encabezado y expectativa en inglés
     */
    static List<ValidationIssue> translateErrors(List<ValidationIssue> errors) {
        if (errors == null || errors.isEmpty()) {
            return List.of();
        }
        List<ValidationIssue> list = new ArrayList<>(errors.size());
        for (ValidationIssue issue : errors) {
            list.add(translateIssue(issue));
        }
        return List.copyOf(list);
    }

    private static ValidationIssue translateIssue(ValidationIssue issue) {
        String recordLabel = translateRecordLabel(issue.recordLabel());
        String expected = translateExpected(issue.expected());
        return new ValidationIssue(
                issue.code(), recordLabel, issue.id(), issue.field(), issue.received(), expected, issue.warning());
    }

    private static String translateRecordLabel(String label) {
        if (label == null) {
            return "";
        }
        String translated = label;
        if (label.startsWith(ROW_PREFIX_ES)) {
            translated = "Row " + label.substring(ROW_PREFIX_ES.length());
        } else if (label.startsWith(RECORD_PREFIX_ES)) {
            translated = "Record " + label.substring(RECORD_PREFIX_ES.length());
        } else if ("encabezado".equalsIgnoreCase(label)) {
            translated = "header";
        }
        return translated;
    }

    private static String translateExpected(String expected) {
        if (expected == null) {
            return "";
        }
        return switch (expected) {
            case "un texto" -> "text";
            case "un número entero" -> "integer";
            case "true o false" -> "boolean: true or false";
            case "una fecha yyyy-MM-dd" -> "date in yyyy-MM-dd format";
            case "sin valor" -> "without value";
            default -> translateComplexExpected(expected);
        };
    }

    private static String translateComplexExpected(String expected) {
        if (expected.toLowerCase(Locale.ROOT).contains("no existe en el mapping")) {
            return "That field doesn't exist in the index mapping";
        }
        return expected.replace("dense_vector con ", "dense_vector with ")
                .replace(" dimensiones", " dimensions")
                .replace("objeto JSON anidado", "nested JSON object")
                .replace("lista JSON de objetos anidados", "JSON list of nested objects");
    }
}
