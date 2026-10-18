package com.bigfito.elastiflix.ingest.csv;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bigfito.elastiflix.core.mapping.FieldMapping;
import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.ingest.mapping.CsvValues;
import com.bigfito.elastiflix.ingest.mapping.Thousands;

/**
 * Arma la tabla «Correspondencia de columnas»: cada columna del CSV va al campo del mismo nombre.
 */
public final class CsvColumnRows {

    /** La columna existe en el mapping y trae datos. */
    public static final String ASSIGNED = "Asignada";

    /** Columna de vector: se genera desde {@code plot}. */
    public static final String IGNORED = "Se ignora";

    /** La columna está vacía en todas las filas. */
    public static final String NO_DATA = "Sin datos";

    /** El nombre no es un campo del mapping. */
    public static final String UNKNOWN = "No existe en el mapping";

    private static final String VECTOR_TYPE = "dense_vector";
    private static final Set<String> LISTS = Set.of("keywords");
    private static final Map<String, String> KINDS = Map.of(
            "text", "Texto",
            "keyword", "Texto",
            "integer", "Número entero",
            "long", "Número entero",
            "float", "Número",
            "boolean", "true o false",
            "date", "Fecha yyyy-MM-dd");

    private final IndexMapping mapping;

    /**
     * Crea el armado con los tipos del mapping.
     *
     * @param mapping campos del índice
     */
    public CsvColumnRows(IndexMapping mapping) {
        this.mapping = mapping;
    }

    /**
     * Una fila por columna, en el orden del encabezado.
     *
     * @param columns  columnas con sus celdas llenas
     * @param rowCount filas de datos leídas
     * @return filas de la tabla; vacía si no hay columnas
     */
    public List<CsvColumnRow> of(List<CsvColumn> columns, int rowCount) {
        List<CsvColumnRow> rows = new ArrayList<>();
        for (CsvColumn column : columns) {
            rows.add(row(column, rowCount));
        }
        return List.copyOf(rows);
    }

    private CsvColumnRow row(CsvColumn column, int rowCount) {
        String name = column.name();
        if (CsvHeader.DOCUMENT_ID.equals(name)) {
            return new CsvColumnRow(name, "(id del documento)", "Metadato; tiene que ser igual a id", ASSIGNED);
        }
        FieldMapping field = find(name);
        if (field == null) {
            return new CsvColumnRow(name, "— Sin campo en el índice —", "Texto", UNKNOWN);
        }
        return known(field, column.filled(), rowCount);
    }

    private static CsvColumnRow known(FieldMapping field, int filled, int rowCount) {
        String name = field.name();
        if (VECTOR_TYPE.equals(field.type())) {
            return new CsvColumnRow(name, name, "Se ignora · se generará desde plot", IGNORED);
        }
        if (filled == 0 && rowCount > 0) {
            return new CsvColumnRow(name, name, "Vacía en todas las filas", NO_DATA);
        }
        return new CsvColumnRow(name, name, content(field, rowCount - filled), ASSIGNED);
    }

    private FieldMapping find(String name) {
        for (FieldMapping field : mapping.fields()) {
            if (field.name().equals(name)) {
                return field;
            }
        }
        return null;
    }

    private static String content(FieldMapping field, int empty) {
        String kind = kind(field);
        if (empty <= 0) {
            return kind;
        }
        return kind + " · vacía en " + Thousands.of(empty) + (empty == 1 ? " fila" : " filas");
    }

    private static String kind(FieldMapping field) {
        if (CsvHeader.ID.equals(field.name())) {
            return "Texto o número, se guarda como texto (keyword)";
        }
        if (CsvValues.isTextList(field.name())) {
            return "Lista JSON de textos";
        }
        return "nested".equals(field.type()) ? nested(field) : KINDS.getOrDefault(field.type(), field.type());
    }

    private static String nested(FieldMapping field) {
        return LISTS.contains(field.name()) ? "Lista JSON de objetos anidados" : "Objeto JSON anidado";
    }
}
