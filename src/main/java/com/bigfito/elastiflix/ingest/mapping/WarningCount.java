package com.bigfito.elastiflix.ingest.mapping;

/**
 * Advertencias iguales, contadas una sola vez para el estado del trabajo.
 *
 * @param code    código {@code EFX-ING-*}
 * @param field   campo
 * @param count   cuántas veces apareció
 * @param message texto de la primera
 */
public record WarningCount(String code, String field, int count, String message) {
}
