package com.bigfito.elastiflix.ingest.mapping;

import java.util.Set;

import com.bigfito.elastiflix.core.error.ErrorCode;

/**
 * Los tres campos de vector. Los lectores los quitan apenas leen un registro, para que el archivo
 * entero nunca esté en memoria con 1 792 números por película.
 */
public final class VectorFields {

    /** {@code plot_256}, {@code plot_512} y {@code plot_1024}. */
    public static final Set<String> NAMES = Set.of("plot_256", "plot_512", "plot_1024");

    private static final String WILL_GENERATE = IngestPhrase.WILL_BE_GENERATED.text();

    private VectorFields() {
    }

    /**
     * Dice si el campo es uno de los vectores.
     *
     * @param name nombre del campo o de la columna
     * @return {@code true} para {@code plot_256}, {@code plot_512} y {@code plot_1024}
     */
    public static boolean isVector(String name) {
        return NAMES.contains(name);
    }

    /**
     * Advertencia {@code EFX-ING-020} de un archivo JSON que trae ese vector.
     *
     * @param name campo de vector
     * @return advertencia del archivo entero
     */
    public static ValidationIssue ignored(String name) {
        return new ValidationIssue(
                ErrorCode.VECTORS_IGNORED.code(), FieldNames.FILE, FieldNames.MISSING, name, name, WILL_GENERATE, true);
    }
}
