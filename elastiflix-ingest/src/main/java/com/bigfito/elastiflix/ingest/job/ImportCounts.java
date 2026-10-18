package com.bigfito.elastiflix.ingest.job;

/**
 * Cuántos registros trae el archivo y cuántos de esos id ya estaban.
 *
 * @param fileCount    registros del archivo
 * @param replaceCount ids que reemplazan un documento ya copiado
 * @param batchSize    películas por lote de escritura
 */
public record ImportCounts(int fileCount, int replaceCount, int batchSize) {
}
