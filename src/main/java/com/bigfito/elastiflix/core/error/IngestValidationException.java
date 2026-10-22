package com.bigfito.elastiflix.core.error;

import java.io.Serial;

/**
 * Archivo de carga que no cumple las reglas de la demo.
 *
 * <p>La Fase 3 lanza esta excepción. Existe desde ahora para que la jerarquía nazca junta.</p>
 */
public class IngestValidationException extends ElastiflixException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Crea la falla de validación sin causa técnica.
     *
     * @param errorCode código {@code EFX-ING-*}
     */
    public IngestValidationException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Crea la falla de validación conservando la causa técnica.
     *
     * @param errorCode código {@code EFX-ING-*}
     * @param cause     excepción original
     */
    public IngestValidationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
