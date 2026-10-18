package com.bigfito.elastiflix.ingest.csv;

import java.util.ArrayList;
import java.util.List;

/**
 * Cuenta las filas de datos y las celdas llenas de cada columna, para la tabla de correspondencia.
 */
final class ColumnTally {

    private final List<String> names;
    private final int[] filled;
    private int rows;

    ColumnTally(List<String> names) {
        this.names = List.copyOf(names);
        this.filled = new int[names.size()];
    }

    void count(List<String> row) {
        rows++;
        int cells = Math.min(row.size(), filled.length);
        for (int index = 0; index < cells; index++) {
            if (!row.get(index).isBlank()) {
                filled[index]++;
            }
        }
    }

    int rows() {
        return rows;
    }

    List<CsvColumn> columns() {
        List<CsvColumn> columns = new ArrayList<>();
        for (int index = 0; index < filled.length; index++) {
            columns.add(new CsvColumn(names.get(index), filled[index]));
        }
        return List.copyOf(columns);
    }
}
