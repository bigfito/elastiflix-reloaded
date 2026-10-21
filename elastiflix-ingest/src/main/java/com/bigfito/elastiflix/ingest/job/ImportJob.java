package com.bigfito.elastiflix.ingest.job;

import java.time.Clock;
import java.util.function.BooleanSupplier;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.catalog.CatalogName;
import com.bigfito.elastiflix.core.catalog.IndexNames;
import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ElasticsearchDiagnostics;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.ingest.vector.PlotVectorizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Copia el catálogo, escribe los lotes y publica.
 *
 * <p>Si algo falla antes de publicar, borra el temporal. Si {@code publish} falla, primero lee el
 * alias: solo borra el temporal cuando Elasticsearch rechazó la publicación con un 4xx y el alias sigue
 * en el índice anterior.</p>
 */
@Component
public final class ImportJob {

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
     * Importa si el nombre es un alias de un solo índice.
     *
     * <p>Si falla antes de {@code _aliases}, el alias queda como estaba y el temporal se borra (si el borrado
     * falla, queda un {@code WARN}). Si {@code _aliases} no confirma, {@code describe} decide; con
     * {@code EFX-IMP-006} no se sabe dónde quedó el alias y el temporal se deja.</p>
     *
     * @param alias     alias del catálogo
     * @param source    películas ya validadas, por lotes
     * @param counts    registros del archivo y tamaño de lote
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
        createTemp(temp);
        importInto(new ImportContext(
                alias, found.concreteIndex(), temp, source, counts, listener, cancelled));
    }

    /**
     * Copia, escribe y publica. El {@code finally} borra el temporal solo si nunca llegó a ser el índice
     * vigente: antes de {@code _aliases} siempre, y después solo si Elasticsearch rechazó la publicación con
     * un 4xx y {@code describe} confirmó el índice anterior. Así ni una falla del aviso de progreso ni una
     * excepción inesperada pueden borrar el catálogo publicado.
     */
    private void importInto(ImportContext context) {
        boolean deleteTemp = true;
        WriteTotals totals;
        long expected;
        try {
            context.listener().onProgress(new ImportProgress(JobSteps.IMPORT, 0, 0, 0, context.temp()));
            long copied = copy(context);
            totals = writeBatches(context);
            expected = verifiedCount(context, copied, totals);
            ensureRunning(context.cancelled());
            deleteTemp = false;
            swap(context);
        } catch (ElastiflixException exception) {
            deleteTemp = deleteTemp || exception.errorCode() == ErrorCode.ALIAS_PUBLISH_FAILED;
            logFailure(exception, context.temp());
            throw exception;
        } finally {
            if (deleteTemp) {
                deleteTemp(context.temp());
            }
        }
        published(context, totals, expected);
    }

    /**
     * Crea el temporal. Un 4xx es un rechazo seguro: el índice no se creó y no hay nada que borrar. Un corte,
     * un timeout o un 5xx son dudosos: el índice pudo crearse, así que se intenta borrar antes de seguir con
     * la falla, como en cualquier otra falla anterior a {@code _aliases}.
     */
    private void createTemp(String temp) {
        try {
            catalog.createWithMapping(temp);
        } catch (ElasticsearchAccessException exception) {
            logFailure(exception, temp);
            if (!ElasticsearchDiagnostics.isClientRejection(exception)) {
                deleteTemp(temp);
            }
            throw exception;
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
        catalog.reindex(context.previous(), context.temp(), context.cancelled());
        ensureRunning(context.cancelled());
        catalog.refresh(context.temp());
        long copied = catalog.count(context.temp());
        if (copied != catalog.count(context.previous())) {
            throw new ElasticsearchAccessException(ErrorCode.REINDEX_COUNT_MISMATCH);
        }
        return copied;
    }

    private WriteTotals writeBatches(ImportContext context) {
        return new BatchWriter(catalog, vectorizer).write(context);
    }

    private long verifiedCount(ImportContext context, long copied, WriteTotals totals) {
        catalog.refresh(context.temp());
        long expected = copied + context.counts().fileCount() - totals.replaced();
        if (catalog.count(context.temp()) != expected) {
            throw new ImportException(ErrorCode.IMPORT_COUNT_MISMATCH);
        }
        return expected;
    }

    /**
     * Ya publicado: un fallo de aquí en adelante no toca el índice nuevo. Si el aviso del paso 5 falla, el
     * trabajo igual terminó bien, así que solo queda un {@code WARN} y {@code run} vuelve sin excepción.
     */
    private void published(ImportContext context, WriteTotals totals, long expected) {
        LOG.info("La importación publicó {} en {} y borró {} ({} documentos)",
                context.alias(), context.temp(), context.previous(), expected);
        try {
            context.listener().onProgress(totals.notice(JobSteps.PUBLISH, context.temp()));
        } catch (ElastiflixException exception) {
            LOG.warn("{}: falló el aviso del paso 5, pero {} ya está publicado tipo={}",
                    exception.errorCode().code(), context.temp(), CauseType.of(exception));
        }
    }

    private void swap(ImportContext context) {
        try {
            catalog.publish(context.alias(), context.temp(), context.previous());
        } catch (ElasticsearchAccessException exception) {
            resolvePublish(context, exception);
        }
    }

    /**
     * Un corte de red, un timeout del cliente o un 5xx no son un rechazo: {@code _aliases} pudo aplicarse
     * después. Solo un 4xx con el alias todavía en el índice anterior permite borrar el temporal.
     */
    private void resolvePublish(ImportContext context, ElasticsearchAccessException failure) {
        CatalogName described = aliasAfter(context.alias());
        if (pointsTo(described, context.temp())) {
            return;
        }
        if (ElasticsearchDiagnostics.isClientRejection(failure) && pointsTo(described, context.previous())) {
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

    /**
     * Para con {@code EFX-IMP-003} si alguien pidió cancelar. Lo usan también los lotes.
     *
     * @param cancelled señal de cancelación
     */
    static void ensureRunning(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) {
            throw new ImportException(ErrorCode.IMPORT_CANCELLED);
        }
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
        LOG.error("{} en el índice {} tipo={}", code, temp, CauseType.of(exception));
    }
}
