package com.bigfito.elastiflix.ingest.service;

import java.util.List;

import com.bigfito.elastiflix.ingest.mapping.ValidationReport;

import tools.jackson.databind.JsonNode;

/**
 * Documentos leídos y el informe que ya produjo el lector, antes del mapping.
 *
 * @param documents un objeto JSON por película
 * @param report    errores de encabezado o de forma; vacío si el lector no encontró
 */
public record LoadedDocuments(List<JsonNode> documents, ValidationReport report) {
}
