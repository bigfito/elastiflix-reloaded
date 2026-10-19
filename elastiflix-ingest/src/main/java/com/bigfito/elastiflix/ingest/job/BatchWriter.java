package com.bigfito.elastiflix.ingest.job;

import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.vector.PlotVectorizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Escribe los lotes en el índice temporal. Avisa el avance dos veces por lote: antes de escribir, con el
 * lote ya vectorizado, y después, cuando Elasticsearch aceptó el lote.
 */
final class BatchWriter {

    private static final int IMPORT_STEP = 4;
    private static final Logger LOG = LoggerFactory.getLogger(BatchWriter.class);

    private final CatalogIndex catalog;
    private final PlotVectorizer vectorizer;

    /**
     * Crea el escritor de lotes.
     *
     * @param catalog    índice temporal
     * @param vectorizer vectores de cada lote
     */
    BatchWriter(CatalogIndex catalog, PlotVectorizer vectorizer) {
        this.catalog = catalog;
        this.vectorizer = vectorizer;
    }

    /**
     * Vectoriza y escribe hasta acabar la fuente.
     *
     * @param context alias, temporal y películas
     * @return lotes, vectorizados, escritos y reemplazos
     */
    WriteTotals write(ImportContext context) {
        WriteTotals totals = new WriteTotals(0, 0, 0, 0);
        List<Movie> movies = context.source().next(context.counts().batchSize());
        while (!movies.isEmpty()) {
            totals = writeOne(context, movies, totals);
            movies = context.source().next(context.counts().batchSize());
        }
        return totals;
    }

    private WriteTotals writeOne(ImportContext context, List<Movie> movies, WriteTotals totals) {
        ensureRunning(context);
        List<Movie> ready = vectorizer.apply(movies);
        WriteTotals done = advanced(totals, ready.size(), replacements(context.temp(), ready));
        // Primero se avisan los vectorizados; los escritos, solo cuando Elasticsearch aceptó el lote.
        context.listener().onProgress(done.withWritten(totals.written()).notice(IMPORT_STEP, context.temp()));
        catalog.writeAll(context.temp(), ready);
        context.listener().onProgress(done.notice(IMPORT_STEP, context.temp()));
        logBatch(done);
        return done;
    }

    private int replacements(String index, List<Movie> movies) {
        return catalog.existingIds(index, ids(movies)).size();
    }

    private static List<String> ids(List<Movie> movies) {
        List<String> ids = new ArrayList<>();
        for (Movie movie : movies) {
            if (movie.id() != null) {
                ids.add(movie.id());
            }
        }
        return ids;
    }

    private static WriteTotals advanced(WriteTotals totals, int ready, int replaced) {
        return new WriteTotals(
                totals.batches() + 1,
                totals.vectorized() + ready,
                totals.written() + ready,
                totals.replaced() + replaced);
    }

    private static void ensureRunning(ImportContext context) {
        if (context.cancelled().getAsBoolean()) {
            throw new ImportException(ErrorCode.IMPORT_CANCELLED);
        }
    }

    private static void logBatch(WriteTotals done) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Lote {} con {} registros", done.batches(), done.written());
        }
    }
}
