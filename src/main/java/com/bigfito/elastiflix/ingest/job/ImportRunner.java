package com.bigfito.elastiflix.ingest.job;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;

import org.springframework.stereotype.Component;

/**
 * Paso 4 y, en bulk, paso 5: corre el trabajo del modo y deja el estado final.
 *
 * <p>Bulk ({@link ImportJob}) termina en {@code publicado}; append ({@link AppendJob}), en {@code agregado}.</p>
 *
 * <p>Si la importación para antes de publicar, {@link ImportJob} borra el temporal; si ese borrado falla,
 * el temporal queda y hay que borrarlo a mano. Si {@code _aliases} no confirma y no se puede saber dónde
 * quedó el alias ({@code EFX-IMP-006}), el temporal se deja a propósito.</p>
 */
@Component
public final class ImportRunner {

    private final ImportJob job;
    private final AppendJob append;
    private final JobStore store;
    private final ElastiflixProperties properties;

    /**
     * Crea el corredor con el alias configurado.
     *
     * @param job        bulk: índice temporal, publicación y vuelta atrás
     * @param append     append: lotes por el alias
     * @param store      progreso y estado final
     * @param properties alias y tamaño de lote
     */
    public ImportRunner(ImportJob job, AppendJob append, JobStore store, ElastiflixProperties properties) {
        this.job = job;
        this.append = append;
        this.store = store;
        this.properties = properties;
    }

    /**
     * Importa un trabajo que ya está en {@code importando}.
     *
     * @param jobId identificador
     */
    public void run(String jobId) {
        ImportWork work = store.require(jobId);
        MovieBatchSource source = new ListMovieSource(work.movies());
        ImportCounts counts = work.counts(properties.ingest().batchSize());
        work.release();
        ImportListener listener = progress -> store.onProgress(jobId, progress);
        String alias = properties.index().alias();
        try {
            if (work.mode() == ImportMode.APPEND) {
                append.run(alias, source, counts, listener, work::isCancelled);
            } else {
                job.run(alias, source, counts, listener, work::isCancelled);
            }
            store.finished(jobId);
        } catch (ElastiflixException exception) {
            store.importStopped(jobId, exception);
        } finally {
            if (store.view(jobId).status().equals(JobStatus.IMPORTING.label())) {
                store.failed(jobId, new ImportException(ErrorCode.JOB_FAILED));
            }
        }
    }
}
