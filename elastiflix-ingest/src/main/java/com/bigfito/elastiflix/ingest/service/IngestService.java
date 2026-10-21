package com.bigfito.elastiflix.ingest.service;

import java.nio.file.Path;
import java.util.function.UnaryOperator;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.job.FileIntake;
import com.bigfito.elastiflix.ingest.job.ImportRunner;
import com.bigfito.elastiflix.ingest.job.IndividualRecord;
import com.bigfito.elastiflix.ingest.job.JobStore;
import com.bigfito.elastiflix.ingest.job.JobView;
import com.bigfito.elastiflix.ingest.job.SaveResult;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;

import org.springframework.stereotype.Component;

/**
 * Puerta de la carga. La página llama aquí y no ve lectores, lotes ni el cliente.
 */
@Component
public final class IngestService {

    private final JobStore store;
    private final FileIntake intake;
    private final ImportRunner runner;
    private final IndividualRecord records;
    private final ElastiflixProperties properties;

    /**
     * Crea la fachada.
     *
     * @param store      trabajos de esta JVM
     * @param intake     validación en segundo plano
     * @param runner     importación en segundo plano
     * @param records    un solo registro, directo al alias
     * @param properties alias configurado
     */
    public IngestService(
            JobStore store,
            FileIntake intake,
            ImportRunner runner,
            IndividualRecord records,
            ElastiflixProperties properties) {
        this.store = store;
        this.intake = intake;
        this.runner = runner;
        this.records = records;
        this.properties = properties;
    }

    /**
     * Guarda el archivo y devuelve el id. La comprobación sigue en un hilo virtual.
     *
     * @param fileName nombre original, sin la ruta temporal
     * @param path     archivo ya copiado al disco local
     * @param settings separador y encabezado; en JSON se ignoran
     * @return identificador del trabajo
     */
    public String acceptFile(String fileName, Path path, CsvSettings settings) {
        String jobId = store.open(fileName, path, settings);
        JobThreads.start("ingest-validate-" + jobId, () -> intake.validate(jobId));
        return jobId;
    }

    /**
     * Copia del progreso. Si el id no está, {@code EFX-IMP-005}.
     *
     * @param jobId identificador devuelto por {@link #acceptFile}
     * @return estado, conteos y los primeros errores
     */
    public JobView job(String jobId) {
        return store.view(jobId);
    }

    /**
     * Arranca la importación solo si el estado es {@code valido}.
     *
     * @param jobId identificador
     */
    public void importFile(String jobId) {
        if (store.beginImport(jobId)) {
            JobThreads.start("ingest-import-" + jobId, () -> runner.run(jobId));
        }
    }

    /**
     * Pide cancelar. No interrumpe una llamada que ya salió hacia Elasticsearch.
     *
     * @param jobId identificador
     */
    public void cancel(String jobId) {
        store.cancel(jobId);
    }

    /**
     * CSV de todos los errores de ese trabajo.
     *
     * @param jobId identificador
     * @return texto UTF-8 con encabezado
     */
    public String errorReport(String jobId) {
        return store.errorReport(jobId);
    }

    /**
     * Informe de errores con otro encabezado y cada error reescrito; la web lo pide en el idioma de la persona.
     *
     * @param jobId   identificador
     * @param header  primera fila del CSV
     * @param rewrite cambia el texto de cada error
     * @return CSV con BOM
     */
    public String errorReport(String jobId, String header, UnaryOperator<ValidationIssue> rewrite) {
        return store.errorReport(jobId, header, rewrite);
    }

    /**
     * Valida un registro, le genera los vectores y lo escribe en el alias.
     *
     * @param movie campos del formulario; los vectores no se usan
     * @return la película guardada, o el informe
     */
    public SaveResult saveOne(Movie movie) {
        return records.save(properties.index().alias(), movie);
    }
}
