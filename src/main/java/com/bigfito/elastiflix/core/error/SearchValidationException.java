package com.bigfito.elastiflix.core.error;

import java.io.Serial;

/**
 * Consulta de búsqueda que no cumple las reglas de la demo.
 */
public class SearchValidationException extends ElastiflixException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Crea la falla de búsqueda sin causa técnica.
     *
     * @param errorCode código {@code EFX-SRCH-*}
     */
    public SearchValidationException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Crea la falla de búsqueda conservando la causa técnica.
     *
     * @param errorCode código {@code EFX-SRCH-*}
     * @param cause     excepción original
     */
    public SearchValidationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
