package com.bigfito.elastiflix.ingest.mapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tools.jackson.databind.node.ObjectNode;

/**
 * Acumulador de una pasada de validación.
 */
final class ValidationRun {

    private final List<ValidationIssue> issues = new ArrayList<>();
    private final List<ObjectNode> bodies = new ArrayList<>();
    private final Set<String> seen = new HashSet<>();
    private final Set<String> ids = new HashSet<>();
    private final Set<String> warned = new HashSet<>();

    List<ValidationIssue> issues() {
        return issues;
    }

    List<ObjectNode> bodies() {
        return bodies;
    }

    Set<String> seen() {
        return seen;
    }

    Set<String> ids() {
        return ids;
    }

    Set<String> warned() {
        return warned;
    }
}
