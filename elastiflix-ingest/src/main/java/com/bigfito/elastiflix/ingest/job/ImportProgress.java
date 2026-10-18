package com.bigfito.elastiflix.ingest.job;

/**
 * Avance que ve quien espera la importación.
 *
 * @param step        paso del mockup, del 1 al 5
 * @param batch       lote que acaba de escribirse; 0 si todavía no hay
 * @param vectorized  registros que ya tienen vector
 * @param written     registros que van a escribirse en este aviso
 * @param replaced    ids del archivo que ya estaban en la copia
 * @param tempIndex   índice temporal {@code <alias>-<AAAAMMDD-HHMMSS>} donde se escribe
 */
public record ImportProgress(int step, int batch, int vectorized, int written, int replaced, String tempIndex) {
}
