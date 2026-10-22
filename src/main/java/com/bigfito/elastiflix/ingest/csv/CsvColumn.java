package com.bigfito.elastiflix.ingest.csv;

/**
 * Una columna del encabezado y en cuántas filas trae algo.
 *
 * @param name   nombre de la primera fila
 * @param filled filas con la celda no vacía
 */
public record CsvColumn(String name, int filled) {
}
