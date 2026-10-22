package com.bigfito.elastiflix.ingest.mapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bigfito.elastiflix.core.model.Movie;

/**
 * Acumulador de una pasada de validación.
 *
 * <p>Las películas se convierten a medida que se revisan. Con el primer error se sueltan y no se
 * convierte ninguna más: el archivo ya no se va a importar.</p>
 */
final class ValidationRun {

    private final List<ValidationIssue> issues = new ArrayList<>();
    private final List<Movie> movies = new ArrayList<>();
    private final Set<String> seen = new HashSet<>();
    private final Set<String> ids = new HashSet<>();
    private boolean failed;

    void add(ValidationIssue issue) {
        issues.add(issue);
        if (!issue.warning() && !failed) {
            failed = true;
            movies.clear();
        }
    }

    void addAll(List<ValidationIssue> found) {
        for (ValidationIssue issue : found) {
            add(issue);
        }
    }

    boolean failed() {
        return failed;
    }

    List<ValidationIssue> issues() {
        return issues;
    }

    List<Movie> movies() {
        return movies;
    }

    Set<String> seen() {
        return seen;
    }

    Set<String> ids() {
        return ids;
    }
}
