package com.bigfito.elastiflix.ingest.job;

import java.time.Clock;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.catalog.CatalogName;
import com.bigfito.elastiflix.core.catalog.IndexNames;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.vector.PlotVectorizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Copia el catálogo, escribe los lotes y publica.
 *
 * <p>Si algo falla antes de publicar, borra el temporal. Si {@code publish} falla, primero lee el
 * alias: solo borra el temporal cuando el alias sigue en el índice anterior.</p>
 */
@Component
public final class ImportJob {

    private static final int IMPORT_STEP = 4;
    private static final int PUBLISH_STEP = 5;
    private static final Logger LOG = LoggerFactory.getLogger(ImportJob.class);

    private final CatalogIndex catalog;
    private final IndexNames names;
    private final Clock clock;
    private final PlotVectorizer vectorizer;
    private final OrphanCleaner cleaner;

    /**
     * Crea el trabajo con las puertas al catálogo y a la inferencia.
     *
     * @param catalog    operaciones del catálogo
     * @param names      nombre del índice temporal
     * @param clock      reloj UTC de ese nombre
     * @param vectorizer vectores de cada lote
     * @param cleaner    huérfanos del alias
     */
    public ImportJob(
            CatalogIndex catalog,
            IndexNames names,
            Clock clock,
            PlotVectorizer vectorizer,
            OrphanCleaner cleaner) {
        this.catalog = catalog;
        this.names = names;
        this.clock = clock;
        this.vectorizer = vectorizer;
        this.cleaner = cleaner;
    }

    /**
     * Importa si el nombre es un alias de un solo índice. Si falla, el alias queda como estaba.
     *
     * @param alias     alias del catálogo
     * @param source    películas ya validadas, por lotes
     * @param counts    registros del archivo y cuántos reemplazan
     * @param listener  progreso de los pasos 4 y 5
     * @param cancelled se consulta antes de copiar, de cada lote y de publicar
     */
    public void run(
            String alias,
            MovieBatchSource source,
            ImportCounts counts,
            ImportListener listener,
            BooleanSupplier cancelled) {
        ensureRunning(cancelled);
        CatalogName.Alias found = requireAlias(alias);
        cleaner.clean(catalog, alias, found.concreteIndex());
        String temp = names.timestamped(alias, clock);
        catalog.createWithMapping(temp);
        importInto(new ImportContext(
                alias, found.concreteIndex(), temp, source, counts, listener, cancelled));
    }

    private void importInto(ImportContext context) {
        try {
            long copied = copy(context);
            WriteTotals totals = writeBatches(context);
            publish(context, copied, totals);
        } catch (ElastiflixException exception) {
            rollback(context.temp(), exception);
        }
    }

    private CatalogName.Alias requireAlias(String alias) {
        CatalogName described = catalog.describe(alias);
        if (described instanceof CatalogName.Alias found) {
            return found;
        }
        throw rejection(described);
    }

    private static ElastiflixException rejection(CatalogName described) {
        if (described instanceof CatalogName.Missing) {
            return new ElasticsearchAccessException(ErrorCode.CATALOG_NOT_FOUND);
        }
        if (described instanceof CatalogName.Ambiguous) {
            return new ElasticsearchAccessException(ErrorCode.ALIAS_AMBIGUOUS);
        }
        return new ImportException(ErrorCode.CATALOG_NOT_ALIAS);
    }

    private long copy(ImportContext context) {
        ensureRunning(context.cancelled());
        catalog.reindex(context.previous(), context.temp());
        ensureRunning(context.cancelled());
        catalog.refresh(context.temp());
        long copied = catalog.count(context.temp());
        if (copied != catalog.count(context.previous())) {
            throw new ElasticsearchAccessException(ErrorCode.REINDEX_COUNT_MISMATCH);
        }
        return copied;
    }

    private WriteTotals writeBatches(ImportContext context) {
        WriteTotals totals = new WriteTotals(0, 0, 0);
        List<Movie> movies = context.source().next(context.counts().batchSize());
        while (!movies.isEmpty()) {
            totals = writeOne(context, movies, totals);
            movies = context.source().next(context.counts().batchSize());
        }
        return totals;
    }

    private WriteTotals writeOne(ImportContext context, List<Movie> movies, WriteTotals totals) {
        ensureRunning(context.cancelled());
        List<Movie> ready = vectorizer.apply(movies);
        catalog.writeAll(context.temp(), ready);
        WriteTotals done = new WriteTotals(
                totals.batches() + 1,
                totals.vectorized() + ready.size(),
                totals.written() + ready.size());
        context.listener().onProgress(new ImportProgress(
                IMPORT_STEP, done.batches(), done.vectorized(), done.written()));
        logBatch(done);
        return done;
    }

    private void publish(ImportContext context, long copied, WriteTotals totals) {
        catalog.refresh(context.temp());
        long expected = copied + context.counts().fileCount() - context.counts().replaceCount();
        if (catalog.count(context.temp()) != expected) {
            throw new ImportException(ErrorCode.IMPORT_COUNT_MISMATCH);
        }
        ensureRunning(context.cancelled());
        swap(context);
        context.listener().onProgress(new ImportProgress(
                PUBLISH_STEP, totals.batches(), totals.vectorized(), totals.written()));
        LOG.info("La importación publicó {} en {} y borró {} ({} documentos)",
                context.alias(), context.temp(), context.previous(), expected);
    }

    private void swap(ImportContext context) {
        try {
            catalog.publish(context.alias(), context.temp(), context.previous());
        } catch (ElasticsearchAccessException exception) {
            resolvePublish(context, exception);
        }
    }

    private void resolvePublish(ImportContext context, ElasticsearchAccessException failure) {
        // Un corte de red no es un rechazo. Solo se borra si describe confirma el índice anterior.
        CatalogName described = aliasAfter(context.alias());
        if (pointsTo(described, context.temp())) {
            return;
        }
        if (pointsTo(described, context.previous())) {
            throw new ElasticsearchAccessException(ErrorCode.ALIAS_PUBLISH_FAILED, failure);
        }
        throw uncertain(failure);
    }

    private CatalogName aliasAfter(String alias) {
        try {
            return catalog.describe(alias);
        } catch (ElastiflixException exception) {
            throw uncertain(exception);
        }
    }

    private static boolean pointsTo(CatalogName described, String index) {
        return described instanceof CatalogName.Alias alias && index.equals(alias.concreteIndex());
    }

    private static ImportException uncertain(ElastiflixException cause) {
        return new ImportException(ErrorCode.PUBLISH_UNCERTAIN, cause);
    }

    private static void ensureRunning(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) {
            throw new ImportException(ErrorCode.IMPORT_CANCELLED);
        }
    }

    private void rollback(String temp, ElastiflixException exception) {
        if (exception.errorCode() != ErrorCode.PUBLISH_UNCERTAIN) {
            deleteTemp(temp);
        }
        logFailure(exception, temp);
        throw exception;
    }

    private void deleteTemp(String temp) {
        try {
            catalog.deleteIndex(temp);
        } catch (ElasticsearchAccessException exception) {
            LOG.warn("No se pudo borrar el índice temporal {}", temp);
        }
    }

    private static void logFailure(ElastiflixException exception, String temp) {
        String code = exception.errorCode().code();
        if (ErrorCode.IMPORT_CANCELLED.code().equals(code)) {
            LOG.info("{}: se canceló {}", code, temp);
            return;
        }
        LOG.error("{} en el índice {}", code, temp);
    }

    private static void logBatch(WriteTotals done) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Lote {} con {} registros", done.batches(), done.written());
        }
    }
}
