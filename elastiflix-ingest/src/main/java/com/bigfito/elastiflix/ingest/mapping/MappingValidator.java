package com.bigfito.elastiflix.ingest.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.movie.MovieDocuments;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Revisa un archivo completo contra el mapping y solo deja pasar uno sin errores.
 */
@Component
public final class MappingValidator {

    private final DocumentChecker checker = new DocumentChecker();

    /**
     * Valida los documentos en orden. Si hay un error, no devuelve películas.
     *
     * @param documents   objetos JSON, uno por película
     * @param existingIds ids que ya están en el catálogo; vacío si no se sabe
     * @return películas aceptadas, el informe y cuántos id se reemplazarían
     */
    public ValidatedMovies validate(List<JsonNode> documents, Set<String> existingIds) {
        ValidationRun run = new ValidationRun();
        int number = 1;
        for (JsonNode document : documents) {
            accept(document, number, run);
            number++;
        }
        noteEmpty(documents, run);
        return result(run, existingIds);
    }

    private void accept(JsonNode document, int number, ValidationRun run) {
        CheckedDocument checked = checker.check(document, number, run.warned());
        run.issues().addAll(checked.issues());
        dropVectors(document);
        dropVectors(checked.body());
        run.bodies().add(checked.body());
        remember(checked, number, run);
    }

    private static void dropVectors(JsonNode node) {
        if (node instanceof ObjectNode object) {
            object.remove(List.of("plot_256", "plot_512", "plot_1024"));
        }
    }

    private static void remember(CheckedDocument checked, int number, ValidationRun run) {
        if (FieldNames.MISSING.equals(checked.id())) {
            return;
        }
        if (!run.seen().add(checked.id())) {
            run.issues().add(duplicate(checked, number));
            return;
        }
        run.ids().add(checked.id());
    }

    private static ValidationIssue duplicate(CheckedDocument checked, int number) {
        return new ValidationIssue(
                ErrorCode.DUPLICATE_ID.code(),
                Integer.toString(number),
                checked.id(),
                FieldNames.ID,
                checked.id(),
                "un id único",
                false);
    }

    private static void noteEmpty(List<JsonNode> documents, ValidationRun run) {
        if (!documents.isEmpty()) {
            return;
        }
        run.issues().add(new ValidationIssue(
                ErrorCode.REQUIRED_FIELD.code(),
                FieldNames.FILE,
                FieldNames.MISSING,
                FieldNames.ID,
                FieldNames.ABSENT,
                "id, title y plot",
                false));
    }

    private static ValidatedMovies result(ValidationRun run, Set<String> existingIds) {
        ValidationReport report = new ValidationReport(run.issues());
        int replace = replacements(run.ids(), existingIds);
        if (!report.accepted()) {
            return new ValidatedMovies(List.of(), report, replace);
        }
        return new ValidatedMovies(movies(run.bodies()), report, replace);
    }

    private static int replacements(Set<String> ids, Set<String> existingIds) {
        if (existingIds == null || existingIds.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String id : ids) {
            if (existingIds.contains(id)) {
                count++;
            }
        }
        return count;
    }

    private static List<Movie> movies(List<ObjectNode> bodies) {
        List<Movie> movies = new ArrayList<>();
        for (ObjectNode body : bodies) {
            movies.add(MovieDocuments.fromNode(body));
        }
        bodies.clear();
        return movies;
    }
}
