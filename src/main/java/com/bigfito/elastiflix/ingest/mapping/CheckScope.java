package com.bigfito.elastiflix.ingest.mapping;

import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.error.ErrorCode;

import tools.jackson.databind.node.ObjectNode;

/**
 * Errores y objeto limpio de un solo registro.
 */
final class CheckScope {

    private final ObjectNode target;
    private final List<ValidationIssue> issues = new ArrayList<>();
    private final String record;
    private final String id;

    CheckScope(ObjectNode target, String record, String id) {
        this.target = target;
        this.record = record;
        this.id = id;
    }

    ObjectNode target() {
        return target;
    }

    List<ValidationIssue> issues() {
        return issues;
    }

    void error(ErrorCode code, String field, String received, String expected) {
        issues.add(issue(code, field, received, expected, false));
    }

    void warning(ErrorCode code, String field, String received, String expected) {
        issues.add(issue(code, field, received, expected, true));
    }

    private ValidationIssue issue(
            ErrorCode code, String field, String received, String expected, boolean warning) {
        return new ValidationIssue(code.code(), record, id, field, received, expected, warning);
    }
}
