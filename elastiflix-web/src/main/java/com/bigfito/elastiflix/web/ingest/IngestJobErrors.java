package com.bigfito.elastiflix.web.ingest;

import java.io.IOException;
import java.util.Map;

import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;
import com.bigfito.elastiflix.core.error.IngestValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Respuestas JSON de los endpoints de trabajos de carga cuando algo falla.
 *
 * <p>Solo atiende a {@link IngestJobController}: las páginas HTML siguen con {@code WebExceptionHandler}. Así
 * la página que consulta el trabajo siempre recibe JSON, nunca una página de error.</p>
 */
@RestControllerAdvice(assignableTypes = IngestJobController.class)
public class IngestJobErrors {

    private static final Logger LOG = LoggerFactory.getLogger(IngestJobErrors.class);

    /**
     * Un trabajo que no está, o que no admite la operación pedida.
     *
     * @param exception falla de importación
     * @return 404 si el id no existe; 409 en cualquier otro {@code EFX-IMP-*}
     */
    @ExceptionHandler(ImportException.class)
    public ResponseEntity<Map<String, String>> job(ImportException exception) {
        if (exception.errorCode() == ErrorCode.UNKNOWN_JOB) {
            return body(HttpStatus.NOT_FOUND, exception.errorCode());
        }
        return body(HttpStatus.CONFLICT, exception.errorCode());
    }

    /**
     * El archivo no pasó una comprobación del servidor.
     *
     * @param exception falla de validación
     * @return 400 con su {@code EFX-ING-*}
     */
    @ExceptionHandler(IngestValidationException.class)
    public ResponseEntity<Map<String, String>> invalid(IngestValidationException exception) {
        return body(HttpStatus.BAD_REQUEST, exception.errorCode());
    }

    /**
     * Cualquier otra falla con código: Elasticsearch, inferencia o configuración.
     *
     * @param exception falla de la demo
     * @return 500 con su código
     */
    @ExceptionHandler(ElastiflixException.class)
    public ResponseEntity<Map<String, String>> failed(ElastiflixException exception) {
        LOG.error("{} en un endpoint de carga", exception.errorCode().code());
        return body(HttpStatus.INTERNAL_SERVER_ERROR, exception.errorCode());
    }

    /**
     * El servidor no pudo leer o guardar el archivo subido.
     *
     * @param exception falla de entrada y salida
     * @return 500 con {@code EFX-IMP-007}
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> io(IOException exception) {
        LOG.error("{} al leer o guardar la subida tipo={}", ErrorCode.JOB_FAILED.code(),
                exception.getClass().getSimpleName());
        return body(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.JOB_FAILED);
    }

    private static ResponseEntity<Map<String, String>> body(HttpStatus status, ErrorCode code) {
        return ResponseEntity.status(status).body(Map.of("code", code.code(), "message", code.summary()));
    }
}
