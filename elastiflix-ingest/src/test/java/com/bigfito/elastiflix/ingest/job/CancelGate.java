package com.bigfito.elastiflix.ingest.job;

/**
 * La prueba de cancelación deja aquí la acción que corre al volver de {@code reindex}.
 */
final class CancelGate {

    private volatile Runnable action = CancelGate::idle;

    void arm(Runnable action) {
        this.action = action;
    }

    void clear() {
        this.action = CancelGate::idle;
    }

    void fire() {
        action.run();
    }

    private static void idle() {
        // La importación de la otra prueba no se cancela.
    }
}
