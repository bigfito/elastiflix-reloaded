package com.bigfito.elastiflix.ingest.mapping;

/**
 * Un error o una advertencia de una fila, un campo o el archivo entero.
 *
 * @param code        código {@code EFX-ING-*}
 * @param recordLabel fila, registro o {@code encabezado}
 * @param id          id de la película, o un guion si todavía no hay
 * @param field       campo o columna
 * @param received    valor que llegó, recortado
 * @param expected    qué se esperaba
 * @param warning     {@code true} si no impide importar
 */
public record ValidationIssue(
        String code,
        String recordLabel,
        String id,
        String field,
        String received,
        String expected,
        boolean warning) {
}
