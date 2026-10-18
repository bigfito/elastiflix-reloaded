package com.bigfito.elastiflix.web.ingest;

import java.util.Map;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Respuestas JSON de la carga cuando el archivo no entra o el trabajo no se puede usar.
 */
@RestControllerAdvice
public class IngestJobErrors {

    private static final Logger LOG = LoggerFactory.getLogger(IngestJobErrors.class);

    /**
     * El contenedor, o el controlador, rechazó el archivo por tamaño.
     *
     * @param exception límite superado
     * @return 413 con {@code EFX-ING-003}
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tooLarge(MaxUploadSizeExceededException exception) {
        LOG.warn("{}: la subida supera {} bytes", ErrorCode.FILE_TOO_LARGE.code(), exception.getMaxUploadSize());
        return body(HttpStatus.CONTENT_TOO_LARGE, ErrorCode.FILE_TOO_LARGE);
    }

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

    private static ResponseEntity<Map<String, String>> body(HttpStatus status, ErrorCode code) {
        return ResponseEntity.status(status).body(Map.of("code", code.code(), "message", code.summary()));
    }
}
