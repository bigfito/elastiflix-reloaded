package com.bigfito.elastiflix.ingest.csv;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bigfito.elastiflix.core.mapping.FieldMapping;
import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.ingest.mapping.CsvValues;
import com.bigfito.elastiflix.ingest.mapping.IngestPhrase;

/**
 * Arma la tabla «Correspondencia de columnas»: cada columna del CSV va al campo del mismo nombre.
 */
public final class CsvColumnRows {

    /** La columna existe en el mapping y trae datos. */
    public static final String ASSIGNED = IngestPhrase.MAPPED.text();

    /** Columna de vector: se genera desde {@code plot}. */
    public static final String IGNORED = IngestPhrase.IGNORED.text();

    /** La columna está vacía en todas las filas. */
    public static final String NO_DATA = IngestPhrase.NO_DATA.text();

    /** El nombre no es un campo del mapping. */
    public static final String UNKNOWN = IngestPhrase.UNKNOWN_COLUMN.text();

    private static final String VECTOR_TYPE = "dense_vector";
    private static final Set<String> LISTS = Set.of("keywords");
    private static final Map<String, String> KINDS = Map.of(
            "text", IngestPhrase.KIND_TEXT.text(),
            "keyword", IngestPhrase.KIND_TEXT.text(),
            "integer", IngestPhrase.KIND_INTEGER.text(),
            "long", IngestPhrase.KIND_INTEGER.text(),
            "float", IngestPhrase.KIND_NUMBER.text(),
            "boolean", IngestPhrase.BOOLEAN.text(),
            "date", IngestPhrase.KIND_DATE.text());

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
            return new CsvColumnRow(
                    name, IngestPhrase.DOCUMENT_ID.text(), IngestPhrase.SAME_AS_ID_META.text(), ASSIGNED);
        }
        FieldMapping field = find(name);
        if (field == null) {
            return new CsvColumnRow(name, IngestPhrase.NO_INDEX_FIELD.text(), IngestPhrase.KIND_TEXT.text(), UNKNOWN);
        }
        return known(field, column.filled(), rowCount);
    }

    private static CsvColumnRow known(FieldMapping field, int filled, int rowCount) {
        String name = field.name();
        if (VECTOR_TYPE.equals(field.type())) {
            return new CsvColumnRow(name, name, IngestPhrase.VECTOR_IGNORED.text(), IGNORED);
        }
        if (filled == 0 && rowCount > 0) {
            return new CsvColumnRow(name, name, IngestPhrase.EMPTY_IN_ALL_ROWS.text(), NO_DATA);
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
        IngestPhrase phrase = empty == 1 ? IngestPhrase.EMPTY_IN_ONE_ROW : IngestPhrase.EMPTY_IN_ROWS;
        return phrase.text(kind, empty);
    }

    private static String kind(FieldMapping field) {
        if (CsvHeader.ID.equals(field.name())) {
            return IngestPhrase.KIND_ID.text();
        }
        if (CsvValues.isTextList(field.name())) {
            return IngestPhrase.KIND_TEXT_LIST.text();
        }
        boolean object = "nested".equals(field.type()) || "object".equals(field.type());
        return object ? nested(field) : KINDS.getOrDefault(field.type(), field.type());
    }

    private static String nested(FieldMapping field) {
        return LISTS.contains(field.name())
                ? IngestPhrase.KIND_NESTED_LIST.text() : IngestPhrase.KIND_NESTED_OBJECT.text();
    }
}
