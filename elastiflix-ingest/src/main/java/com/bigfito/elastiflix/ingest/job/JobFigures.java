package com.bigfito.elastiflix.ingest.job;

import java.util.List;

import com.bigfito.elastiflix.core.error.ErrorCode;

/**
 * Cifras que la página de carga muestra junto al paso.
 */
final class JobFigures {

    private static final int BATCH = 100;

    private JobFigures() {
    }

    static int validCount(int errors, int fileCount) {
        if (errors == 0 && fileCount > 0) {
            return fileCount;
        }
        return 0;
    }

    static int batches(int fileCount) {
        if (fileCount <= 0) {
            return 0;
        }
        return (fileCount + BATCH - 1) / BATCH;
    }

    static List<String> checks(int errors, int fileCount) {
        if (errors > 0 || fileCount == 0) {
            return List.of();
        }
        return List.of(
                fileCount + " registros leídos, con id, title y plot.",
                "Ningún id repetido.",
                "Las columnas de vector, si vienen, se ignoran y se generan desde plot.");
    }

    static String failureMessage(String failureCode) {
        if (failureCode == null || failureCode.isBlank()) {
            return "";
        }
        for (ErrorCode code : ErrorCode.values()) {
            if (code.code().equals(failureCode)) {
                return code.userMessage();
            }
        }
        return "";
    }
}
