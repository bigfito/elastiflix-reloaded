package com.bigfito.elastiflix.ingest.job;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.csv.CsvColumnRow;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;
import com.bigfito.elastiflix.ingest.mapping.ValidatedMovies;

/**
 * Un trabajo de esta JVM. Lo cambia el hilo de la carga y lo lee la página.
 */
final class ImportWork {

    private final String id;
    private final Path path;
    private final String fileName;
    private final CsvSettings settings;
    private final long fileSize;
    private final AtomicBoolean cancelled = new AtomicBoolean();

    private JobStatus status = JobStatus.RECEIVED;
    private KeptReport kept = KeptReport.EMPTY;
    private List<Movie> movies = List.of();
    private List<CsvColumnRow> columns = List.of();
    private int fileCount;
    private int replaceCount;
    private int step = JobSteps.RECEIVED;
    private int batch;
    private int vectorized;
    private int written;
    private String failureCode = "";
    private String tempIndex = "";

    /**
     * Abre el trabajo en {@code recibido}.
     *
     * @param id       identificador
     * @param fileName nombre original, sin ruta
     * @param path     archivo local
     * @param settings separador y encabezado; en JSON no se usan
     * @param fileSize bytes del archivo subido
     */
    ImportWork(String id, String fileName, Path path, CsvSettings settings, long fileSize) {
        this.id = id;
        this.fileName = fileName;
        this.path = path;
        this.settings = settings;
        this.fileSize = fileSize;
    }

    String id() {
        return id;
    }

    Path path() {
        return path;
    }

    String fileName() {
        return fileName;
    }

    CsvSettings settings() {
        return settings;
    }

    synchronized JobStatus status() {
        return status;
    }

    /** Informe guardado: los primeros errores y las cifras del informe entero. */
    synchronized KeptReport kept() {
        return kept;
    }

    synchronized List<Movie> movies() {
        return movies;
    }

    /**
     * Suelta las películas. Los conteos se quedan para la página.
     */
    synchronized void release() {
        movies = List.of();
    }

    /**
     * Conteos para {@link ImportJob}. El lote lo pone quien llama.
     *
     * @param batchSize películas por lote
     * @return conteos de este archivo
     */
    synchronized ImportCounts counts(int batchSize) {
        return new ImportCounts(fileCount, batchSize);
    }

    boolean isCancelled() {
        return cancelled.get();
    }

    synchronized void validating() {
        if (status == JobStatus.CANCELLED) {
            return;
        }
        status = JobStatus.VALIDATING;
        step = JobSteps.UPLOAD;
    }

    /**
     * Anota lo que se leyó antes de validar: así la página tiene el conteo aunque el archivo sea inválido.
     * El archivo ya pasó el paso 2; desde aquí corre el paso 3, la validación contra el mapping.
     *
     * @param records registros o filas leídos
     * @param read    tabla de correspondencia del CSV; vacía en JSON
     */
    synchronized void describe(int records, List<CsvColumnRow> read) {
        fileCount = records;
        columns = List.copyOf(read);
        step = JobSteps.VALIDATION;
    }

    synchronized void ready(ValidatedMovies checked) {
        if (stopIfCancelled()) {
            return;
        }
        kept = KeptReport.of(checked.report());
        movies = List.copyOf(checked.movies());
        fileCount = movies.size();
        replaceCount = checked.replaceCount();
        status = JobStatus.VALID;
        step = JobSteps.IMPORT;
    }

    /**
     * Deja el trabajo en {@code invalido}, detenido en el paso que rechazó el archivo.
     *
     * @param issues  informe que impide importar
     * @param stopped 2 si falló la comprobación del archivo; 3 si falló la lectura o el mapping
     */
    synchronized void invalid(ValidationReport issues, int stopped) {
        kept = KeptReport.of(issues);
        status = JobStatus.INVALID;
        step = stopped;
    }

    synchronized void failed(String code) {
        failureCode = code;
        status = JobStatus.FAILED;
    }

    /**
     * Pasa a {@code importando} solo si el archivo ya es válido y nadie canceló.
     *
     * @return {@code true} si hay que arrancar el hilo
     */
    synchronized boolean beginImport() {
        if (stopIfCancelled()) {
            return false;
        }
        if (status != JobStatus.VALID) {
            return false;
        }
        status = JobStatus.IMPORTING;
        step = JobSteps.IMPORT;
        return true;
    }

    synchronized void progress(ImportProgress progress) {
        step = progress.step();
        batch = progress.batch();
        vectorized = progress.vectorized();
        written = progress.written();
        tempIndex = progress.tempIndex() == null ? "" : progress.tempIndex();
    }

    synchronized void become(JobStatus next) {
        status = next;
        if (next == JobStatus.PUBLISHED) {
            step = JobSteps.PUBLISH;
        }
    }

    /**
     * Pide parar. Si todavía no se importa, el estado pasa ya a {@code cancelado}.
     */
    synchronized void requestCancel() {
        cancelled.set(true);
        if (status == JobStatus.RECEIVED || status == JobStatus.VALID) {
            status = JobStatus.CANCELLED;
        }
    }

    /**
     * Copia para la página. {@code replaceCount} es el de la validación: el avance no lo cambia. Las cifras
     * del informe ya están calculadas desde {@link #ready} o {@link #invalid}.
     *
     * @param batchSize películas por lote configuradas
     * @return copia que no cambia
     */
    synchronized JobView view(int batchSize) {
        ReportFigures figures = kept.figures();
        int invalidRecords = figures.invalidRecords();
        return new JobView(id, status.label(), JobFigures.kindOf(fileName), fileSize,
                step, batch, vectorized, written,
                fileCount, JobFigures.validCount(fileCount, invalidRecords), invalidRecords, replaceCount,
                figures.errorTotal(), figures.preview(), figures.warnings(),
                JobFigures.checks(status, figures.errorTotal(), fileCount), columns,
                failureCode, JobFigures.failureMessage(failureCode),
                batchSize, JobFigures.batches(fileCount, batchSize), tempIndex);
    }

    private boolean stopIfCancelled() {
        if (!cancelled.get()) {
            return false;
        }
        status = JobStatus.CANCELLED;
        return true;
    }
}
