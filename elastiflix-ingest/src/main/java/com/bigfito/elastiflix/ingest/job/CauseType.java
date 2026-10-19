package com.bigfito.elastiflix.ingest.job;

import com.bigfito.elastiflix.core.error.ElastiflixException;

/**
 * Nombre corto de la causa técnica de una falla, para el log. Nunca su mensaje, que podría traer datos.
 */
final class CauseType {

    private CauseType() {
    }

    /**
     * Tipo de la causa, o el de la excepción si no tiene causa.
     *
     * @param exception falla con su código
     * @return por ejemplo {@code IOException}
     */
    static String of(ElastiflixException exception) {
        Throwable cause = exception.getCause();
        if (cause == null) {
            return exception.getClass().getSimpleName();
        }
        return cause.getClass().getSimpleName();
    }
}
