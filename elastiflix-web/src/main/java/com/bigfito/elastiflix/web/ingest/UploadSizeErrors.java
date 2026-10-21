package com.bigfito.elastiflix.web.ingest;

import java.util.Locale;
import java.util.Map;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.web.locale.UiMessages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Un archivo mayor que el tope responde 413 JSON con {@code EFX-ING-003}.
 *
 * <p>No tiene alcance: el contenedor rechaza la subida al leer el multipart, antes de elegir controlador,
 * así que un {@code assignableTypes} no la vería.</p>
 *
 * <p>Va primero: si el tope lo detecta {@code IngestJobController}, el manejador de {@code RuntimeException}
 * de {@link IngestJobErrors} no debe convertir el 413 en un 500.</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class UploadSizeErrors {

    private static final Logger LOG = LoggerFactory.getLogger(UploadSizeErrors.class);

    private final UiMessages messages;

    /**
     * Crea el manejador.
     *
     * @param messages catálogo de mensajes, para responder en el idioma de la persona
     */
    public UploadSizeErrors(UiMessages messages) {
        this.messages = messages;
    }

    /**
     * El contenedor, o el controlador, rechazó el archivo por tamaño.
     *
     * @param exception límite superado
     * @param locale    idioma de la petición
     * @return 413 con {@code EFX-ING-003} y su «qué pasó» en ese idioma
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tooLarge(MaxUploadSizeExceededException exception, Locale locale) {
        LOG.warn("{}: la subida supera {} bytes", ErrorCode.FILE_TOO_LARGE.code(), exception.getMaxUploadSize());
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(Map.of(
                "code", ErrorCode.FILE_TOO_LARGE.code(),
                "message", messages.errorSummary(ErrorCode.FILE_TOO_LARGE, locale)));
    }
}
