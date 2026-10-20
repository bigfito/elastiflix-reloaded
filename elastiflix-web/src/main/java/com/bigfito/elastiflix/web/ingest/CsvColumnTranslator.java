package com.bigfito.elastiflix.web.ingest;

import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.ingest.csv.CsvColumnRow;
import com.bigfito.elastiflix.ingest.csv.CsvColumnRows;

/**
 * Traduce las columnas CSV detectadas y sus estados y contenidos al inglés.
 */
final class CsvColumnTranslator {

    private CsvColumnTranslator() {
    }

    /**
     * Traduce una lista de filas de correspondencia de columnas CSV.
     *
     * @param columns lista original de columnas
     * @return lista traducida
     */
    static List<CsvColumnRow> translateColumns(List<CsvColumnRow> columns) {
        if (columns == null || columns.isEmpty()) {
            return List.of();
        }
        List<CsvColumnRow> list = new ArrayList<>(columns.size());
        for (CsvColumnRow row : columns) {
            list.add(translateColumn(row));
        }
        return List.copyOf(list);
    }

    private static CsvColumnRow translateColumn(CsvColumnRow row) {
        return new CsvColumnRow(
                row.column(),
                translateField(row.field()),
                translateContent(row.content()),
                translateStatus(row.status()));
    }

    private static String translateField(String field) {
        if (field == null) {
            return "";
        }
        if ("— Sin campo en el índice —".equals(field)) {
            return "— No field in the index —";
        }
        return "(id del documento)".equals(field) ? "(document id)" : field;
    }

    private static String translateStatus(String status) {
        return switch (status) {
            case CsvColumnRows.ASSIGNED -> "Mapped";
            case CsvColumnRows.IGNORED -> "Ignored";
            case CsvColumnRows.NO_DATA -> "No data";
            case CsvColumnRows.UNKNOWN -> "Not in the mapping";
            case null -> "";
            default -> status;
        };
    }

    private static String translateContent(String content) {
        if (content == null) {
            return "";
        }
        if (content.contains(" · vacía en ")) {
            String[] parts = content.split(" · vacía en ", 2);
            String empty = parts[1].replace("filas", "rows").replace("fila", "row");
            return translateKind(parts[0]) + " · empty in " + empty;
        }
        return switch (content) {
            case "Metadato; tiene que ser igual a id" -> "Same as id in every row";
            case "Texto o número, se guarda como texto (keyword)" -> "Number, stored as text (keyword)";
            case "Se ignora · se generará desde plot" -> "Ignored · generated from plot";
            case "Vacía en todas las filas" -> "Empty in every row";
            default -> translateKind(content);
        };
    }

    private static String translateKind(String kind) {
        return switch (kind) {
            case "Texto" -> "Text";
            case "Número entero" -> "Integer";
            case "Número" -> "Number";
            case "true o false" -> "true or false";
            case "Fecha yyyy-MM-dd" -> "Date yyyy-MM-dd";
            case "Lista JSON de textos" -> "JSON list of strings";
            case "Lista JSON de objetos anidados" -> "JSON list of nested objects";
            case "Objeto JSON anidado" -> "Nested JSON object";
            default -> kind;
        };
    }
}
