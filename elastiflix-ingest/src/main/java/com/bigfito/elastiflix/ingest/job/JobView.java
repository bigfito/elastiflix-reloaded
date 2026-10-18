package com.bigfito.elastiflix.ingest.job;

import java.util.List;

import com.bigfito.elastiflix.ingest.csv.CsvColumnRow;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;
import com.bigfito.elastiflix.ingest.mapping.WarningCount;

/**
 * Copia del trabajo que puede leer la página. No cambia si el trabajo sigue.
 *
 * @param id             identificador del trabajo
 * @param status         uno de los ocho estados, en español
 * @param kind           {@code json} o {@code csv}, según la extensión del archivo
 * @param fileSize       bytes del archivo subido
 * @param step           paso del mockup, del 1 al 5; 0 si todavía no hay
 * @param batch          último lote escrito
 * @param vectorized     registros que ya tienen vector
 * @param written        registros ya escritos
 * @param fileCount      registros (JSON) o filas (CSV) leídos, también si el archivo es inválido
 * @param validCount     {@code fileCount} menos los registros con al menos un error
 * @param invalidRecords registros distintos con al menos un error
 * @param replaceCount   ids del archivo que ya están en el catálogo; no cambia durante la importación
 * @param errorTotal     errores del informe, no solo los seis que se muestran
 * @param errors         como mucho los primeros 6 errores
 * @param warnings       advertencias agrupadas por código y campo
 * @param checks         comprobaciones superadas; vacío si la carga no está lista
 * @param columns        tabla de correspondencia del CSV; vacía en JSON
 * @param failureCode    código de la excepción, o vacío si no falló
 * @param failureMessage qué pasó, según ese código, o vacío
 * @param batchSize      películas por lote, según {@code elastiflix.ingest.batch-size}
 * @param batches        lotes que hacen falta para escribir el archivo
 * @param tempIndex      índice temporal de la importación; vacío hasta que se crea
 */
public record JobView(
        String id,
        String status,
        String kind,
        long fileSize,
        int step,
        int batch,
        int vectorized,
        int written,
        int fileCount,
        int validCount,
        int invalidRecords,
        int replaceCount,
        int errorTotal,
        List<ValidationIssue> errors,
        List<WarningCount> warnings,
        List<String> checks,
        List<CsvColumnRow> columns,
        String failureCode,
        String failureMessage,
        int batchSize,
        int batches,
        String tempIndex) {
}
