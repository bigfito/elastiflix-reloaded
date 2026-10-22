package com.bigfito.elastiflix.ingest.json;

import java.util.List;

import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;

import tools.jackson.databind.JsonNode;

/**
 * Objetos leídos de un JSON, ya sin vectores, y las advertencias de esa lectura.
 *
 * @param documents un nodo por película, en el orden del archivo; la lista se puede vaciar
 * @param warnings  una advertencia {@code EFX-ING-020} por cada vector que traía el archivo
 */
public record JsonRead(List<JsonNode> documents, List<ValidationIssue> warnings) {
}
