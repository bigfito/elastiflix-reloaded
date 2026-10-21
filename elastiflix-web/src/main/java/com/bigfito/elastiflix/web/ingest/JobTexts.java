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
 * @param replace       lo que la carga reemplaza: ids que ya están (append) o el catálogo entero (bulk)
 * @param replaceLabel  título de esa caja según el modo
 * @param errors        errores del informe
 * @param errorSummary  por ejemplo {@code 23 errores en 17 registros}
 * @param errorRange    por ejemplo {@code Mostrando 6 de 23 errores.}
 * @param importText    explicación de la importación según el modo, el aviso de bloqueo o, en append, cuántas se
 *                      agregaron al terminar o al parar
 * @param progressText  por ejemplo {@code 0 de 4 322 importadas · lote 0 de 44 · índice temporal …} (bulk) o
 *                      {@code 0 de 4 322 agregadas · lote 0 de 44 · índice vigente (alias …)} (append)
 * @param importLabel   texto del botón: reemplazar el catálogo (bulk) o agregar (append)
 * @param mappingNote   nota de la tabla de columnas; vacía en JSON
 * @param fileMeta      detalle de la ficha del archivo
 * @param failure       código y «qué pasó» de una falla, o vacío
 * @param warnings      advertencias, una línea por grupo: {@code campo: mensaje (N).}
 * @param columnTones   tono de cada columna del CSV, en orden: {@code ok}, {@code warn} o {@code error}
 */
public record JobTexts(
        String locationLabel,
        String read,
        String valid,
        String replace,
        String replaceLabel,
        String errors,
        String errorSummary,
        String errorRange,
        String importText,
        String progressText,
        String importLabel,
        String mappingNote,
        String fileMeta,
        String failure,
        List<String> warnings,
        List<String> columnTones) {

    private static final String INVALID = "invalido";
    private static final String APPEND = "append";
    private static final String APPENDED = "agregado";
    private static final Set<String> STOPPED = Set.of("fallido", "cancelado");
    private static final String MOVIES = "ingest.job.count.movie";
    private static final double MEGABYTE = 1024d * 1024d;
    private static final int IMPORT_STEP = 4;
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
                writer.msg(writer.append() ? "ingest.job.count.replace" : "ingest.job.count.replace.bulk"),
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
                writer.warnings(translator),
                writer.columnTones());
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

        boolean append() {
            return APPEND.equals(job.mode());
        }

        /** Append ya escribió o paró: lo que importa es cuántas quedaron en el catálogo. */
        boolean appendFinished() {
            return append() && job.step() >= IMPORT_STEP
                    && (APPENDED.equals(job.status()) || STOPPED.contains(job.status()));
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
            if (appendFinished()) {
                String key = APPENDED.equals(job.status())
                        ? "ingest.job.import.appended" : "ingest.job.import.appendStopped";
                return msg(key, number(job.written()), counted(job.fileCount(), MOVIES), alias);
            }
            return msg(append() ? "ingest.job.import.plan.append" : "ingest.job.import.plan",
                    counted(job.fileCount(), MOVIES),
                    counted(job.batches(), "ingest.job.count.batch"),
                    number(job.batchSize()),
                    alias);
        }

        String progressText(String alias) {
            String suffix = append() ? ".append" : "";
            String done = msg("ingest.job.progress.done" + suffix, number(job.written()), number(job.fileCount()));
            if (INVALID.equals(job.status())) {
                return done;
            }
            String target = append() || !job.tempIndex().isEmpty()
                    ? targetOf(alias) : alias + "-AAAAMMDD-HHMMSS";
            return msg("ingest.job.progress.running" + suffix, done,
                    number(job.batch()), number(job.batches()), target);
        }

        /** En append, el alias; en bulk, el índice temporal ya creado. */
        private String targetOf(String alias) {
            return append() ? alias : job.tempIndex();
        }

        String importLabel() {
            if (INVALID.equals(job.status())) {
                return msg("ingest.job.importLabel.blocked");
            }
            return msg(append() ? "ingest.job.importLabel.append" : "ingest.job.importLabel.ready",
                    number(job.fileCount()));
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

        List<String> columnTones() {
            List<String> tones = new ArrayList<>();
            for (CsvColumnRow column : job.columns()) {
                if (CsvColumnRows.ASSIGNED.equals(column.status())) {
                    tones.add("ok");
                } else if (CsvColumnRows.UNKNOWN.equals(column.status())) {
                    tones.add("error");
                } else {
                    tones.add("warn");
                }
            }
            return List.copyOf(tones);
        }

        private String line(WarningCount warning, String message) {
            if (warning.count() > 1) {
                return msg("ingest.job.warning.many", warning.field(), message, number(warning.count()));
            }
            return msg("ingest.job.warning.one", warning.field(), message);
        }
    }
}
