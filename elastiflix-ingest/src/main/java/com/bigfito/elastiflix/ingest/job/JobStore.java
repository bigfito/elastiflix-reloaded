package com.bigfito.elastiflix.ingest.job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.ingest.csv.CsvColumnRow;
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
public final class JobStore {

    private static final Logger LOG = LoggerFactory.getLogger(JobStore.class);

    private final Map<String, ImportWork> jobs = new ConcurrentHashMap<>();
    private final Map<String, Instant> activity = new ConcurrentHashMap<>();
    private final ElastiflixProperties properties;
    private final Clock clock;

    /**
     * Crea el mapa de trabajos.
     *
     * @param properties tamaño de lote que muestra la página
     * @param clock      reloj UTC de la última actividad de cada trabajo
     */
    public JobStore(ElastiflixProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Crea el trabajo en {@code recibido}.
     *
     * @param fileName nombre original
     * @param path     archivo local
     * @param settings ajustes del CSV
     * @return identificador nuevo
     */
    public String open(String fileName, Path path, CsvSettings settings) {
        evict();
        String id = UUID.randomUUID().toString();
        ImportWork work = new ImportWork(id, fileName, path, settings, sizeOf(path));
        activity.put(id, clock.instant());
        jobs.put(id, work);
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
        return require(jobId).view(properties.ingest().batchSize());
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
        return true;
    }

    /**
     * Anota el avance de ese trabajo. Otro trabajo no recibe el aviso.
     *
     * @param jobId    identificador
     * @param progress paso, lote, vectorizados, escritos e índice temporal
     */
    public void onProgress(String jobId, ImportProgress progress) {
        require(jobId).progress(progress);
    }

    /**
     * Anota cuántos registros se leyeron y, en CSV, la tabla de correspondencia.
     *
     * @param jobId   identificador
     * @param records registros o filas de datos
     * @param columns tabla del CSV; vacía en JSON
     */
    public void fileRead(String jobId, int records, List<CsvColumnRow> columns) {
        require(jobId).describe(records, columns);
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
     * Guarda los errores y deja el trabajo en {@code invalido}, detenido en el paso que lo rechazó.
     *
     * @param jobId   identificador
     * @param issues  informe que impide importar
     * @param stopped 2 si falló la comprobación del archivo ({@code EFX-ING-001} a {@code 004}); 3 si no
     */
    public void invalid(String jobId, ValidationReport issues, int stopped) {
        ImportWork work = require(jobId);
        if (finishEarly(work)) {
            return;
        }
        work.invalid(issues, stopped);
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
        LOG.error("{} en el trabajo {} tipo={}", exception.errorCode().code(), jobId, causeType(exception));
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
        if (ErrorCode.IMPORT_CANCELLED.code().equals(exception.errorCode().code())) {
            work.become(JobStatus.CANCELLED);
            LOG.info("{}: se canceló el trabajo {}", exception.errorCode().code(), jobId);
        } else {
            work.failed(exception.errorCode().code());
            LOG.error("{} en el trabajo {} tipo={}", exception.errorCode().code(), jobId, causeType(exception));
        }
        discardIfTerminal(work);
    }

    ImportWork require(String jobId) {
        ImportWork work = jobs.get(jobId);
        if (work == null) {
            throw new ImportException(ErrorCode.UNKNOWN_JOB);
        }
        activity.put(jobId, clock.instant());
        return work;
    }

    private boolean finishEarly(ImportWork work) {
        if (!work.isCancelled()) {
            return false;
        }
        work.become(JobStatus.CANCELLED);
        logValidation(work);
        discardIfTerminal(work);
        return true;
    }

    /**
     * Cancela los {@code valido} olvidados y olvida los trabajos finales que pasan del tope. Nunca toca un
     * trabajo {@code importando}.
     */
    private void evict() {
        for (ImportWork work : JobRetention.idleValid(jobs.values(), activity, clock.instant())) {
            work.requestCancel();
            LOG.info("{}: el trabajo {} venció sin importarse", ErrorCode.IMPORT_CANCELLED.code(), work.id());
            discardIfTerminal(work);
        }
        for (ImportWork work : JobRetention.overflow(jobs.values(), activity)) {
            jobs.remove(work.id());
            activity.remove(work.id());
        }
    }

    private void discardIfTerminal(ImportWork work) {
        if (work.status().terminal()) {
            work.release();
            deleteLocal(work);
        }
    }

    private void deleteLocal(ImportWork work) {
        try {
            Files.deleteIfExists(work.path());
        } catch (IOException exception) {
            LOG.warn("No se pudo borrar el archivo local del trabajo {}", work.id());
        }
    }

    private static long sizeOf(Path path) {
        try {
            return Files.size(path);
        } catch (IOException exception) {
            LOG.warn("No se pudo leer el tamaño del archivo subido tipo={}", exception.getClass().getSimpleName());
            return 0;
        }
    }

    private static String causeType(ElastiflixException exception) {
        Throwable cause = exception.getCause();
        if (cause == null) {
            return exception.getClass().getSimpleName();
        }
        return cause.getClass().getSimpleName();
    }

    private static void logValidation(ImportWork work) {
        ValidationReport report = work.report();
        LOG.info("El archivo {} terminó de validarse: {} errores y {} advertencias",
                work.id(), report.errors().size(), report.warnings().size());
    }
}
