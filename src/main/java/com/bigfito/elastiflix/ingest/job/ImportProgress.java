package com.bigfito.elastiflix.ingest.job;

/**
 * Avance que ve quien espera la importación.
 *
 * <p>Al crear el temporal llega un aviso con todo en 0. Después, {@code BatchWriter} manda dos avisos por lote:
 * antes de escribirlo, con el lote ya vectorizado, y después, cuando Elasticsearch lo aceptó. Al publicar
 * llega el aviso del paso 5 con los totales.</p>
 *
 * @param step        paso del mockup, del 1 al 5
 * @param batch       número del lote en curso, desde 1: el que se acaba de vectorizar o de escribir; 0 antes del
 *                    primero. En el aviso del paso 5, el total de lotes
 * @param vectorized  registros que ya tienen vector, acumulados y contando el lote en curso
 * @param written     registros que Elasticsearch ya aceptó, acumulados; en el aviso de antes de escribir un lote,
 *                    todavía sin ese lote
 * @param tempIndex   índice temporal {@code <alias>-<AAAAMMDD-HHMMSS>} donde se escribe
 */
public record ImportProgress(int step, int batch, int vectorized, int written, String tempIndex) {
}
