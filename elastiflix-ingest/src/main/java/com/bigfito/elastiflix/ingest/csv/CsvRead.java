package com.bigfito.elastiflix.ingest.csv;

import java.util.List;

import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;

/**
 * Películas leídas de un CSV y el informe de esa lectura.
 *
 * @param movies películas sin vectores; vacía si el encabezado no sirve
 * @param report errores y advertencias
 */
public record CsvRead(List<Movie> movies, ValidationReport report) {

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
