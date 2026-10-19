package com.bigfito.elastiflix.ingest.job;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;

import org.springframework.stereotype.Component;

/**
 * Paso 4 y paso 5: copia, escribe y publica, y deja el estado final del trabajo.
 *
 * <p>Si la importación para antes de publicar, {@link ImportJob} borra el temporal; si ese borrado falla,
 * el temporal queda y hay que borrarlo a mano. Si {@code _aliases} no confirma y no se puede saber dónde
 * quedó el alias ({@code EFX-IMP-006}), el temporal se deja a propósito.</p>
 */
@Component
public final class ImportRunner {

    private final ImportJob job;
    private final JobStore store;
    private final ElastiflixProperties properties;

    /**
     * Crea el corredor con el alias configurado.
     *
     * @param job        publicación y vuelta atrás
     * @param store      progreso y estado final
     * @param properties alias y tamaño de lote
     */
    public ImportRunner(ImportJob job, JobStore store, ElastiflixProperties properties) {
        this.job = job;
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
        try {
            job.run(
                    properties.index().alias(),
                    source,
                    counts,
                    progress -> store.onProgress(jobId, progress),
                    work::isCancelled);
            store.published(jobId);
        } catch (ElastiflixException exception) {
            store.importStopped(jobId, exception);
        } finally {
            if (store.view(jobId).status().equals(JobStatus.IMPORTING.label())) {
                store.failed(jobId, new ImportException(ErrorCode.JOB_FAILED));
            }
        }
    }
}
