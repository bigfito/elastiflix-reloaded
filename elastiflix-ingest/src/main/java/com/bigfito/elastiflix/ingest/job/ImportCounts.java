package com.bigfito.elastiflix.ingest.job;

/**
 * Cuántos registros trae el archivo y en lotes de cuántos se escriben.
 *
 * <p>Los reemplazos no van aquí: {@link BatchWriter} los cuenta sobre la copia, lote por lote.</p>
 *
 * @param fileCount registros del archivo
 * @param batchSize películas por lote de escritura
 */
public record ImportCounts(int fileCount, int batchSize) {
}
