package com.bigfito.elastiflix.ingest.job;

/**
 * Lote ya escrito, para seguir el progreso.
 *
 * @param batches    lotes completados
 * @param vectorized registros vectorizados
 * @param written    registros escritos
 */
record WriteTotals(int batches, int vectorized, int written) {
}
