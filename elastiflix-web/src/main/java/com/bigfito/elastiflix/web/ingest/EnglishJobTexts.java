package com.bigfito.elastiflix.web.ingest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.text.Thousands;
import com.bigfito.elastiflix.ingest.csv.CsvColumnRow;
import com.bigfito.elastiflix.ingest.csv.CsvColumnRows;
import com.bigfito.elastiflix.ingest.job.JobView;
import com.bigfito.elastiflix.ingest.mapping.WarningCount;
import com.bigfito.elastiflix.web.locale.UiMessages;

/**
 * Genera las cadenas formateadas en inglés para la vista de un trabajo de carga.
 */
final class EnglishJobTexts {

    private static final double MEGABYTE = 1024.0 * 1024.0;
    private static final String INVALID = "invalido";
    private static final String OF = " of ";
    private static final String VECTOR_WARNING =
            "The file contains plot_256, plot_512 and plot_1024: they will be ignored and generated "
                    + "with the inference service.";
    private static final Set<String> VECTORS = Set.of("plot_256", "plot_512", "plot_1024");

    private EnglishJobTexts() {
    }

    /**
     * Construye las cadenas en inglés para el trabajo dado.
     *
     * @param job      vista del trabajo
     * @param alias    nombre del alias del catálogo
     * @param locale   idioma activo (inglés)
     * @param messages proveedor de mensajes de error
     * @return instancia de {@link JobTexts} con textos en inglés
     */
    static JobTexts of(JobView job, String alias, Locale locale, UiMessages messages) {
        boolean csv = "csv".equals(job.kind());
        return new JobTexts(
                csv ? "Row" : "Record",
                Thousands.of(job.fileCount(), locale),
                Thousands.of(job.validCount(), locale),
                Thousands.of(job.replaceCount(), locale),
                Thousands.of(job.errorTotal(), locale),
                errorSummary(job, locale),
                errorRange(job, locale),
                importText(job, alias, locale),
                progressText(job, alias, locale),
                importLabel(job, locale),
                csv ? mappingNote(job.columns()) : "",
                fileMeta(job, csv, locale),
                failure(job, locale, messages),
                warnings(job.warnings(), locale));
    }

    static String errorSummary(JobView job, Locale locale) {
        String errors = plural(job.errorTotal(), "error", "errors", locale);
        if (job.invalidRecords() == 0) {
            return errors;
        }
        return errors + " in " + plural(job.invalidRecords(), "record", "records", locale);
    }

    static String errorRange(JobView job, Locale locale) {
        return "Showing " + job.errors().size() + OF + Thousands.of(job.errorTotal(), locale) + " errors.";
    }

    private static String importText(JobView job, String alias, Locale locale) {
        if (INVALID.equals(job.status())) {
            return "The import is blocked: the file doesn't match the index mapping and no records were written.";
        }
        return Thousands.of(job.fileCount(), locale) + " movies will be imported in "
                + plural(job.batches(), "batch", "batches", locale) + OF + job.batchSize()
                + " into a temporary index that starts as a copy of the current catalog. "
                + "When it finishes, the count is checked and the " + alias
                + " alias switches to the new index and the previous index is deleted, in a single operation. "
                + "If Elasticsearch rejects a batch or the check fails, the temporary index is deleted "
                + "and the catalog stays exactly as it was. "
                + "The plot vectors are generated for each movie with the Elasticsearch inference service.";
    }

    private static String progressText(JobView job, String alias, Locale locale) {
        if (INVALID.equals(job.status())) {
            return "0 of " + Thousands.of(job.fileCount(), locale) + " imported";
        }
        String temp = job.tempIndex().isEmpty() ? alias + "-AAAAMMDD-HHMMSS" : job.tempIndex();
        return Thousands.of(job.written(), locale) + OF + Thousands.of(job.fileCount(), locale)
                + " imported · batch " + job.batch() + OF + job.batches()
                + " · temporary index " + temp;
    }

    private static String importLabel(JobView job, Locale locale) {
        if (INVALID.equals(job.status())) {
            return "Import blocked";
        }
        return "Import " + Thousands.of(job.fileCount(), locale) + " records";
    }

    private static String mappingNote(List<CsvColumnRow> columns) {
        List<String> unknown = new ArrayList<>();
        for (CsvColumnRow column : columns) {
            if (CsvColumnRows.UNKNOWN.equals(column.status()) || "Not in the mapping".equals(column.status())) {
                unknown.add(column.column());
            }
        }
        int total = columns.size();
        if (unknown.isEmpty()) {
            return total + OF + total
                    + " columns recognized: they all have the same name as an index field and were mapped"
                    + " automatically.";
        }
        String names = unknown.size() == 1
                ? "The " + unknown.getFirst() + " column doesn't exist"
                : "The " + String.join(", ", unknown) + " columns don't exist";
        return (total - unknown.size()) + OF + total + " columns mapped. " + names
                + " in the index mapping, so the upload can't continue.";
    }

    private static String fileMeta(JobView job, boolean csv, Locale locale) {
        String size = String.format(Locale.ROOT, "%.1f MB", job.fileSize() / MEGABYTE);
        if (job.fileCount() == 0) {
            return size;
        }
        String count = Thousands.of(job.fileCount(), locale);
        if (csv) {
            return size + " · CSV · " + job.columns().size() + " columns · " + count + " rows";
        }
        return size + " · " + count + " documents";
    }

    private static String failure(JobView job, Locale locale, UiMessages messages) {
        if (job.failureCode() == null || job.failureCode().isBlank()) {
            return "";
        }
        String message = job.failureMessage();
        if (messages != null) {
            for (ErrorCode ec : ErrorCode.values()) {
                if (ec.code().equals(job.failureCode())) {
                    message = messages.errorSummary(ec, locale);
                    break;
                }
            }
        }
        if (message == null || message.isBlank()) {
            return job.failureCode();
        }
        return job.failureCode() + " · " + message + ".";
    }

    private static List<String> warnings(List<WarningCount> warnings, Locale locale) {
        List<String> lines = new ArrayList<>();
        boolean vectors = false;
        for (WarningCount warning : warnings) {
            if (VECTORS.contains(warning.field())) {
                vectors = true;
            } else {
                String times = warning.count() > 1 ? " (" + Thousands.of(warning.count(), locale) + ")" : "";
                lines.add(warning.field() + ": " + warning.message() + times + ".");
            }
        }
        if (vectors) {
            lines.addFirst(VECTOR_WARNING);
        }
        return List.copyOf(lines);
    }

    private static String plural(int count, String one, String many, Locale locale) {
        return Thousands.of(count, locale) + " " + (count == 1 ? one : many);
    }
}
