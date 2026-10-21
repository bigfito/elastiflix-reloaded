package com.bigfito.elastiflix.web.ingest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.text.Thousands;
import com.bigfito.elastiflix.ingest.csv.CsvColumnRow;
import com.bigfito.elastiflix.ingest.job.JobView;
import com.bigfito.elastiflix.ingest.mapping.IngestPhrase;
import com.bigfito.elastiflix.ingest.mapping.PhraseMatch;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.WarningCount;
import com.bigfito.elastiflix.web.locale.UiMessages;

import org.springframework.stereotype.Component;

/**
 * Pone en el idioma de la persona los textos que escribe la carga: errores, advertencias, columnas del CSV,
 * comprobaciones, la falla de un trabajo y el informe CSV.
 *
 * <p>Cada frase fija viene de {@link IngestPhrase}, el vocabulario de {@code elastiflix-ingest}, y se traduce
 * por su clave {@code ingest.phrase.*}. Un texto que no es una frase del vocabulario es un dato del archivo y
 * queda igual. Un número se agrupa por miles según el idioma.</p>
 */
@Component
public class IngestTranslator {

    private static final String PHRASE_KEY = "ingest.phrase.";

    private final UiMessages messages;

    /**
     * Crea el traductor.
     *
     * @param messages catálogo de mensajes
     */
    public IngestTranslator(UiMessages messages) {
        this.messages = messages;
    }

    /**
     * La vista del trabajo con sus textos en el idioma pedido. Los números y el estado no cambian.
     *
     * @param view   vista del trabajo, o {@code null}
     * @param locale idioma
     * @return la vista traducida, o {@code null}
     */
    public JobView job(JobView view, Locale locale) {
        if (view == null) {
            return null;
        }
        return new JobView(
                view.id(), view.status(), view.kind(), view.fileSize(),
                view.step(), view.batch(), view.vectorized(), view.written(),
                view.fileCount(), view.validCount(), view.invalidRecords(), view.replaceCount(),
                view.errorTotal(), issues(view.errors(), locale), warnings(view.warnings(), locale),
                texts(view.checks(), locale), columns(view.columns(), locale),
                view.failureCode(), failure(view.failureCode(), view.failureMessage(), locale),
                view.batchSize(), view.batches(), view.tempIndex());
    }

    /**
     * Errores o advertencias en el idioma pedido.
     *
     * @param issues lista de la carga o del registro individual
     * @param locale idioma
     * @return lista traducida
     */
    public List<ValidationIssue> issues(List<ValidationIssue> issues, Locale locale) {
        List<ValidationIssue> list = new ArrayList<>();
        for (ValidationIssue issue : issues == null ? List.<ValidationIssue>of() : issues) {
            list.add(issue(issue, locale));
        }
        return List.copyOf(list);
    }

    /**
     * Reescribe cada error del informe CSV en el idioma pedido.
     *
     * @param locale idioma
     * @return la función que traduce un error
     */
    public UnaryOperator<ValidationIssue> rewriter(Locale locale) {
        return issue -> issue(issue, locale);
    }

    /**
     * Encabezado del informe CSV en el idioma pedido.
     *
     * @param csv    {@code true} si el archivo era CSV (su primera columna es la fila)
     * @param locale idioma
     * @return primera fila del informe
     */
    public String reportHeader(boolean csv, Locale locale) {
        return messages.getFor(locale, csv ? "ingest.report.header.row" : "ingest.report.header.record");
    }

    /**
     * Un texto de la carga en el idioma pedido.
     *
     * @param raw    texto que escribió la carga
     * @param locale idioma
     * @return la frase traducida, o el mismo texto si es un dato
     */
    public String text(String raw, Locale locale) {
        return IngestPhrase.match(raw)
                .map(match -> phrase(match, locale))
                .orElse(raw == null ? "" : raw);
    }

    private ValidationIssue issue(ValidationIssue issue, Locale locale) {
        return new ValidationIssue(
                issue.code(),
                text(issue.recordLabel(), locale),
                issue.id(),
                text(issue.field(), locale),
                text(issue.received(), locale),
                expected(issue, locale),
                issue.warning());
    }

    private String expected(ValidationIssue issue, Locale locale) {
        for (ErrorCode code : ErrorCode.values()) {
            if (code.code().equals(issue.code()) && code.summary().equals(issue.expected())) {
                return messages.errorSummary(code, locale);
            }
            if (code.code().equals(issue.code()) && code.userMessage().equals(issue.expected())) {
                return messages.errorUserMessage(code, locale);
            }
        }
        return text(issue.expected(), locale);
    }

    private List<WarningCount> warnings(List<WarningCount> warnings, Locale locale) {
        List<WarningCount> list = new ArrayList<>();
        for (WarningCount warning : warnings == null ? List.<WarningCount>of() : warnings) {
            list.add(new WarningCount(
                    warning.code(), warning.field(), warning.count(), text(warning.message(), locale)));
        }
        return List.copyOf(list);
    }

    private List<CsvColumnRow> columns(List<CsvColumnRow> columns, Locale locale) {
        List<CsvColumnRow> list = new ArrayList<>();
        for (CsvColumnRow row : columns == null ? List.<CsvColumnRow>of() : columns) {
            list.add(new CsvColumnRow(row.column(), text(row.field(), locale),
                    text(row.content(), locale), text(row.status(), locale)));
        }
        return List.copyOf(list);
    }

    private List<String> texts(List<String> texts, Locale locale) {
        List<String> list = new ArrayList<>();
        for (String text : texts == null ? List.<String>of() : texts) {
            list.add(text(text, locale));
        }
        return List.copyOf(list);
    }

    private String failure(String code, String message, Locale locale) {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.code().equals(code)) {
                return messages.errorSummary(errorCode, locale);
            }
        }
        return message;
    }

    private String phrase(PhraseMatch match, Locale locale) {
        Object[] args = new Object[match.args().size()];
        for (int index = 0; index < args.length; index++) {
            Object arg = match.args().get(index);
            args[index] = arg instanceof Long number ? Thousands.of(number, locale) : text((String) arg, locale);
        }
        return messages.getFor(locale, PHRASE_KEY + match.phrase().key(), args);
    }
}
