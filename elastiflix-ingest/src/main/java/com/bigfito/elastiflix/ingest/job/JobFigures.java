package com.bigfito.elastiflix.ingest.job;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.ingest.mapping.Thousands;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;

/**
 * Cifras que la página de carga muestra junto al paso.
 */
final class JobFigures {

    private static final int ERROR_PREVIEW = 6;

    private JobFigures() {
    }

    /**
     * Registros distintos con al menos un error. Los errores del encabezado o del archivo entero no
     * tienen número de registro y no cuentan.
     */
    static int invalidRecords(List<ValidationIssue> errors) {
        Set<String> records = new HashSet<>();
        for (ValidationIssue issue : errors) {
            if (isRecordNumber(issue.recordLabel())) {
                records.add(issue.recordLabel());
            }
        }
        return records.size();
    }

    static int validCount(int fileCount, int invalidRecords) {
        return Math.max(0, fileCount - invalidRecords);
    }

    static int batches(int fileCount, int batchSize) {
        if (fileCount <= 0 || batchSize <= 0) {
            return 0;
        }
        return (fileCount + batchSize - 1) / batchSize;
    }

    /**
     * Comprobaciones superadas. Solo existen cuando la validación terminó bien: en {@code validando} el
     * archivo todavía no se comparó con el mapping, y un {@code fallido} no se validó entero.
     */
    static List<String> checks(JobStatus status, int errors, int fileCount) {
        if (!passedValidation(status) || errors > 0 || fileCount == 0) {
            return List.of();
        }
        return List.of(
                Thousands.of(fileCount) + " registros leídos, con id, title y plot.",
                "Ningún id repetido.");
    }

    private static boolean passedValidation(JobStatus status) {
        return status == JobStatus.VALID || status == JobStatus.IMPORTING || status == JobStatus.PUBLISHED;
    }

    static List<ValidationIssue> preview(List<ValidationIssue> errors) {
        if (errors.size() <= ERROR_PREVIEW) {
            return errors;
        }
        return List.copyOf(new ArrayList<>(errors.subList(0, ERROR_PREVIEW)));
    }

    static String kindOf(String fileName) {
        return isCsv(fileName) ? "csv" : "json";
    }

    /** El tipo sale de la extensión del nombre original: la página y el código de lectura usan esta regla. */
    static boolean isCsv(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    /** Solo el «qué pasó»: la página ya antepone el código. */
    static String failureMessage(String failureCode) {
        if (failureCode == null || failureCode.isBlank()) {
            return "";
        }
        for (ErrorCode code : ErrorCode.values()) {
            if (code.code().equals(failureCode)) {
                return code.summary();
            }
        }
        return "";
    }

    static boolean isRecordNumber(String label) {
        if (label == null || label.isEmpty()) {
            return false;
        }
        for (int index = 0; index < label.length(); index++) {
            if (!Character.isDigit(label.charAt(index))) {
                return false;
            }
        }
        return true;
    }
}
