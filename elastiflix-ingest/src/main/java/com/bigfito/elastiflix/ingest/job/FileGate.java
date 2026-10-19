package com.bigfito.elastiflix.ingest.job;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.IngestValidationException;
import com.bigfito.elastiflix.ingest.file.CheckedFile;
import com.bigfito.elastiflix.ingest.file.FileCandidate;
import com.bigfito.elastiflix.ingest.file.FileChecker;
import com.bigfito.elastiflix.ingest.service.LoadedDocuments;
import com.bigfito.elastiflix.ingest.service.MovieReaderFactory;

import org.springframework.stereotype.Component;

/**
 * Comprueba el nombre, el tamaño y el UTF-8, y después abre el lector.
 */
@Component
public final class FileGate {

    private final FileChecker checker = new FileChecker();
    private final MovieReaderFactory readers;
    private final ElastiflixProperties properties;

    /**
     * Crea la puerta con el tamaño máximo configurado.
     *
     * @param readers    json o csv
     * @param properties tamaño máximo del archivo
     */
    public FileGate(MovieReaderFactory readers, ElastiflixProperties properties) {
        this.readers = readers;
        this.properties = properties;
    }

    /**
     * Lee los documentos si el archivo pasa las comprobaciones.
     *
     * @param work trabajo que ya tiene la ruta
     * @return documentos y el informe del lector
     */
    public LoadedDocuments open(ImportWork work) {
        try {
            return read(work);
        } catch (IOException exception) {
            throw new IngestValidationException(unreadable(work.fileName()), exception);
        }
    }

    /** Un CSV que no se puede leer no es «El JSON no se puede leer». */
    private static ErrorCode unreadable(String fileName) {
        return JobFigures.isCsv(fileName) ? ErrorCode.UNREADABLE_CSV : ErrorCode.UNREADABLE_JSON;
    }

    private LoadedDocuments read(ImportWork work) throws IOException {
        Path path = work.path();
        CheckedFile checked = check(work, path);
        return readers.load(checked.kind(), path, work.settings());
    }

    private CheckedFile check(ImportWork work, Path path) throws IOException {
        try (InputStream content = Files.newInputStream(path)) {
            FileCandidate candidate = new FileCandidate(work.fileName(), Files.size(path), content);
            return checker.check(candidate, properties.ingest().maxFileSize());
        }
    }
}
