package com.bigfito.elastiflix.ingest.csv;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.mapping.FieldMapping;
import com.bigfito.elastiflix.core.mapping.IndexMapping;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprueba la primera fila de un CSV contra el mapping.
 */
final class CsvHeader {

    static final String ID = "id";
    static final String TITLE = "title";
    static final String PLOT = "plot";
    static final String DOCUMENT_ID = "_id";
    static final String HEADER = "encabezado";
    private static final String MISSING_ID = "—";
    private static final Set<String> VECTORS = Set.of("plot_256", "plot_512", "plot_1024");
    private static final Logger LOG = LoggerFactory.getLogger(CsvHeader.class);

    private final Set<String> allowed;

    CsvHeader(IndexMapping mapping) {
        Set<String> names = new HashSet<>();
        for (FieldMapping field : mapping.fields()) {
            names.add(field.name());
        }
        names.add(DOCUMENT_ID);
        allowed = Set.copyOf(names);
    }

    /**
     * Dice si la columna es un campo del mapping o el metadato {@code _id}.
     *
     * @param column nombre de la primera fila
     * @return {@code false} para una columna desconocida
     */
    boolean knows(String column) {
        return allowed.contains(column);
    }

    /**
     * Errores y advertencias del encabezado. No mira las filas de datos.
     *
     * @param columns nombres de la primera fila
     * @return incidencias; vacía si el encabezado sirve
     */
    List<ValidationIssue> check(List<String> columns) {
        List<ValidationIssue> issues = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String column : columns) {
            checkColumn(column, seen, issues);
        }
        require(columns, issues);
        return issues;
    }

    private void checkColumn(String column, Set<String> seen, List<ValidationIssue> issues) {
        if (column == null || column.isBlank()) {
            issues.add(error(ErrorCode.INVALID_HEADER, "", "un nombre vacío", "un nombre del mapping"));
            return;
        }
        if (!seen.add(column)) {
            issues.add(error(ErrorCode.INVALID_HEADER, column, "nombre repetido", "un nombre único"));
            return;
        }
        if (VECTORS.contains(column)) {
            LOG.warn("{}: se ignora la columna {}", ErrorCode.VECTORS_IGNORED.code(), column);
            issues.add(warning(column));
        }
        if (!allowed.contains(column)) {
            issues.add(error(ErrorCode.UNKNOWN_FIELD, column,
                    "Una columna llamada " + column,
                    "Ese campo no existe en el mapping del índice"));
        }
    }

    private static void require(List<String> columns, List<ValidationIssue> issues) {
        if (!columns.contains(ID) || !columns.contains(TITLE) || !columns.contains(PLOT)) {
            issues.add(error(ErrorCode.INVALID_HEADER, HEADER,
                    "faltan id, title o plot",
                    "la primera fila incluye id, title y plot"));
        }
    }

    private static ValidationIssue warning(String column) {
        return new ValidationIssue(
                ErrorCode.VECTORS_IGNORED.code(),
                HEADER,
                MISSING_ID,
                column,
                "columna " + column,
                "se generarán con el servicio de inferencia",
                true);
    }

    private static ValidationIssue error(ErrorCode code, String field, String received, String expected) {
        return new ValidationIssue(code.code(), HEADER, MISSING_ID, field, received, expected, false);
    }
}
