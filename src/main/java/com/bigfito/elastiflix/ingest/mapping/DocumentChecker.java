package com.bigfito.elastiflix.ingest.mapping;

import java.util.List;

import com.bigfito.elastiflix.core.error.ErrorCode;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Revisa un documento y arma el objeto que después se convierte en película.
 *
 * <p>El objeto nuevo solo recibe lo que pasa la revisión: nunca el metadato {@code _id}, los vectores
 * ni los textos en blanco. Los lectores JSON y CSV y el registro individual ya quitan los vectores
 * antes de validar.</p>
 */
final class DocumentChecker {

    private static final String NOT_IN_MAPPING = IngestPhrase.NOT_IN_MAPPING.text();

    CheckedDocument check(JsonNode node, String record) {
        if (!(node instanceof ObjectNode object)) {
            return notObject(record);
        }
        return checkObject(object, record);
    }

    private CheckedDocument checkObject(ObjectNode object, String record) {
        String id = idOf(object);
        CheckScope scope = new CheckScope(MovieNodes.MAPPER.createObjectNode(), record, id);
        object.forEachEntry((name, value) -> {
            if (!FieldNames.DOCUMENT_ID.equals(name)) {
                copy(name, value, scope);
            }
        });
        requireFields(scope);
        matchDocumentId(object, id, scope);
        return new CheckedDocument(id, scope.target(), scope.issues());
    }

    private static CheckedDocument notObject(String record) {
        ValidationIssue issue = new ValidationIssue(
                ErrorCode.TYPE_MISMATCH.code(),
                record,
                FieldNames.MISSING,
                IngestPhrase.DOCUMENT.text(),
                IngestPhrase.NOT_OBJECT.text(),
                IngestPhrase.MOVIE_OBJECT.text(),
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

    private void copy(String name, JsonNode value, CheckScope scope) {
        if (!FieldRules.known(name)) {
            scope.error(ErrorCode.UNKNOWN_FIELD, name, IngestPhrase.FIELD_NAMED.text(name), NOT_IN_MAPPING);
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
        scope.error(ErrorCode.REQUIRED_FIELD, name, FieldNames.ABSENT, IngestPhrase.REQUIRED_FIELDS.text());
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
            scope.error(ErrorCode.TYPE_MISMATCH, FieldNames.DOCUMENT_ID,
                    IngestPhrase.NOT_TEXT.text(), IngestPhrase.SAME_AS_ID.text());
            return;
        }
        if (!metaId.equals(id)) {
            scope.error(ErrorCode.ID_MISMATCH, FieldNames.DOCUMENT_ID, metaId, id);
        }
    }
}
