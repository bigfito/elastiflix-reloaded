package com.bigfito.elastiflix.ingest.job;

/**
 * Los pasos del mockup de «Cargar datos», en un solo lugar: cinco en bulk y cuatro en append.
 */
final class JobSteps {

    /** Paso 1: el archivo llegó al servidor. */
    static final int RECEIVED = 1;

    /** Paso 2: extensión, tamaño y UTF-8 ({@code EFX-ING-001} a {@code 004}). */
    static final int UPLOAD = 2;

    /** Paso 3: lectura y validación contra el mapping. */
    static final int VALIDATION = 3;

    /** Paso 4: vectores y lotes en el índice temporal (bulk) o en el índice vigente por el alias (append). */
    static final int IMPORT = 4;

    /** Paso 5, solo en bulk: el alias pasa al índice nuevo. */
    static final int PUBLISH = 5;

    private JobSteps() {
    }
}
