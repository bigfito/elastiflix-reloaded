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
        return validateAndRelease(new ArrayList<>(documents), List.of(), existingIds);
    }

    /**
     * Valida igual que {@link #validate} y suelta cada nodo de la lista apenas lo revisa, para que el
     * archivo crudo y las películas no convivan enteros en memoria.
     *
     * @param documents   lista modificable; al terminar solo tiene {@code null}
     * @param labels      fila o registro de cada documento, en el mismo orden (en CSV, la fila real del
     *                    archivo); vacía para numerar desde 1
     * @param existingIds ids que ya están en el catálogo; vacío si no se sabe
     * @return películas aceptadas, el informe y cuántos id se reemplazarían
     */
    public ValidatedMovies validateAndRelease(
            List<JsonNode> documents, List<String> labels, Set<String> existingIds) {
        ValidationRun run = new ValidationRun();
        ListIterator<JsonNode> pending = documents.listIterator();
        while (pending.hasNext()) {
            int index = pending.nextIndex();
            JsonNode document = pending.next();
            pending.set(null);
            accept(document, labelOf(labels, index), run);
        }
        noteEmpty(documents, run);
        return result(run, existingIds);
    }

    private static String labelOf(List<String> labels, int index) {
        return index < labels.size() ? labels.get(index) : Integer.toString(index + 1);
    }

    private void accept(JsonNode document, String label, ValidationRun run) {
        CheckedDocument checked = checker.check(document, label, run.warned());
        run.addAll(checked.issues());
        remember(checked, label, run);
        if (!run.failed()) {
            run.movies().add(MovieDocuments.fromNode(checked.body()));
        }
    }

    private static void remember(CheckedDocument checked, String label, ValidationRun run) {
        if (FieldNames.MISSING.equals(checked.id())) {
            return;
        }
        if (!run.seen().add(checked.id())) {
            run.add(duplicate(checked, label));
            return;
        }
        run.ids().add(checked.id());
    }

    private static ValidationIssue duplicate(CheckedDocument checked, String label) {
        return new ValidationIssue(
                ErrorCode.DUPLICATE_ID.code(),
                label,
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
