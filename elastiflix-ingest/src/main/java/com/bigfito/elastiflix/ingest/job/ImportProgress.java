package com.bigfito.elastiflix.ingest.job;

/**
 * Avance que ve quien espera la importación.
 *
 * @param step        paso del mockup, del 1 al 5
 * @param batch       lote que acaba de escribirse; 0 si todavía no hay
 * @param vectorized  registros que ya tienen vector
 * @param written     registros ya escritos en el índice temporal
 */
public record ImportProgress(int step, int batch, int vectorized, int written) {
}
