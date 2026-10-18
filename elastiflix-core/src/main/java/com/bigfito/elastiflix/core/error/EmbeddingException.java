package com.bigfito.elastiflix.core.error;

import java.io.Serial;

/**
 * Falla al generar o recortar un vector.
 */
public class EmbeddingException extends ElastiflixException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Crea la falla de embeddings sin causa técnica.
     *
     * @param errorCode código {@code EFX-EMB-*}
     */
    public EmbeddingException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Crea la falla de embeddings conservando la causa técnica.
     *
     * @param errorCode código {@code EFX-EMB-*}
     * @param cause     excepción original
     */
    public EmbeddingException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
