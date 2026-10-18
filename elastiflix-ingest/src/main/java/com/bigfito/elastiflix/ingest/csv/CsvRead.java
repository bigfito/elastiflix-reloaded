package com.bigfito.elastiflix.ingest.csv;

import java.util.List;

import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;

import tools.jackson.databind.JsonNode;

/**
 * Filas leídas de un CSV, todavía sin convertir a película, y el informe de esa lectura.
 *
 * @param documents un objeto por fila, con el texto crudo de cada celda; vacío si el encabezado no sirve
 * @param report    errores y advertencias del encabezado y del ancho de las filas
 * @param rowCount  filas de datos que no están en blanco, incluidas las de ancho incorrecto
 * @param columns   columnas del encabezado con sus celdas llenas; vacía si no se leyeron filas
 */
public record CsvRead(List<JsonNode> documents, ValidationReport report, int rowCount, List<CsvColumn> columns) {

    /**
     * Copia las listas para que nadie las cambie después.
     */
    public CsvRead {
        documents = List.copyOf(documents);
        columns = List.copyOf(columns);
    }

    /**
     * Un rechazo con una sola incidencia y ninguna película.
     *
     * @param issue error que detiene la lectura
     * @return lectura rechazada
     */
    public static CsvRead rejected(ValidationIssue issue) {
        return new CsvRead(List.of(), new ValidationReport(List.of(issue)), 0, List.of());
    }
}
