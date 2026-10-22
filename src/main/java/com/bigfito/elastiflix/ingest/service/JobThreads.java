package com.bigfito.elastiflix.ingest.service;

import com.bigfito.elastiflix.core.error.ErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hilos virtuales de la carga, con nombre propio y con su falla registrada en SLF4J.
 *
 * <p>Sin el manejador, una excepción que no es {@code EFX} sale del hilo y la imprime la JVM en stderr,
 * fuera del log de la aplicación. El trabajo ya queda {@code fallido} con {@code EFX-IMP-007}; aquí solo se
 * registra el tipo, nunca el mensaje, que podría traer contenido del archivo.</p>
 */
final class JobThreads {

    private static final Logger LOG = LoggerFactory.getLogger(JobThreads.class);

    private JobThreads() {
    }

    /**
     * Arranca la tarea en un hilo virtual.
     *
     * @param name nombre del hilo, con el id del trabajo
     * @param task validación o importación
     * @return el hilo ya arrancado
     */
    static Thread start(String name, Runnable task) {
        return Thread.ofVirtual().name(name).uncaughtExceptionHandler(JobThreads::logUncaught).start(task);
    }

    /**
     * Registra la excepción que terminó el hilo.
     *
     * @param thread    hilo que terminó
     * @param exception lo que lanzó
     */
    static void logUncaught(Thread thread, Throwable exception) {
        LOG.error("{} hilo={} tipo={}", ErrorCode.JOB_FAILED.code(), thread.getName(),
                exception.getClass().getSimpleName());
    }
}
