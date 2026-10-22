package com.bigfito.elastiflix.core.catalog;

/**
 * Estado del catálogo: si responde, cuántos documentos tiene y, si no, el código de error.
 *
 * @param name          alias configurado
 * @param concreteIndex índice vigente, o {@code null} si no se pudo resolver
 * @param documentCount documentos, o {@code null} si no está conectado
 * @param connected     si se pudo leer el catálogo
 * @param errorCode     código {@code EFX-*}, o {@code null} si está bien
 */
public record CatalogStatus(
        String name,
        String concreteIndex,
        Long documentCount,
        boolean connected,
        String errorCode) {
}
