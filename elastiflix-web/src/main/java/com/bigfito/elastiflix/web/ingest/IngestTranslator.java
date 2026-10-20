package com.bigfito.elastiflix.web.ingest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.text.Thousands;
import com.bigfito.elastiflix.ingest.csv.CsvColumnRow;
import com.bigfito.elastiflix.ingest.job.JobView;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.WarningCount;
import com.bigfito.elastiflix.web.locale.SupportedLocale;
import com.bigfito.elastiflix.web.locale.UiMessages;

import org.springframework.stereotype.Component;

/**
 * Traduce textos dinámicos de los trabajos de carga (JobView, columnas CSV, comprobaciones, advertencias e informe).
 */
@Component
public class IngestTranslator {

    /**
     * Traduce una vista de trabajo al idioma especificado.
     *
     * @param view     copia original del trabajo
     * @param locale   idioma solicitado
     * @param messages mensajes de error para fallos
     * @return vista traducida si el idioma es inglés, o la original
     */
    public JobView translateJob(JobView view, Locale locale, UiMessages messages) {
        if (view == null || !SupportedLocale.isEnglish(locale)) {
            return view;
        }
        return buildTranslatedJob(view, locale, messages);
    }

    private JobView buildTranslatedJob(JobView view, Locale locale, UiMessages messages) {
        List<CsvColumnRow> columns = translateColumns(view.columns());
        List<ValidationIssue> errors = translateErrors(view.errors());
        List<WarningCount> warnings = translateWarnings(view.warnings());
        List<String> checks = translateChecks(view.checks(), view.fileCount(), locale);
        String failure = translateFailure(view.failureCode(), view.failureMessage(), locale, messages);
        return new JobView(
                view.id(), view.status(), view.kind(), view.fileSize(),
                view.step(), view.batch(), view.vectorized(), view.written(),
                view.fileCount(), view.validCount(), view.invalidRecords(), view.replaceCount(),
                view.errorTotal(), errors, warnings, checks, columns,
                view.failureCode(), failure, view.batchSize(), view.batches(), view.tempIndex());
    }

    /**
     * Traduce una lista de filas de correspondencia de columnas CSV.
     *
     * @param columns lista original de columnas
     * @return lista traducida
     */
    public List<CsvColumnRow> translateColumns(List<CsvColumnRow> columns) {
        return CsvColumnTranslator.translateColumns(columns);
    }

    /**
     * Traduce los errores de validación.
     *
     * @param errors errores originales
     * @return lista de errores con encabezado y expectativa en inglés
     */
    public List<ValidationIssue> translateErrors(List<ValidationIssue> errors) {
        return ValidationIssueTranslator.translateErrors(errors);
    }

    /**
     * Traduce los grupos de advertencias.
     *
     * @param warnings advertencias originales
     * @return advertencias traducidas al inglés
     */
    public List<WarningCount> translateWarnings(List<WarningCount> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return List.of();
        }
        List<WarningCount> list = new ArrayList<>(warnings.size());
        for (WarningCount w : warnings) {
            String msg = "sin valor".equals(w.message()) ? "stored without a value"
                    : "se omitió".equals(w.message()) ? "skipped" : w.message();
            list.add(new WarningCount(w.code(), w.field(), w.count(), msg));
        }
        return List.copyOf(list);
    }

    /**
     * Traduce los textos de comprobaciones superadas.
     *
     * @param checks    comprobaciones originales
     * @param fileCount número de registros
     * @param locale    idioma
     * @return textos en inglés
     */
    public List<String> translateChecks(List<String> checks, int fileCount, Locale locale) {
        if (checks == null || checks.isEmpty()) {
            return List.of();
        }
        List<String> list = new ArrayList<>(checks.size());
        for (String c : checks) {
            if (c.contains("registros leídos")) {
                list.add(Thousands.of(fileCount, locale) + " records read, with id, title and plot.");
            } else if (c.contains("Ningún id repetido")) {
                list.add("No duplicate id.");
            } else {
                list.add(c);
            }
        }
        return List.copyOf(list);
    }

    private static String translateFailure(
            String code, String message, Locale locale, UiMessages messages) {
        if (code == null || code.isBlank()) {
            return "";
        }
        if (messages != null) {
            for (ErrorCode ec : ErrorCode.values()) {
                if (ec.code().equals(code)) {
                    return messages.errorSummary(ec, locale);
                }
            }
        }
        return message != null ? message : "";
    }

    /**
     * Traduce el informe CSV de errores.
     *
     * @param rawCsv texto CSV original
     * @param locale idioma solicitado
     * @param isCsv  verdadero si el archivo original era CSV
     * @return texto CSV con encabezados y etiquetas traducidas
     */
    public String toErrorCsv(String rawCsv, Locale locale, boolean isCsv) {
        if (rawCsv == null || rawCsv.isBlank() || !SupportedLocale.isEnglish(locale)) {
            return rawCsv;
        }
        String header = isCsv ? "row,id,field,received,expected\n" : "record,id,field,received,expected\n";
        String csv = rawCsv.replace("registro,id,campo,recibido,esperado\n", header)
                .replace("registro,id,campo,recibido,esperado\r\n", header);
        csv = csv.replace("\nencabezado,", "\nheader,")
                .replace("\nFila ", "\nRow ")
                .replace("\nRegistro ", "\nRecord ");
        return csv;
    }
}
