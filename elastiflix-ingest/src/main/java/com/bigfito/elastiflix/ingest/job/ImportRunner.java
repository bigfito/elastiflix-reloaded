package com.bigfito.elastiflix.ingest.job;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;

import org.springframework.stereotype.Component;

/**
 * Paso 4 y paso 5: copia, escribe y publica, o deja el temporal borrado.
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
        try {
            job.run(
                    properties.index().alias(),
                    new ListMovieSource(work.movies()),
                    work.counts(properties.ingest().batchSize()),
                    store,
                    work.cancelledFlag());
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
