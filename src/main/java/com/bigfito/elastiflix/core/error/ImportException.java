package com.bigfito.elastiflix.core.error;

import java.io.Serial;

/**
 * Importación que no pudo publicarse en el catálogo.
 */
public class ImportException extends ElastiflixException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Crea la falla de importación sin causa técnica.
     *
     * @param errorCode código {@code EFX-IMP-*}
     */
    public ImportException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Crea la falla de importación conservando la causa técnica.
     *
     * @param errorCode código {@code EFX-IMP-*}
     * @param cause     excepción original
     */
    public ImportException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
