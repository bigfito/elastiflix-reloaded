package com.bigfito.elastiflix.ingest.csv;

import java.util.List;

import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;

import tools.jackson.databind.JsonNode;

/**
 * Filas leídas de un CSV, todavía sin convertir a película, y el informe de esa lectura.
 *
 * @param documents un objeto por fila, con el texto crudo de cada celda; vacío si el encabezado no sirve
 * @param report    errores y advertencias del encabezado
 */
public record CsvRead(List<JsonNode> documents, ValidationReport report) {

    /**
     * Un rechazo con una sola incidencia y ninguna película.
     *
     * @param issue error que detiene la lectura
     * @return lectura rechazada
     */
    public static CsvRead rejected(ValidationIssue issue) {
        return new CsvRead(List.of(), new ValidationReport(List.of(issue)));
    }
}
