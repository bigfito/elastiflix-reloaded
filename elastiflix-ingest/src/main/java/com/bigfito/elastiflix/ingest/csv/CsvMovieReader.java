package com.bigfito.elastiflix.ingest.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.IngestValidationException;
import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;
import com.bigfito.elastiflix.ingest.movie.MovieDocuments;

import tools.jackson.databind.node.ObjectNode;

/**
 * Lee un CSV de películas. La primera fila tiene que ser el encabezado.
 *
 * <p>Las listas y los objetos van como JSON dentro de la celda. Las columnas de vector se
 * aceptan y se ignoran, con una advertencia por columna.</p>
 */
public final class CsvMovieReader {

    private static final char BOM = '\uFEFF';

    private final CsvHeader header;

    /**
     * Crea el lector con los nombres que el mapping permite.
     *
     * @param mapping campos del índice
     */
    public CsvMovieReader(IndexMapping mapping) {
        header = new CsvHeader(mapping);
    }

    /**
     * Lee las películas. Si el encabezado no sirve, la lista viene vacía y el informe trae el error.
     *
     * @param path     archivo CSV
     * @param settings separador y casilla de encabezado
     * @return películas sin vectores, y el informe
     */
    public CsvRead read(Path path, CsvSettings settings) {
        if (!settings.headerPresent()) {
            return CsvRead.rejected(issue(ErrorCode.HEADER_NOT_CONFIRMED));
        }
        try (BufferedReader input = Files.newBufferedReader(path, StandardCharsets.UTF_8);
                CsvRows rows = new CsvRows(input, settings.separator())) {
            skipBom(input);
            return readRows(rows);
        } catch (IOException exception) {
            throw new IngestValidationException(ErrorCode.UNREADABLE_JSON, exception);
        }
    }

    private CsvRead readRows(CsvRows rows) {
        Optional<List<String>> columns = rows.nextRow();
        if (columns.isEmpty()) {
            return CsvRead.rejected(issue(ErrorCode.INVALID_HEADER));
        }
        List<ValidationIssue> issues = header.check(columns.get());
        if (!new ValidationReport(issues).accepted()) {
            return new CsvRead(List.of(), new ValidationReport(issues));
        }
        return new CsvRead(movies(rows, columns.get()), new ValidationReport(issues));
    }

    private static List<Movie> movies(CsvRows rows, List<String> columns) {
        List<Movie> movies = new ArrayList<>();
        Optional<List<String>> row = rows.nextRow();
        while (row.isPresent()) {
            if (!blank(row.get())) {
                movies.add(movie(columns, row.get()));
            }
            row = rows.nextRow();
        }
        return movies;
    }

    private static Movie movie(List<String> columns, List<String> row) {
        ObjectNode object = CsvCells.mapper().createObjectNode();
        int fields = Math.min(columns.size(), row.size());
        for (int index = 0; index < fields; index++) {
            put(object, columns.get(index), row.get(index));
        }
        return MovieDocuments.fromNode(object);
    }

    private static void put(ObjectNode object, String name, String cell) {
        if (CsvHeader.DOCUMENT_ID.equals(name) || cell.isEmpty()) {
            return;
        }
        if (name.startsWith("plot_")) {
            return;
        }
        object.set(name, CsvCells.toNode(cell));
    }

    private static boolean blank(List<String> row) {
        for (String cell : row) {
            if (!cell.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void skipBom(BufferedReader input) throws IOException {
        input.mark(1);
        if (input.read() != BOM) {
            input.reset();
        }
    }

    private static ValidationIssue issue(ErrorCode code) {
        return new ValidationIssue(code.code(), CsvHeader.HEADER, "—", "", "", code.userMessage(), false);
    }
}
