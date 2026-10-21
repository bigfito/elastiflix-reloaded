package com.bigfito.elastiflix.ingest.job;

import com.bigfito.elastiflix.ingest.mapping.ValidationReport;

/**
 * Lo que un trabajo guarda de su informe: un recorte para el CSV descargable y las cifras del informe entero.
 *
 * <p>El recorte trae como mucho {@value JobRetention#MAX_KEPT_ERRORS} errores y ninguna advertencia suelta,
 * para que la memoria no crezca con cada archivo. Las cifras (total de errores, registros con error y
 * advertencias agrupadas por código y campo) se calculan antes de recortar, así que la página sigue mostrando
 * las del archivo entero.</p>
 *
 * @param report  los primeros errores, en el orden del archivo
 * @param figures cifras del informe entero
 */
record KeptReport(ValidationReport report, ReportFigures figures) {

    /** Lo que guarda un trabajo que todavía no se validó. */
    static final KeptReport EMPTY = of(ValidationReport.empty());

    /**
     * Calcula las cifras del informe entero y guarda solo sus primeros errores.
     *
     * @param full informe completo de la validación
     * @return recorte y cifras
     */
    static KeptReport of(ValidationReport full) {
        return new KeptReport(full.firstErrors(JobRetention.MAX_KEPT_ERRORS), ReportFigures.of(full));
    }
}
