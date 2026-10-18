package com.bigfito.elastiflix.ingest.service;

import java.nio.file.Path;
import java.util.List;

import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.ingest.csv.CsvColumnRows;
import com.bigfito.elastiflix.ingest.csv.CsvMovieReader;
import com.bigfito.elastiflix.ingest.csv.CsvRead;
import com.bigfito.elastiflix.ingest.csv.CsvSettings;
import com.bigfito.elastiflix.ingest.file.FileKind;
import com.bigfito.elastiflix.ingest.json.JsonDocuments;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;

import tools.jackson.databind.JsonNode;

import org.springframework.stereotype.Component;

/**
 * Elige el lector según la extensión y devuelve los objetos para el validador.
 */
@Component
public final class MovieReaderFactory {

    private final JsonDocuments json = new JsonDocuments();
    private final CsvMovieReader csv;
    private final CsvColumnRows columnRows;

    /**
     * Crea la fábrica con los nombres de columna que el mapping permite.
     *
     * @param mapping campos del índice
     */
    public MovieReaderFactory(IndexMapping mapping) {
        csv = new CsvMovieReader(mapping);
        columnRows = new CsvColumnRows(mapping);
    }

    /**
     * Lee el archivo. Si el encabezado del CSV no se puede usar, la lista viene vacía.
     *
     * @param kind     json o csv
     * @param path     archivo local
     * @param settings separador y casilla de encabezado
     * @return documentos y el informe del lector
     */
    public LoadedDocuments load(FileKind kind, Path path, CsvSettings settings) {
        if (kind == FileKind.JSON) {
            List<JsonNode> documents = json.read(path);
            return new LoadedDocuments(documents, ValidationReport.empty(), documents.size(), List.of());
        }
        return csvDocuments(path, settings);
    }

    private LoadedDocuments csvDocuments(Path path, CsvSettings settings) {
        CsvRead read = csv.read(path, settings);
        return new LoadedDocuments(read.documents(), read.report(), read.rowCount(),
                columnRows.of(read.columns(), read.rowCount()));
    }
}
