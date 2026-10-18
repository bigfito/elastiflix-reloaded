package com.bigfito.elastiflix.ingest.job;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;
import com.bigfito.elastiflix.ingest.mapping.ValidatedMovies;

/**
 * Un trabajo de esta JVM. Lo cambia el hilo de la carga y lo lee la página.
 */
final class ImportWork {

    private static final int ERROR_PREVIEW = 6;
    private static final int UPLOAD_STEP = 2;
    private static final int VALIDATION_STEP = 3;
    private static final int IMPORT_STEP = 4;
    private static final int PUBLISH_STEP = 5;

    private final String id;
    private final Path path;
    private final String fileName;
    private final CsvSettings settings;
    private final AtomicBoolean cancelled = new AtomicBoolean();

    private JobStatus status = JobStatus.RECEIVED;
    private ValidationReport report = ValidationReport.empty();
    private List<Movie> movies = List.of();
    private int fileCount;
    private int replaceCount;
    private int step = 1;
    private int batch;
    private int vectorized;
    private int written;
    private String failureCode = "";

    /**
     * Abre el trabajo en {@code recibido}.
     *
     * @param id       identificador
     * @param fileName nombre original, sin ruta
     * @param path     archivo local
     * @param settings separador y encabezado; en JSON no se usan
     */
    ImportWork(String id, String fileName, Path path, CsvSettings settings) {
        this.id = id;
        this.fileName = fileName;
        this.path = path;
        this.settings = settings;
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

    synchronized ValidationReport report() {
        return report;
    }

    synchronized List<Movie> movies() {
        return movies;
    }

    /**
     * Conteos para {@link ImportJob}. El lote lo pone quien llama.
     *
     * @param batchSize películas por lote
     * @return conteos de este archivo
     */
    synchronized ImportCounts counts(int batchSize) {
        return new ImportCounts(fileCount, replaceCount, batchSize);
    }

    BooleanSupplier cancelledFlag() {
        return cancelled::get;
    }

    synchronized void validating() {
        status = JobStatus.VALIDATING;
        step = UPLOAD_STEP;
    }

    synchronized void ready(ValidatedMovies checked) {
        if (stopIfCancelled()) {
            return;
        }
        report = checked.report();
        movies = List.copyOf(checked.movies());
        fileCount = movies.size();
        replaceCount = checked.replaceCount();
        status = JobStatus.VALID;
        step = IMPORT_STEP;
    }

    synchronized void invalid(ValidationReport issues) {
        report = issues;
        status = JobStatus.INVALID;
        step = VALIDATION_STEP;
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
        step = IMPORT_STEP;
        return true;
    }

    synchronized void progress(ImportProgress progress) {
        step = progress.step();
        batch = progress.batch();
        vectorized = progress.vectorized();
        written = progress.written();
    }

    synchronized void become(JobStatus next) {
        status = next;
        if (next == JobStatus.PUBLISHED) {
            step = PUBLISH_STEP;
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

    synchronized JobView view() {
        int errors = report.errors().size();
        return new JobView(
                id,
                status.label(),
                step,
                batch,
                vectorized,
                written,
                fileCount,
                replaceCount,
                preview(report.errors()),
                report.warnings(),
                failureCode,
                JobFigures.failureMessage(failureCode),
                JobFigures.checks(errors, fileCount),
                JobFigures.validCount(errors, fileCount),
                errors,
                JobFigures.batches(fileCount));
    }

    private boolean stopIfCancelled() {
        if (!cancelled.get()) {
            return false;
        }
        status = JobStatus.CANCELLED;
        return true;
    }

    private static List<ValidationIssue> preview(List<ValidationIssue> errors) {
        if (errors.size() <= ERROR_PREVIEW) {
            return errors;
        }
        return List.copyOf(new ArrayList<>(errors.subList(0, ERROR_PREVIEW)));
    }
}
