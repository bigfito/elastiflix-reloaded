package com.bigfito.elastiflix.ingest.mapping;

import java.util.ArrayList;
import java.util.List;

/**
 * Errores y advertencias de un archivo. Si hay un error, el archivo no se importa.
 *
 * @param issues en el orden en que se encontraron
 */
public record ValidationReport(List<ValidationIssue> issues) {

    /**
     * Copia la lista para que nadie la cambie después.
     */
    public ValidationReport {
        issues = List.copyOf(issues);
    }

    /**
     * Informe vacío: el archivo puede importarse.
     *
     * @return informe sin incidencias
     */
    public static ValidationReport empty() {
        return new ValidationReport(List.of());
    }

    /**
     * Dice si no hay errores. Las advertencias no cuentan.
     *
     * @return {@code true} cuando se puede importar
     */
    public boolean accepted() {
        return errors().isEmpty();
    }

    /**
     * Incidencias que impiden la importación.
     *
     * @return solo los errores
     */
    public List<ValidationIssue> errors() {
        return filter(false);
    }

    /**
     * Incidencias que se informan y no detienen.
     *
     * @return solo las advertencias
     */
    public List<ValidationIssue> warnings() {
        return filter(true);
    }

    private List<ValidationIssue> filter(boolean warning) {
        List<ValidationIssue> selected = new ArrayList<>();
        for (ValidationIssue issue : issues) {
            if (issue.warning() == warning) {
                selected.add(issue);
            }
        }
        return List.copyOf(selected);
    }
}
