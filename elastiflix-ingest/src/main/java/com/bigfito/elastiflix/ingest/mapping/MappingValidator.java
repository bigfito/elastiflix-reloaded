package com.bigfito.elastiflix.ingest.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.ingest.movie.MovieDocuments;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

/**
 * Revisa un archivo completo contra el mapping y solo deja pasar uno sin errores.
 */
@Component
public final class MappingValidator {

    private final DocumentChecker checker = new DocumentChecker();

    /**
     * Valida los documentos en orden. Si hay un error, no devuelve películas.
     *
     * @param documents   objetos JSON, uno por película; la lista no cambia
     * @param existingIds ids que ya están en el catálogo; vacío si no se sabe
     * @return películas aceptadas, el informe y cuántos id se reemplazarían
     */
    public ValidatedMovies validate(List<JsonNode> documents, Set<String> existingIds) {
        return validateAndRelease(new ArrayList<>(documents), existingIds);
    }

    /**
     * Valida igual que {@link #validate} y suelta cada nodo de la lista apenas lo revisa, para que el
     * archivo crudo y las películas no convivan enteros en memoria.
     *
     * @param documents   lista modificable; al terminar solo tiene {@code null}
     * @param existingIds ids que ya están en el catálogo; vacío si no se sabe
     * @return películas aceptadas, el informe y cuántos id se reemplazarían
     */
    public ValidatedMovies validateAndRelease(List<JsonNode> documents, Set<String> existingIds) {
        ValidationRun run = new ValidationRun();
        ListIterator<JsonNode> pending = documents.listIterator();
        while (pending.hasNext()) {
            JsonNode document = pending.next();
            pending.set(null);
            accept(document, pending.nextIndex(), run);
        }
        noteEmpty(documents, run);
        return result(run, existingIds);
    }

    private void accept(JsonNode document, int number, ValidationRun run) {
        CheckedDocument checked = checker.check(document, number, run.warned());
        run.addAll(checked.issues());
        remember(checked, number, run);
        if (!run.failed()) {
            run.movies().add(MovieDocuments.fromNode(checked.body()));
        }
    }

    private static void remember(CheckedDocument checked, int number, ValidationRun run) {
        if (FieldNames.MISSING.equals(checked.id())) {
            return;
        }
        if (!run.seen().add(checked.id())) {
            run.add(duplicate(checked, number));
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
        run.add(new ValidationIssue(
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
        return new ValidatedMovies(run.movies(), report, replace);
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
}
