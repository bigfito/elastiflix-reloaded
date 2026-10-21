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
 * Textos de «Cargar datos» para un trabajo, con las cifras ya escritas como en el mockup.
 *
 * <p>Se arman en el servidor, en el idioma de la persona y desde {@code messages_*.properties}, para que una
 * prueba MockMvc pueda compararlos palabra por palabra; el JavaScript solo los pinta.</p>
 *
 * @param locationLabel encabezado de la primera columna de errores: {@code Registro} o {@code Fila}
 * @param read          registros leídos
 * @param valid         registros válidos
 * @param replace       registros que reemplazarán existentes
 * @param errors        errores del informe
 * @param errorSummary  por ejemplo {@code 23 errores en 17 registros}
 * @param errorRange    por ejemplo {@code Mostrando 6 de 23 errores.}
 * @param importText    explicación de la importación, o el aviso de bloqueo
 * @param progressText  por ejemplo {@code 0 de 4 322 importadas · lote 0 de 44 · índice temporal …}
 * @param importLabel   texto del botón de importar
 * @param mappingNote   nota de la tabla de columnas; vacía en JSON
 * @param fileMeta      detalle de la ficha del archivo
 * @param failure       código y «qué pasó» de una falla, o vacío
 * @param warnings      advertencias, una línea por grupo: {@code campo: mensaje (N).}
 */
public record JobTexts(
        String locationLabel,
        String read,
        String valid,
        String replace,
        String errors,
        String errorSummary,
        String errorRange,
        String importText,
        String progressText,
        String importLabel,
        String mappingNote,
        String fileMeta,
        String failure,
        List<String> warnings) {

    private static final String INVALID = "invalido";
    private static final double MEGABYTE = 1024d * 1024d;
    private static final Set<String> VECTORS = Set.of("plot_256", "plot_512", "plot_1024");

    /**
     * Arma los textos del trabajo en el idioma pedido.
     *
     * @param job        vista del trabajo tal como la dio la carga, sin traducir
     * @param alias      alias del catálogo
     * @param locale     idioma
     * @param messages   catálogo de mensajes
     * @param translator frases de la carga en el idioma pedido
     * @return textos listos para pintar
     */
    public static JobTexts of(
            JobView job, String alias, Locale locale, UiMessages messages, IngestTranslator translator) {
        Writer writer = new Writer(job, locale, messages);
        boolean csv = "csv".equals(job.kind());
        return new JobTexts(
                writer.msg(csv ? "ingest.job.th.row" : "ingest.job.th.record"),
                writer.number(job.fileCount()),
                writer.number(job.validCount()),
                writer.number(job.replaceCount()),
                writer.number(job.errorTotal()),
                writer.errorSummary(),
                writer.msg("ingest.job.errorRange",
                        String.valueOf(job.errors().size()), writer.number(job.errorTotal())),
                writer.importText(alias),
                writer.progressText(alias),
                writer.importLabel(),
                csv ? writer.mappingNote() : "",
                writer.fileMeta(csv),
                writer.failure(),
                writer.warnings(translator));
    }

    /** Arma cada texto con el trabajo, el idioma y el catálogo. */
    private record Writer(JobView job, Locale locale, UiMessages messages) {

        String msg(String key, Object... args) {
            return messages.getFor(locale, key, args);
        }

        String number(long value) {
            return Thousands.of(value, locale);
        }

        String counted(long count, String key) {
            return msg(key + (count == 1 ? ".one" : ".many"), number(count));
        }

        String errorSummary() {
            String errors = counted(job.errorTotal(), "ingest.job.count.error");
            if (job.invalidRecords() == 0) {
                return errors;
            }
            return msg("ingest.job.errorSummary.inRecords", errors,
                    counted(job.invalidRecords(), "ingest.job.count.record"));
        }

        String importText(String alias) {
            if (INVALID.equals(job.status())) {
                return msg("ingest.job.import.blocked");
            }
            return msg("ingest.job.import.plan",
                    counted(job.fileCount(), "ingest.job.count.movie"),
                    counted(job.batches(), "ingest.job.count.batch"),
                    String.valueOf(job.batchSize()),
                    alias);
        }

        String progressText(String alias) {
            String done = msg("ingest.job.progress.done", number(job.written()), number(job.fileCount()));
            if (INVALID.equals(job.status())) {
                return done;
            }
            String temp = job.tempIndex().isEmpty() ? alias + "-AAAAMMDD-HHMMSS" : job.tempIndex();
            return msg("ingest.job.progress.running", done,
                    String.valueOf(job.batch()), String.valueOf(job.batches()), temp);
        }

        String importLabel() {
            if (INVALID.equals(job.status())) {
                return msg("ingest.job.importLabel.blocked");
            }
            return msg("ingest.job.importLabel.ready", number(job.fileCount()));
        }

        String mappingNote() {
            List<String> unknown = new ArrayList<>();
            for (CsvColumnRow column : job.columns()) {
                if (CsvColumnRows.UNKNOWN.equals(column.status())) {
                    unknown.add(column.column());
                }
            }
            String total = String.valueOf(job.columns().size());
            if (unknown.isEmpty()) {
                return msg("ingest.job.mapping.all", total);
            }
            String names = unknown.size() == 1
                    ? msg("ingest.job.mapping.unknown.one", unknown.getFirst())
                    : msg("ingest.job.mapping.unknown.many", String.join(", ", unknown));
            return msg("ingest.job.mapping.some",
                    String.valueOf(job.columns().size() - unknown.size()), total, names);
        }

        String fileMeta(boolean csv) {
            String size = String.format(Locale.ROOT, "%.1f MB", job.fileSize() / MEGABYTE);
            if (job.fileCount() == 0) {
                return size;
            }
            if (csv) {
                return msg("ingest.job.fileMeta.csv",
                        size, String.valueOf(job.columns().size()), number(job.fileCount()));
            }
            return msg("ingest.job.fileMeta.json", size, number(job.fileCount()));
        }

        String failure() {
            String code = job.failureCode();
            if (code == null || code.isBlank()) {
                return "";
            }
            for (ErrorCode errorCode : ErrorCode.values()) {
                if (errorCode.code().equals(code)) {
                    return msg("ingest.job.failure", code, messages.errorSummary(errorCode, locale));
                }
            }
            String message = job.failureMessage();
            return message == null || message.isBlank() ? code : msg("ingest.job.failure", code, message);
        }

        List<String> warnings(IngestTranslator translator) {
            List<String> lines = new ArrayList<>();
            boolean vectors = false;
            for (WarningCount warning : job.warnings()) {
                if (VECTORS.contains(warning.field())) {
                    vectors = true;
                } else {
                    lines.add(line(warning, translator.text(warning.message(), locale)));
                }
            }
            if (vectors) {
                lines.addFirst(msg("ingest.job.warning.vectors"));
            }
            return List.copyOf(lines);
        }

        private String line(WarningCount warning, String message) {
            if (warning.count() > 1) {
                return msg("ingest.job.warning.many", warning.field(), message, number(warning.count()));
            }
            return msg("ingest.job.warning.one", warning.field(), message);
        }
    }
}
