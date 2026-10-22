package com.bigfito.elastiflix.ingest.csv;

/**
 * Una fila de la tabla «Correspondencia de columnas».
 *
 * @param column  nombre en el encabezado del CSV
 * @param field   campo del índice al que se asigna
 * @param content qué trae la celda
 * @param status  {@code Asignada}, {@code Se ignora}, {@code Sin datos} o {@code No existe en el mapping}
 */
public record CsvColumnRow(String column, String field, String content, String status) {
}
