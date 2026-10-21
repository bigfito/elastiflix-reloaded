package com.bigfito.elastiflix.ingest.job;

/**
 * Conteos acumulados de la escritura por lotes.
 *
 * <p>Se calculan apenas se vectoriza un lote, antes de escribirlo: cuentan el lote en curso como si ya
 * estuviera escrito. Por eso el aviso de antes de escribir usa {@link #withWritten} con la cifra anterior.</p>
 *
 * @param batches    lotes procesados, contando el que está en curso
 * @param vectorized registros vectorizados, contando el lote en curso
 * @param written    registros escritos cuando Elasticsearch acepte el lote en curso
 * @param replaced   ids del archivo que ya estaban en el índice donde se escribe
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
