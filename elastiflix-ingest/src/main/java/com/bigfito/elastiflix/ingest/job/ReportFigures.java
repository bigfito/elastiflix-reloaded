package com.bigfito.elastiflix.ingest.job;

import java.util.List;

import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;
import com.bigfito.elastiflix.ingest.mapping.WarningCount;

/**
 * Cifras del informe que muestra la página. Se calculan una vez, cuando el informe cambia, y no en cada
 * consulta de estado: la página pregunta cada segundo y el informe de un archivo grande tiene miles de
 * incidencias.
 *
 * @param preview        como mucho los primeros 6 errores
 * @param errorTotal     errores del informe
 * @param invalidRecords registros distintos con al menos un error
 * @param warnings       advertencias agrupadas por código y campo
 */
record ReportFigures(List<ValidationIssue> preview, int errorTotal, int invalidRecords, List<WarningCount> warnings) {

    /**
     * Copia las listas para que nadie las cambie después.
     */
    ReportFigures {
        preview = List.copyOf(preview);
        warnings = List.copyOf(warnings);
    }

    /**
     * Calcula las cifras de un informe.
     *
     * @param report errores y advertencias del archivo
     * @return cifras listas para la página
     */
    static ReportFigures of(ValidationReport report) {
        List<ValidationIssue> errors = report.errors();
        return new ReportFigures(JobFigures.preview(errors), errors.size(), JobFigures.invalidRecords(errors),
                report.groupedWarnings());
    }
}
