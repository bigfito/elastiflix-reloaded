package com.bigfito.elastiflix.core.error;

import java.io.Serial;

/**
 * Excepción base de Elastiflix. Siempre lleva un {@link ErrorCode}.
 */
public class ElastiflixException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    /**
     * Crea la excepción con su código y la causa técnica original.
     *
     * @param errorCode código del catálogo
     * @param cause     excepción original; nunca se descarta
     */
    public ElastiflixException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.userMessage(), cause);
        this.errorCode = errorCode;
    }

    /**
     * Crea la excepción con su código, sin causa técnica.
     *
     * @param errorCode código del catálogo
     */
    public ElastiflixException(ErrorCode errorCode) {
        super(errorCode.userMessage());
        this.errorCode = errorCode;
    }

    /**
     * Devuelve el código de error asociado.
     *
     * @return el código de error
     */
    public ErrorCode errorCode() {
        return errorCode;
    }
}
