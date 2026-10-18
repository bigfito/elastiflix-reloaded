package com.bigfito.elastiflix.ingest.service;

import java.nio.file.Path;
import java.util.List;

import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.ingest.csv.CsvMovieReader;
import com.bigfito.elastiflix.ingest.csv.CsvRead;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.file.FileKind;
import com.bigfito.elastiflix.ingest.json.JsonDocuments;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;

import org.springframework.stereotype.Component;

/**
 * Elige el lector según la extensión y devuelve los objetos para el validador.
 */
@Component
public final class MovieReaderFactory {

    private final JsonDocuments json = new JsonDocuments();
    private final CsvMovieReader csv;

    /**
     * Crea la fábrica con los nombres de columna que el mapping permite.
     *
     * @param mapping campos del índice
     */
    public MovieReaderFactory(IndexMapping mapping) {
        csv = new CsvMovieReader(mapping);
    }

    /**
     * Lee el archivo. Si el CSV no trae encabezado válido, la lista viene vacía.
     *
     * @param kind     json o csv
     * @param path     archivo local
     * @param settings separador y casilla de encabezado
     * @return documentos y el informe del lector
     */
    public LoadedDocuments load(FileKind kind, Path path, CsvSettings settings) {
        if (kind == FileKind.JSON) {
            return new LoadedDocuments(json.read(path), ValidationReport.empty());
        }
        return csvDocuments(path, settings);
    }

    private LoadedDocuments csvDocuments(Path path, CsvSettings settings) {
        CsvRead read = csv.read(path, settings);
        if (!read.report().accepted()) {
            return new LoadedDocuments(List.of(), read.report());
        }
        return new LoadedDocuments(read.documents(), read.report());
    }
}
