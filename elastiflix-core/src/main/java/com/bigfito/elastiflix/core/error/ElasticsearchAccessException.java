package com.bigfito.elastiflix.core.error;

import java.io.Serial;

/**
 * Falla al leer o modificar el catálogo en Elasticsearch.
 */
public class ElasticsearchAccessException extends ElastiflixException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Crea la falla de catálogo sin causa técnica.
     *
     * @param errorCode código {@code EFX-ES-*}
     */
    public ElasticsearchAccessException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Crea la falla de catálogo conservando la causa técnica.
     *
     * @param errorCode código {@code EFX-ES-*}
     * @param cause     excepción original del cliente
     */
    public ElasticsearchAccessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
