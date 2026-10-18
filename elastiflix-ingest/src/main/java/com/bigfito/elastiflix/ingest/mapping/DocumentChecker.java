package com.bigfito.elastiflix.ingest.mapping;

import java.util.List;
import java.util.Set;

import com.bigfito.elastiflix.core.error.ErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Revisa un documento y arma el objeto que después se convierte en película.
 */
final class DocumentChecker {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentChecker.class);
    private static final String NOT_IN_MAPPING = "Ese campo no existe en el mapping del índice";

    CheckedDocument check(JsonNode node, String record, Set<String> warnedVectors) {
        if (!(node instanceof ObjectNode object)) {
            return notObject(record);
        }
        return checkObject(object, record, warnedVectors);
    }

    private CheckedDocument checkObject(ObjectNode object, String record, Set<String> warnedVectors) {
        String id = idOf(object);
        CheckScope scope = new CheckScope(MovieNodes.MAPPER.createObjectNode(), record, id);
        object.forEachEntry((name, value) -> write(name, value, scope, warnedVectors));
        requireFields(scope);
        matchDocumentId(object, id, scope);
        return new CheckedDocument(id, scope.target(), scope.issues());
    }

    private static CheckedDocument notObject(String record) {
        ValidationIssue issue = new ValidationIssue(
                ErrorCode.TYPE_MISMATCH.code(),
                record,
                FieldNames.MISSING,
                "documento",
                "no es un objeto",
                "un objeto de película",
                false);
        return new CheckedDocument(FieldNames.MISSING, MovieNodes.MAPPER.createObjectNode(), List.of(issue));
    }

    private static String idOf(ObjectNode object) {
        String id = IdTexts.of(object.get(FieldNames.ID));
        if (id == null) {
            return FieldNames.MISSING;
        }
        return id;
    }

    private void write(String name, JsonNode value, CheckScope scope, Set<String> warnedVectors) {
        if (FieldNames.DOCUMENT_ID.equals(name)) {
            return;
        }
        if (FieldRules.vector(name)) {
            warnVector(name, scope, warnedVectors);
            return;
        }
        copy(name, value, scope);
    }

    private void warnVector(String name, CheckScope scope, Set<String> warnedVectors) {
        if (!warnedVectors.add(name)) {
            return;
        }
        LOG.warn("{}: se ignora la columna {}", ErrorCode.VECTORS_IGNORED.code(), name);
        scope.issues().add(VectorFields.ignored(name));
    }

    private void copy(String name, JsonNode value, CheckScope scope) {
        if (!FieldRules.known(name)) {
            scope.error(ErrorCode.UNKNOWN_FIELD, name, "Un campo llamado " + name, NOT_IN_MAPPING);
            return;
        }
        if (scalar(FieldRules.kind(name))) {
            ScalarValues.write(FieldRules.kind(name), name, value, scope);
            return;
        }
        NestedValues.write(FieldRules.kind(name), name, value, scope);
    }

    private static boolean scalar(FieldKind kind) {
        return kind == FieldKind.TEXT
                || kind == FieldKind.KEYWORD
                || kind == FieldKind.BOOLEAN
                || kind == FieldKind.INTEGER
                || kind == FieldKind.DECIMAL
                || kind == FieldKind.DATE;
    }

    private void requireFields(CheckScope scope) {
        for (String name : FieldNames.REQUIRED) {
            requireOne(name, scope);
        }
    }

    private void requireOne(String name, CheckScope scope) {
        if (scope.target().has(name) || reported(scope, name)) {
            return;
        }
        scope.error(ErrorCode.REQUIRED_FIELD, name, FieldNames.ABSENT, "id, title y plot");
    }

    private static boolean reported(CheckScope scope, String name) {
        for (ValidationIssue issue : scope.issues()) {
            if (name.equals(issue.field()) && !issue.warning()) {
                return true;
            }
        }
        return false;
    }

    private void matchDocumentId(ObjectNode source, String id, CheckScope scope) {
        JsonNode meta = source.get(FieldNames.DOCUMENT_ID);
        if (meta == null || meta.isNull()) {
            return;
        }
        compare(id, IdTexts.of(meta), scope);
    }

    private void compare(String id, String metaId, CheckScope scope) {
        if (metaId == null) {
            scope.error(ErrorCode.TYPE_MISMATCH, FieldNames.DOCUMENT_ID, "no es texto", "el mismo texto que id");
            return;
        }
        if (!metaId.equals(id)) {
            scope.error(ErrorCode.ID_MISMATCH, FieldNames.DOCUMENT_ID, metaId, id);
        }
    }
}
