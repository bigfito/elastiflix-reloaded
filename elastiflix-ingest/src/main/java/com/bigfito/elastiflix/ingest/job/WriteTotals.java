package com.bigfito.elastiflix.ingest.job;

/**
 * Lote ya escrito, para seguir el progreso.
 *
 * @param batches    lotes completados
 * @param vectorized registros vectorizados
 * @param written    registros escritos
 * @param replaced   ids del archivo que ya estaban en la copia
 */
record WriteTotals(int batches, int vectorized, int written, int replaced) {

    /**
     * El aviso que ve la página, con los cuatro conteos juntos.
     *
     * @param step paso del mockup
     * @return avance de este lote
     */
    ImportProgress notice(int step, String tempIndex) {
        return new ImportProgress(step, batches, vectorized, written, replaced, tempIndex);
    }
}
