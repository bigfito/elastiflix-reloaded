package com.bigfito.elastiflix.ingest.job;

import java.util.List;

import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;

/**
 * Copia del trabajo que puede leer la página. No cambia si el trabajo sigue.
 *
 * @param id           identificador del trabajo
 * @param status       uno de los ocho estados, en español
 * @param step         paso del mockup, del 1 al 5; 0 si todavía no hay
 * @param batch        último lote escrito
 * @param vectorized   registros que ya tienen vector
 * @param written      registros ya escritos
 * @param fileCount    registros del archivo
 * @param replaceCount ids que reemplazan un documento ya copiado
 * @param errors       como mucho los primeros 6 errores
 * @param warnings     todas las advertencias
 * @param failureCode  código de la excepción, o vacío si no falló
 */
public record JobView(
        String id,
        String status,
        int step,
        int batch,
        int vectorized,
        int written,
        int fileCount,
        int replaceCount,
        List<ValidationIssue> errors,
        List<ValidationIssue> warnings,
        String failureCode) {
}
