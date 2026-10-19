package com.bigfito.elastiflix.web.ingest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.bigfito.elastiflix.ingest.csv.CsvColumnRow;
import com.bigfito.elastiflix.ingest.csv.CsvColumnRows;
import com.bigfito.elastiflix.ingest.job.JobView;
import com.bigfito.elastiflix.core.text.Thousands;
import com.bigfito.elastiflix.ingest.mapping.WarningCount;

/**
 * Textos de «Cargar datos» para un trabajo, con las cifras ya escritas como en el mockup.
 *
 * <p>Se arman en el servidor para que una prueba MockMvc pueda compararlos palabra por palabra; el
 * JavaScript solo los pinta.</p>
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
    private static final String OF = " de ";
    private static final double MEGABYTE = 1024d * 1024d;
    private static final Set<String> VECTORS = Set.of("plot_256", "plot_512", "plot_1024");
    private static final String VECTOR_WARNING = "El archivo trae plot_256, plot_512 y plot_1024: "
            + "se ignorarán y se generarán con el servicio de inferencia.";

    /**
     * Arma los textos del trabajo.
     *
     * @param job   copia del trabajo
     * @param alias alias del catálogo
     * @return textos listos para pintar
     */
    public static JobTexts of(JobView job, String alias) {
        boolean csv = "csv".equals(job.kind());
        return new JobTexts(
                csv ? "Fila" : "Registro",
                Thousands.of(job.fileCount()),
                Thousands.of(job.validCount()),
                Thousands.of(job.replaceCount()),
                Thousands.of(job.errorTotal()),
                errorSummary(job),
                "Mostrando " + job.errors().size() + OF + Thousands.of(job.errorTotal()) + " errores.",
                importText(job, alias),
                progressText(job, alias),
                importLabel(job),
                csv ? mappingNote(job.columns()) : "",
                fileMeta(job, csv),
                failure(job),
                warnings(job.warnings()));
    }

    private static String errorSummary(JobView job) {
        String errors = plural(job.errorTotal(), "error", "errores");
        if (job.invalidRecords() == 0) {
            return errors;
        }
        return errors + " en " + plural(job.invalidRecords(), "registro", "registros");
    }

    private static String importText(JobView job, String alias) {
        if (INVALID.equals(job.status())) {
            return "La importación está bloqueada: el archivo no coincide con el mapping del índice "
                    + "y no se escribió ningún registro.";
        }
        return "Se importarán " + plural(job.fileCount(), "película", "películas")
                + " en " + plural(job.batches(), "lote", "lotes") + OF + job.batchSize()
                + " dentro de un índice temporal que empieza como copia del catálogo actual. "
                + "Al terminar se comprueba el conteo y el alias " + alias
                + " pasa al índice nuevo y el índice anterior se borra, en una sola operación. "
                + "Si Elasticsearch rechaza un lote o la comprobación falla, el índice temporal se borra "
                + "y el catálogo queda exactamente como estaba. "
                + "Los vectores de la sinopsis se generan para cada película con el servicio de inferencia "
                + "de Elasticsearch.";
    }

    private static String progressText(JobView job, String alias) {
        String done = Thousands.of(job.written()) + OF + Thousands.of(job.fileCount()) + " importadas";
        if (INVALID.equals(job.status())) {
            return done;
        }
        String temp = job.tempIndex().isEmpty() ? alias + "-AAAAMMDD-HHMMSS" : job.tempIndex();
        return done + " · lote " + job.batch() + OF + job.batches() + " · índice temporal " + temp;
    }

    private static String importLabel(JobView job) {
        if (INVALID.equals(job.status())) {
            return "Importación bloqueada";
        }
        return "Importar " + Thousands.of(job.fileCount()) + " registros";
    }

    private static String mappingNote(List<CsvColumnRow> columns) {
        List<String> unknown = new ArrayList<>();
        for (CsvColumnRow column : columns) {
            if (CsvColumnRows.UNKNOWN.equals(column.status())) {
                unknown.add(column.column());
            }
        }
        int total = columns.size();
        if (unknown.isEmpty()) {
            return total + OF + total + " columnas reconocidas: todas tienen el mismo nombre que un campo "
                    + "del índice y se asignaron automáticamente.";
        }
        String names = unknown.size() == 1
                ? "La columna " + unknown.getFirst() + " no existe"
                : "Las columnas " + String.join(", ", unknown) + " no existen";
        return (total - unknown.size()) + OF + total + " columnas asignadas. " + names
                + " en el mapping del índice, así que la carga no puede continuar.";
    }

    private static String fileMeta(JobView job, boolean csv) {
        String size = String.format(Locale.ROOT, "%.1f MB", job.fileSize() / MEGABYTE);
        if (job.fileCount() == 0) {
            return size;
        }
        if (csv) {
            return size + " · CSV · " + job.columns().size() + " columnas · " + Thousands.of(job.fileCount())
                    + " filas";
        }
        return size + " · " + Thousands.of(job.fileCount()) + " documentos";
    }

    private static String failure(JobView job) {
        if (job.failureCode() == null || job.failureCode().isBlank()) {
            return "";
        }
        if (job.failureMessage() == null || job.failureMessage().isBlank()) {
            return job.failureCode();
        }
        return job.failureCode() + " · " + job.failureMessage() + ".";
    }

    private static List<String> warnings(List<WarningCount> warnings) {
        List<String> lines = new ArrayList<>();
        boolean vectors = false;
        for (WarningCount warning : warnings) {
            if (VECTORS.contains(warning.field())) {
                vectors = true;
            } else {
                String times = warning.count() > 1 ? " (" + Thousands.of(warning.count()) + ")" : "";
                lines.add(warning.field() + ": " + warning.message() + times + ".");
            }
        }
        if (vectors) {
            lines.addFirst(VECTOR_WARNING);
        }
        return List.copyOf(lines);
    }

    private static String plural(int count, String one, String many) {
        return Thousands.of(count) + " " + (count == 1 ? one : many);
    }
}
