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
     * El aviso que ve la página: lotes, vectorizados y escritos.
     *
     * @param step      paso del mockup
     * @param tempIndex índice temporal donde se escribe
     * @return avance de este lote
     */
    ImportProgress notice(int step, String tempIndex) {
        return new ImportProgress(step, batches, vectorized, written, tempIndex);
    }

    /**
     * Los mismos conteos con otra cifra de escritos: el aviso de antes de escribir el lote.
     *
     * @param count escritos hasta ahora
     * @return copia con esa cifra
     */
    WriteTotals withWritten(int count) {
        return new WriteTotals(batches, vectorized, count, replaced);
    }
}
