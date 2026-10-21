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
import java.util.function.UnaryOperator;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.ingest.csv.CsvColumnRow;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.mapping.ValidatedMovies;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;

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
     * @param mode     append o bulk
     * @return identificador nuevo
     */
    public String open(String fileName, Path path, CsvSettings settings, ImportMode mode) {
        evict();
        String id = UUID.randomUUID().toString();
        ImportWork work = new ImportWork(id, new JobFile(fileName, path, sizeOf(path)), settings, mode);
        activity.put(id, clock.instant());
        jobs.put(id, work);
        LOG.debug("Llegó el archivo {}", fileName);
        return id;
    }

    /**
     * Copia del trabajo. Si el id no está, {@code EFX-IMP-005}. Antes revisa los trabajos vencidos, para
     * que un {@code valido} olvidado suelte sus películas aunque nadie suba otro archivo.
     *
     * @param jobId identificador
     * @return copia que no cambia
     */
    public JobView view(String jobId) {
        evict();
        return require(jobId).view(properties.ingest().batchSize());
    }

    /**
     * CSV de los errores guardados, no solo los seis del estado: todos hasta
     * {@value JobRetention#MAX_KEPT_ERRORS} y, si hubo más, los primeros {@value JobRetention#MAX_KEPT_ERRORS}.
     * El total real está en {@link JobView#errorTotal()}.
     *
     * @param jobId identificador
     * @return texto con encabezado
     */
    public String errorReport(String jobId) {
        return require(jobId).kept().report().toErrorCsv();
    }

    /**
     * Informe de errores con otro encabezado y cada error reescrito, por ejemplo traducido. Trae los mismos
     * errores guardados que {@link #errorReport(String)}.
     *
     * @param jobId   identificador
     * @param header  primera fila del CSV
     * @param rewrite cambia el texto de cada error
     * @return CSV con BOM
     */
    public String errorReport(String jobId, String header, UnaryOperator<ValidationIssue> rewrite) {
        return require(jobId).kept().report().toErrorCsv(header, rewrite);
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
     * Marca el trabajo como {@code importando} si se puede. Un {@code valido} vencido ya no se importa.
     *
     * @param jobId identificador
     * @return {@code false} si no estaba {@code valido}
     */
    public boolean beginImport(String jobId) {
        evict();
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
        if (stopIfCancelled(work)) {
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
        if (stopIfCancelled(work)) {
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
        LOG.error("{} en el trabajo {} tipo={}", exception.errorCode().code(), jobId, CauseType.of(exception));
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
            LOG.error("{} en el trabajo {} tipo={}", exception.errorCode().code(), jobId, CauseType.of(exception));
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

    /**
     * Si alguien pidió cancelar, deja el trabajo en {@code cancelado}, suelta lo que retiene y avisa que hay
     * que parar. La validación lo consulta entre etapas; {@link #ready} e {@link #invalid}, antes de guardar.
     *
     * @param work trabajo que se valida
     * @return {@code true} si se canceló y no hay que seguir
     */
    boolean stopIfCancelled(ImportWork work) {
        if (!work.isCancelled()) {
            return false;
        }
        work.become(JobStatus.CANCELLED);
        LOG.info("{}: se canceló el trabajo {} durante la validación", ErrorCode.IMPORT_CANCELLED.code(),
                work.id());
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


    private static void logValidation(ImportWork work) {
        ReportFigures figures = work.kept().figures();
        LOG.info("El archivo {} terminó de validarse: {} errores y {} advertencias",
                work.id(), figures.errorTotal(), figures.warningTotal());
    }
}
