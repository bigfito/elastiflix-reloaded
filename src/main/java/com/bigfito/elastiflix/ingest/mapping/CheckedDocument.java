package com.bigfito.elastiflix.ingest.mapping;

import java.util.List;

import tools.jackson.databind.node.ObjectNode;

/**
 * Un registro ya revisado, con su objeto listo para convertirse en película.
 *
 * @param id     id leído, o un guion si no había
 * @param body   objeto sin vectores ni campos inválidos
 * @param issues errores y advertencias de este registro
 */
record CheckedDocument(String id, ObjectNode body, List<ValidationIssue> issues) {
}
