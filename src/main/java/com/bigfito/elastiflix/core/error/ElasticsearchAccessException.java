package com.bigfito.elastiflix.core.error;

import java.io.Serial;

/**
 * Falla al leer o modificar el catálogo en Elasticsearch.
 *
 * <p>Lleva un código {@code EFX-ES-*}, o {@code EFX-IMP-006} cuando no se sabe si la publicación del alias
 * se aplicó.</p>
 */
public class ElasticsearchAccessException extends ElastiflixException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Crea la falla de catálogo sin causa técnica.
     *
     * @param errorCode código {@code EFX-ES-*}, o {@code EFX-IMP-006} si la publicación quedó incierta
     */
    public ElasticsearchAccessException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Crea la falla de catálogo conservando la causa técnica.
     *
     * @param errorCode código {@code EFX-ES-*}, o {@code EFX-IMP-006} si la publicación quedó incierta
     * @param cause     excepción original del cliente
     */
    public ElasticsearchAccessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
