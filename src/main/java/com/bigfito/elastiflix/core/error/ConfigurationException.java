package com.bigfito.elastiflix.core.error;

import java.io.Serial;

/**
 * Falla de arranque por configuración incompleta o incoherente.
 */
public class ConfigurationException extends ElastiflixException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Crea la falla de configuración sin causa técnica.
     *
     * @param errorCode código {@code EFX-CFG-*}
     */
    public ConfigurationException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Crea la falla de configuración conservando la causa técnica.
     *
     * @param errorCode código {@code EFX-CFG-*}
     * @param cause     excepción original
     */
    public ConfigurationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
