package com.bigfito.elastiflix.ingest.job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;
import com.bigfito.elastiflix.ingest.mapping.ValidatedMovies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mapa de trabajos de esta JVM. Un reinicio lo olvida.
 */
@Component
public final class JobStore implements ImportListener {

    private static final Logger LOG = LoggerFactory.getLogger(JobStore.class);

    private final Map<String, ImportWork> jobs = new ConcurrentHashMap<>();
    private volatile ImportWork importing;

    /**
     * Crea el trabajo en {@code recibido}.
     *
     * @param fileName nombre original
     * @param path     archivo local
     * @param settings ajustes del CSV
     * @return identificador nuevo
     */
    public String open(String fileName, Path path, CsvSettings settings) {
        String id = UUID.randomUUID().toString();
        jobs.put(id, new ImportWork(id, fileName, path, settings));
        LOG.debug("Llegó el archivo {}", fileName);
        return id;
    }

    /**
     * Copia del trabajo. Si el id no está, {@code EFX-IMP-005}.
     *
     * @param jobId identificador
     * @return copia que no cambia
     */
    public JobView view(String jobId) {
        return require(jobId).view();
    }

    /**
     * CSV de todos los errores, no solo los seis del estado.
     *
     * @param jobId identificador
     * @return texto con encabezado
     */
    public String errorReport(String jobId) {
        return require(jobId).report().toErrorCsv();
    }

    /**
     * Pide la cancelación. Si ya se publicó, el estado no cambia.
     *
     * @param jobId identificador
     */
    public void cancel(String jobId) {
        ImportWork work = require(jobId);
        if (work.status() == JobStatus.PUBLISHED) {
            return;
        }
        work.requestCancel();
        LOG.info("{}: se pidió cancelar {}", ErrorCode.IMPORT_CANCELLED.code(), jobId);
        discardIfTerminal(work);
    }

    /**
     * Marca el trabajo como {@code importando} si se puede.
     *
     * @param jobId identificador
     * @return {@code false} si no estaba {@code valido}
     */
    public boolean beginImport(String jobId) {
        ImportWork work = require(jobId);
        if (!work.beginImport()) {
            discardIfTerminal(work);
            return false;
        }
        importing = work;
        return true;
    }

    @Override
    public void onProgress(ImportProgress progress) {
        ImportWork current = importing;
        if (current != null) {
            current.progress(progress);
        }
    }

    /**
     * El archivo pasó la validación, o quedó cancelado si ya lo habían pedido.
     *
     * @param jobId   identificador
     * @param checked películas y el informe de campos
     */
    public void ready(String jobId, ValidatedMovies checked) {
        ImportWork work = require(jobId);
        if (finishEarly(work)) {
            return;
        }
        work.ready(checked);
        logValidation(work);
    }

    /**
     * Guarda los errores y deja el trabajo en {@code invalido}.
     *
     * @param jobId  identificador
     * @param issues informe que impide importar
     */
    public void invalid(String jobId, ValidationReport issues) {
        ImportWork work = require(jobId);
        if (finishEarly(work)) {
            return;
        }
        work.invalid(issues);
        logValidation(work);
        discardIfTerminal(work);
    }

    /**
     * Deja el trabajo en {@code fallido} y borra el archivo local.
     *
     * @param jobId     identificador
     * @param exception falla de Elasticsearch, de inferencia o de lectura
     */
    public void failed(String jobId, ElastiflixException exception) {
        ImportWork work = require(jobId);
        work.failed(exception.errorCode().code());
        LOG.error("{} en el trabajo {}", exception.errorCode().code(), jobId);
        discardIfTerminal(work);
    }

    /**
     * El alias ya apunta al índice nuevo.
     *
     * @param jobId identificador
     */
    public void published(String jobId) {
        ImportWork work = require(jobId);
        work.become(JobStatus.PUBLISHED);
        importing = null;
        discardIfTerminal(work);
    }

    /**
     * La importación no publicó. Distingue la cancelación de un fallo.
     *
     * @param jobId     identificador
     * @param exception lo que lanzó {@link ImportJob}
     */
    public void importStopped(String jobId, ElastiflixException exception) {
        ImportWork work = require(jobId);
        importing = null;
        if (ErrorCode.IMPORT_CANCELLED.code().equals(exception.errorCode().code())) {
            work.become(JobStatus.CANCELLED);
            LOG.info("{}: se canceló el trabajo {}", exception.errorCode().code(), jobId);
        } else {
            work.failed(exception.errorCode().code());
            LOG.error("{} en el trabajo {}", exception.errorCode().code(), jobId);
        }
        discardIfTerminal(work);
    }

    ImportWork require(String jobId) {
        ImportWork work = jobs.get(jobId);
        if (work == null) {
            throw new ImportException(ErrorCode.UNKNOWN_JOB);
        }
        return work;
    }

    private boolean finishEarly(ImportWork work) {
        if (!work.cancelledFlag().getAsBoolean()) {
            return false;
        }
        work.become(JobStatus.CANCELLED);
        logValidation(work);
        discardIfTerminal(work);
        return true;
    }

    private void discardIfTerminal(ImportWork work) {
        JobStatus status = work.status();
        if (terminal(status)) {
            deleteLocal(work);
        }
    }

    private static boolean terminal(JobStatus status) {
        return status == JobStatus.INVALID
                || status == JobStatus.PUBLISHED
                || status == JobStatus.FAILED
                || status == JobStatus.CANCELLED;
    }

    private void deleteLocal(ImportWork work) {
        try {
            Files.deleteIfExists(work.path());
        } catch (IOException exception) {
            LOG.warn("No se pudo borrar el archivo local del trabajo {}", work.id());
        }
    }

    private static void logValidation(ImportWork work) {
        ValidationReport report = work.report();
        LOG.info("El archivo {} terminó de validarse: {} errores y {} advertencias",
                work.id(), report.errors().size(), report.warnings().size());
    }
}
