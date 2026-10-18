package com.bigfito.elastiflix.ingest.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.bigfito.elastiflix.core.catalog.CatalogIndex;
import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.core.error.IngestValidationException;
import com.bigfito.elastiflix.ingest.mapping.MappingValidator;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;
import com.bigfito.elastiflix.ingest.mapping.ValidatedMovies;
import com.bigfito.elastiflix.ingest.service.IngestService;
import com.bigfito.elastiflix.ingest.service.LoadedDocuments;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

/**
 * Paso 2 y paso 3: comprueba el archivo y lo valida entero, en un hilo virtual.
 */
@Component
public final class FileIntake {

    private final FileGate gate;
    private final MappingValidator validator = new MappingValidator();
    private final CatalogIndex catalog;
    private final ElastiflixProperties properties;
    private final JobStore store;

    /**
     * Crea la validación con las mismas reglas que un registro suelto.
     *
     * @param gate       comprobación y lectura
     * @param catalog    solo pregunta qué ids ya existen
     * @param properties alias del catálogo
     * @param store      dónde queda el informe
     */
    public FileIntake(FileGate gate, CatalogIndex catalog, ElastiflixProperties properties, JobStore store) {
        this.gate = gate;
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
            store.invalid(jobId, report(exception));
        } catch (ElastiflixException exception) {
            store.failed(jobId, exception);
        } finally {
            closeIfStill(jobId, JobStatus.VALIDATING);
        }
    }

    private void closeIfStill(String jobId, JobStatus open) {
        if (store.view(jobId).status().equals(open.label())) {
            store.failed(jobId, new ImportException(ErrorCode.JOB_FAILED));
        }
    }

    private void finish(String jobId, ImportWork work) {
        LoadedDocuments loaded = gate.open(work);
        if (!loaded.report().accepted()) {
            store.invalid(jobId, loaded.report());
            return;
        }
        ValidatedMovies checked = validator.validate(loaded.documents(), existing(loaded.documents()));
        storeOutcome(jobId, loaded.report(), checked);
    }

    private void storeOutcome(String jobId, ValidationReport header, ValidatedMovies checked) {
        ValidatedMovies merged = new ValidatedMovies(
                checked.movies(), join(header, checked.report()), checked.replaceCount());
        if (merged.report().accepted()) {
            store.ready(jobId, merged);
            return;
        }
        store.invalid(jobId, merged.report());
    }

    private Set<String> existing(List<JsonNode> documents) {
        return catalog.existingIds(properties.index().alias(), ids(documents));
    }

    private static List<String> ids(List<JsonNode> documents) {
        List<String> ids = new ArrayList<>();
        for (JsonNode document : documents) {
            addId(ids, document.get("id"));
        }
        return ids;
    }

    private static void addId(List<String> ids, JsonNode id) {
        if (id == null || id.isNull()) {
            return;
        }
        String text = id.asString().trim();
        if (!text.isEmpty()) {
            ids.add(text);
        }
    }

    private static ValidationReport join(ValidationReport header, ValidationReport fields) {
        List<ValidationIssue> issues = new ArrayList<>();
        issues.addAll(header.issues());
        issues.addAll(fields.issues());
        return new ValidationReport(issues);
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
