package com.bigfito.elastiflix.core.config;

import com.bigfito.elastiflix.core.error.ElastiflixException;

import org.springframework.boot.ExitCodeExceptionMapper;
import org.springframework.stereotype.Component;

/**
 * Convierte una {@link ElastiflixException} en código de salida 1.
 *
 * <p>No llama a {@code System.exit}. Boot cierra el proceso al terminar el runner.</p>
 */
@Component
public class ElastiflixExitCodeMapper implements ExitCodeExceptionMapper {

    private static final int FAILURE = 1;

    /**
     * Devuelve 1 si la falla, o alguna de sus causas, es de Elastiflix.
     *
     * @param exception falla del arranque
     * @return 1 si es de Elastiflix; 0 si no
     */
    @Override
    public int getExitCode(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ElastiflixException) {
                return FAILURE;
            }
            current = current.getCause();
        }
        return 0;
    }
}
