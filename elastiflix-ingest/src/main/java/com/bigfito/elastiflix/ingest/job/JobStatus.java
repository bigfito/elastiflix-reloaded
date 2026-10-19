package com.bigfito.elastiflix.ingest.job;

/**
 * Estado de un trabajo de carga, con el nombre que verá la página.
 */
public enum JobStatus {

    /** El archivo ya está en disco y todavía no se revisa. */
    RECEIVED("recibido"),

    /** Se están comprobando el archivo y los campos. */
    VALIDATING("validando"),

    /** No hay errores. Se puede importar. */
    VALID("valido"),

    /** Hay errores de campos o de formato. No se importa. */
    INVALID("invalido"),

    /** Los lotes se están escribiendo en el índice temporal. */
    IMPORTING("importando"),

    /** El alias ya apunta al índice nuevo. */
    PUBLISHED("publicado"),

    /** La importación o la validación falló con una excepción. */
    FAILED("fallido"),

    /**
     * Alguien pidió cancelar. El índice temporal, si existía, se borra; si no se pudo borrar, queda un
     * {@code WARN} con su nombre y hay que borrarlo a mano.
     */
    CANCELLED("cancelado");

    private final String label;

    JobStatus(String label) {
        this.label = label;
    }

    /**
     * Nombre en español, sin tilde, como lo deja el diseño.
     *
     * @return por ejemplo {@code valido} o {@code publicado}
     */
    public String label() {
        return label;
    }

    /**
     * Dice si el trabajo ya no va a cambiar: inválido, publicado, fallido o cancelado.
     *
     * @return {@code true} en un estado final
     */
    public boolean terminal() {
        return this == INVALID || this == PUBLISHED || this == FAILED || this == CANCELLED;
    }
}
