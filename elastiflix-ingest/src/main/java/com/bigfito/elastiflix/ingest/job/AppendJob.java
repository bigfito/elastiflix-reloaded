package com.bigfito.elastiflix.ingest.job;

import java.util.List;
import java.util.function.BooleanSupplier;

import com.bigfito.elastiflix.core.catalog.AppendResult;
import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.vector.PlotVectorizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Modo append: agrega las películas al índice vigente por el alias, sin índice temporal.
 *
 * <p>Escribe por lotes con {@code require_alias=true}; un {@code id} que ya está se reemplaza. Lo que se escribió
 * se queda aunque la carga pare: un lote con rechazos da {@code EFX-IMP-008} y una cancelación entre lotes,
 * {@code EFX-IMP-009}. Los escritos que ve la página cuentan los ítems que Elasticsearch aceptó, también los del
 * lote que falló. No crea ni borra índices.</p>
 */
@Component
public final class AppendJob {

    private static final Logger LOG = LoggerFactory.getLogger(AppendJob.class);
    private static final String NO_TEMP = "";

    private final CatalogIndex catalog;
    private final PlotVectorizer vectorizer;

    /**
     * Crea el trabajo con las puertas al catálogo y a la inferencia.
     *
     * @param catalog    operaciones del catálogo
     * @param vectorizer vectores de cada lote
     */
    public AppendJob(CatalogIndex catalog, PlotVectorizer vectorizer) {
        this.catalog = catalog;
        this.vectorizer = vectorizer;
    }

    /**
     * Agrega el archivo si el nombre es un alias de un solo índice. Al final, o si para, hace {@code refresh} para
     * que lo escrito se vea en las búsquedas.
     *
     * @param alias     alias del catálogo
     * @param source    películas ya validadas, por lotes
     * @param counts    registros del archivo y tamaño de lote
     * @param listener  progreso del paso 4
     * @param cancelled se consulta antes de empezar y antes de cada lote
     */
    public void run(
            String alias,
            MovieBatchSource source,
            ImportCounts counts,
            ImportListener listener,
            BooleanSupplier cancelled) {
        ensureRunning(cancelled);
        CatalogAliases.require(catalog, alias, ErrorCode.APPEND_WITHOUT_CATALOG);
        listener.onProgress(new ImportProgress(JobSteps.IMPORT, 0, 0, 0, NO_TEMP));
        Tally tally = new Tally();
        try {
            List<Movie> movies = source.next(counts.batchSize());
            while (!movies.isEmpty()) {
                ensureRunning(cancelled);
                tally.totals = batch(alias, movies, tally.totals, listener);
                movies = source.next(counts.batchSize());
            }
        } catch (ElastiflixException exception) {
            logStop(exception, alias, tally.totals);
            throw exception;
        } finally {
            refresh(alias);
        }
        LOG.info("La carga agregó {} películas a {} ({} reemplazadas)",
                tally.totals.written(), alias, tally.totals.replaced());
    }

    /**
     * Vectoriza, cuenta reemplazos y escribe un lote. Avisa antes de escribir (con el lote vectorizado) y después,
     * con los ítems que Elasticsearch aceptó; si alguno falló, lanza después de avisar.
     */
    private WriteTotals batch(String alias, List<Movie> movies, WriteTotals totals, ImportListener listener) {
        List<Movie> ready = vectorizer.apply(movies);
        int replaced = catalog.existingIds(alias, BatchWriter.ids(ready)).size();
        WriteTotals vectorized = new WriteTotals(totals.batches() + 1, totals.vectorized() + ready.size(),
                totals.written(), totals.replaced() + replaced);
        listener.onProgress(vectorized.notice(JobSteps.IMPORT, NO_TEMP));
        AppendResult result = catalog.appendAll(alias, ready);
        WriteTotals done = vectorized.withWritten(totals.written() + result.accepted());
        listener.onProgress(done.notice(JobSteps.IMPORT, NO_TEMP));
        if (result.hasRejections()) {
            throw new ImportException(ErrorCode.APPEND_BATCH_REJECTED);
        }
        return done;
    }

    private static void ensureRunning(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) {
            throw new ImportException(ErrorCode.APPEND_CANCELLED);
        }
    }

    private void refresh(String alias) {
        try {
            catalog.refresh(alias);
        } catch (ElastiflixException exception) {
            LOG.warn("{}: no se pudo hacer refresh de {} al terminar la carga", exception.errorCode().code(), alias);
        }
    }

    private static void logStop(ElastiflixException exception, String alias, WriteTotals totals) {
        String code = exception.errorCode().code();
        if (ErrorCode.APPEND_CANCELLED.code().equals(code)) {
            LOG.info("{}: se canceló la carga en {} con {} películas agregadas", code, alias, totals.written());
            return;
        }
        LOG.error("{} en {} con {} películas agregadas tipo={}",
                code, alias, totals.written(), CauseType.of(exception));
    }

    /** Lo escrito hasta ahora, para el log si la carga para. */
    private static final class Tally {
        private WriteTotals totals = new WriteTotals(0, 0, 0, 0);
    }
}
