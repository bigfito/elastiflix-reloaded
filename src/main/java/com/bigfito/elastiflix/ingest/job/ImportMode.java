package com.bigfito.elastiflix.ingest.job;

import java.util.Locale;

/**
 * Modo de una carga por archivo.
 *
 * <p>{@link #APPEND} agrega las películas al índice vigente por el alias y es el modo por defecto, porque no borra
 * nada. {@link #BULK} escribe solo el archivo en un índice temporal y publica: el catálogo queda con esas
 * películas.</p>
 */
public enum ImportMode {

    /** Agrega al índice vigente, sin índice temporal. */
    APPEND("append"),

    /** Reemplaza el catálogo con las películas del archivo. */
    BULK("bulk");

    private final String label;

    ImportMode(String label) {
        this.label = label;
    }

    /**
     * Nombre en el contrato web y en el estado del trabajo.
     *
     * @return {@code append} o {@code bulk}
     */
    public String label() {
        return label;
    }

    /**
     * Lee el parámetro {@code modo}. Solo {@code bulk} (sin importar mayúsculas) elige bulk; cualquier otro valor,
     * también vacío o ausente, es append, el modo que no borra nada.
     *
     * @param value valor recibido; puede ser {@code null}
     * @return el modo
     */
    public static ImportMode from(String value) {
        if (value != null && BULK.label.equals(value.strip().toLowerCase(Locale.ROOT))) {
            return BULK;
        }
        return APPEND;
    }
}
