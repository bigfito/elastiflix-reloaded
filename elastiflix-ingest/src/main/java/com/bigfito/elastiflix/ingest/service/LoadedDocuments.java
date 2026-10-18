package com.bigfito.elastiflix.ingest.service;

import java.util.List;

import com.bigfito.elastiflix.ingest.csv.CsvColumnRow;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;

import tools.jackson.databind.JsonNode;

/**
 * Documentos leídos y el informe que ya produjo el lector, antes del mapping.
 *
 * @param documents   un objeto JSON por película; el validador la vacía a medida que revisa
 * @param report      errores de encabezado o de forma; vacío si el lector no encontró
 * @param recordCount registros (JSON) o filas de datos (CSV) que se leyeron
 * @param columns     tabla de correspondencia del CSV; vacía en JSON
 */
public record LoadedDocuments(
        List<JsonNode> documents, ValidationReport report, int recordCount, List<CsvColumnRow> columns) {

    /**
     * Copia la tabla de columnas. La lista de documentos no se copia: el validador la va vaciando.
     */
    public LoadedDocuments {
        columns = List.copyOf(columns);
    }
}
