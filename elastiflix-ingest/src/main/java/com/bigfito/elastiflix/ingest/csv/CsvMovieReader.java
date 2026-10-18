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
import com.bigfito.elastiflix.ingest.mapping.CsvValues;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;
import com.bigfito.elastiflix.ingest.mapping.VectorFields;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Lee un CSV de películas. La primera fila tiene que ser el encabezado.
 *
 * <p>Las listas y los objetos van como JSON dentro de la celda. Las columnas de vector se
 * aceptan y se ignoran, con una advertencia por columna. Una columna que no está en el mapping es
 * un error del encabezado, pero las filas se leen igual sin esa columna: así el informe cuenta las
 * filas y trae también los errores de los demás campos.</p>
 */
public final class CsvMovieReader {

    private static final char BOM = '﻿';

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
     * Lee las filas como objetos JSON. Si el encabezado no sirve, la lista viene vacía.
     *
     * @param path     archivo CSV
     * @param settings separador y casilla de encabezado
     * @return objetos crudos, con {@code _id} y los subcampos, el informe y el conteo por columna
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
            throw new IngestValidationException(ErrorCode.UNREADABLE_CSV, exception);
        }
    }

    private CsvRead readRows(CsvRows rows) {
        Optional<List<String>> columns = rows.nextRow();
        if (columns.isEmpty()) {
            return CsvRead.rejected(issue(ErrorCode.INVALID_HEADER));
        }
        List<ValidationIssue> issues = header.check(columns.get());
        if (stopsReading(issues)) {
            return new CsvRead(List.of(), new ValidationReport(issues), 0, List.of());
        }
        ColumnTally tally = new ColumnTally(columns.get());
        List<JsonNode> documents = documents(rows, columns.get(), issues, tally);
        return new CsvRead(documents, new ValidationReport(issues), tally.rows(), tally.columns());
    }

    /** Solo una columna desconocida deja seguir: el resto de los errores del encabezado impide leer. */
    private static boolean stopsReading(List<ValidationIssue> issues) {
        for (ValidationIssue issue : issues) {
            if (!issue.warning() && !ErrorCode.UNKNOWN_FIELD.code().equals(issue.code())) {
                return true;
            }
        }
        return false;
    }

    private List<JsonNode> documents(
            CsvRows rows, List<String> columns, List<ValidationIssue> issues, ColumnTally tally) {
        List<JsonNode> documents = new ArrayList<>();
        Optional<List<String>> row = rows.nextRow();
        int number = 2;
        while (row.isPresent()) {
            if (!blank(row.get())) {
                tally.count(row.get());
                addRow(documents, issues, columns, row.get(), number);
            }
            number++;
            row = rows.nextRow();
        }
        return documents;
    }

    private void addRow(
            List<JsonNode> documents,
            List<ValidationIssue> issues,
            List<String> columns,
            List<String> row,
            int number) {
        if (row.size() != columns.size()) {
            issues.add(width(number, columns.size(), row.size()));
            return;
        }
        documents.add(document(columns, row));
    }

    private static ValidationIssue width(int number, int expected, int received) {
        return new ValidationIssue(
                ErrorCode.COLUMN_COUNT.code(),
                Integer.toString(number),
                "—",
                "",
                Integer.toString(received),
                Integer.toString(expected) + " celdas",
                false);
    }

    private ObjectNode document(List<String> columns, List<String> row) {
        ObjectNode object = CsvValues.newObject();
        for (int index = 0; index < columns.size(); index++) {
            String name = columns.get(index);
            // Los vectores no se convierten: la advertencia ya está en el encabezado y se generan desde plot.
            if (header.knows(name) && !VectorFields.isVector(name)) {
                put(object, name, row.get(index));
            }
        }
        return object;
    }

    private static void put(ObjectNode object, String name, String cell) {
        if (cell.isBlank()) {
            return;
        }
        JsonNode value = CsvValues.read(name, cell);
        if (value != null) {
            object.set(name, value);
        }
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
