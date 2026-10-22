package com.bigfito.elastiflix.core.error;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;

/**
 * Separa un rechazo 4xx de un fallo de red, sin copiar el texto {@code reason}.
 */
public final class ElasticsearchDiagnostics {

    private static final int CLIENT_ERROR = 400;
    private static final int SERVER_ERROR = 500;
    private static final int UNAUTHORIZED = 401;
    private static final int FORBIDDEN = 403;

    private ElasticsearchDiagnostics() {
    }

    /**
     * Un estado HTTP de cliente, del 400 al 499.
     *
     * @param status código HTTP, o 0 si no hubo respuesta
     * @return {@code true} si Elasticsearch rechazó la petición
     */
    public static boolean clientError(int status) {
        return status >= CLIENT_ERROR && status < SERVER_ERROR;
    }

    /**
     * Dice si la falla es un rechazo seguro: Elasticsearch respondió con un 4xx.
     *
     * <p>Un corte de red, un timeout del cliente o un 5xx no lo son: la operación pudo aplicarse.</p>
     *
     * @param failure excepción del catálogo; se mira su causa
     * @return {@code true} solo si la causa es un {@code ElasticsearchException} 4xx
     */
    public static boolean isClientRejection(Throwable failure) {
        return failure != null && failure.getCause() instanceof ElasticsearchException elastic
                && clientError(elastic.status());
    }

    /**
     * Tipo del error de Elasticsearch, o el nombre de la clase si no hay tipo.
     *
     * @param cause excepción capturada
     * @return texto corto para el log
     */
    public static String typeOf(Throwable cause) {
        if (cause instanceof ElasticsearchException elastic) {
            String type = elastic.error() == null ? null : elastic.error().type();
            if (type != null && !type.isBlank()) {
                return type;
            }
        }
        return cause.getClass().getSimpleName();
    }

    /**
     * Código de la demo según el estado HTTP.
     *
     * @param status código HTTP, o 0 si no hubo respuesta
     * @return {@code EFX-ES-001} en un 401 o 403 (URL o API key no válidas), sin respuesta o en un 5xx;
     *         {@code EFX-ES-007} en el resto de los 4xx
     */
    public static ErrorCode codeFor(int status) {
        if (status == UNAUTHORIZED || status == FORBIDDEN) {
            return ErrorCode.ELASTICSEARCH_UNAVAILABLE;
        }
        if (clientError(status)) {
            return ErrorCode.ELASTICSEARCH_REJECTED;
        }
        return ErrorCode.ELASTICSEARCH_UNAVAILABLE;
    }
}
