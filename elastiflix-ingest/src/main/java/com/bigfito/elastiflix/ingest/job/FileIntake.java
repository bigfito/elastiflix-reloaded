package com.bigfito.elastiflix.ingest.job;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.core.error.IngestValidationException;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.mapping.MappingValidator;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;
import com.bigfito.elastiflix.ingest.mapping.ValidatedMovies;
import com.bigfito.elastiflix.ingest.service.IngestService;
import com.bigfito.elastiflix.ingest.service.LoadedDocuments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Paso 2 y paso 3: comprueba el archivo y lo valida entero, en un hilo virtual.
 */
@Component
public final class FileIntake {

    private static final Logger LOG = LoggerFactory.getLogger(FileIntake.class);
    private static final Set<ErrorCode> FILE_CHECKS = EnumSet.of(
            ErrorCode.BAD_EXTENSION, ErrorCode.DOUBLE_EXTENSION, ErrorCode.FILE_TOO_LARGE, ErrorCode.INVALID_UTF8);

    private final FileGate gate;

    private final MappingValidator validator;
    private final CatalogIndex catalog;
    private final ElastiflixProperties properties;
    private final JobStore store;

    /**
     * Crea la validación con las mismas reglas que un registro suelto.
     *
     * @param gate       comprobación y lectura
     * @param validator  reglas del mapping, el mismo bean que usa el registro individual
     * @param catalog    solo pregunta qué ids ya existen
     * @param properties alias del catálogo
     * @param store      dónde queda el informe
     */
    public FileIntake(
            FileGate gate,
            MappingValidator validator,
            CatalogIndex catalog,
            ElastiflixProperties properties,
            JobStore store) {
        this.gate = gate;
        this.validator = validator;
        this.catalog = catalog;
        this.properties = properties;
        this.store = store;
    }

    /**
     * Valida el trabajo. Un error de campos lo deja en {@code invalido} y no importa.
     *
     * @param jobId identificador que ya devolvió {@link IngestService#acceptFile}
     */
    public void validate(String jobId) {
        ImportWork work = store.require(jobId);
        work.validating();
        try {
            finish(jobId, work);
        } catch (IngestValidationException exception) {
            store.invalid(jobId, report(exception), stepOf(exception.errorCode()));
        } catch (ElastiflixException exception) {
            store.failed(jobId, exception);
        } finally {
            deleteUploaded(work);
            closeIfStill(jobId, JobStatus.VALIDATING);
        }
    }

    /** Después de validar el archivo ya no se relee: las películas quedan en el trabajo. */
    private static void deleteUploaded(ImportWork work) {
        try {
            Files.deleteIfExists(work.path());
        } catch (IOException exception) {
            LOG.warn("No se pudo borrar el archivo local del trabajo {} tipo={}", work.id(),
                    exception.getClass().getSimpleName());
        }
    }

    private void closeIfStill(String jobId, JobStatus open) {
        if (store.view(jobId).status().equals(open.label())) {
            store.failed(jobId, new ImportException(ErrorCode.JOB_FAILED));
        }
    }

    private void finish(String jobId, ImportWork work) {
        LoadedDocuments loaded = gate.open(work);
        store.fileRead(jobId, loaded.recordCount(), loaded.columns());
        if (loaded.documents().isEmpty() && !loaded.report().accepted()) {
            store.invalid(jobId, loaded.report(), JobSteps.VALIDATION);
            return;
        }
        ValidatedMovies checked = validator.validateAndRelease(loaded.documents(), loaded.labels(), Set.of());
        ValidationReport report = ReportOrder.join(loaded.report(), checked.report());
        if (!report.accepted()) {
            store.invalid(jobId, report, JobSteps.VALIDATION);
            return;
        }
        store.ready(jobId, new ValidatedMovies(checked.movies(), report, replacements(checked.movies())));
    }

    /**
     * Cuántos ids del archivo ya están en el catálogo. Se pregunta solo con el archivo ya válido: con
     * Elasticsearch caído, un archivo con errores igual muestra sus errores. Los ids no se repiten, porque
     * un id repetido es un error.
     */
    private int replacements(List<Movie> movies) {
        List<String> ids = new ArrayList<>();
        for (Movie movie : movies) {
            ids.add(movie.id());
        }
        return catalog.existingIds(properties.index().alias(), ids).size();
    }

    /** {@code EFX-ING-001} a {@code 004} son las comprobaciones del paso 2; lo demás ya es el paso 3. */
    private static int stepOf(ErrorCode code) {
        return FILE_CHECKS.contains(code) ? JobSteps.UPLOAD : JobSteps.VALIDATION;
    }

    private static ValidationReport report(IngestValidationException exception) {
        return new ValidationReport(List.of(new ValidationIssue(
                exception.errorCode().code(),
                "archivo",
                "—",
                "archivo",
                "—",
                exception.errorCode().summary(),
                false)));
    }
}
