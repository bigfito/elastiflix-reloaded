package com.bigfito.elastiflix.ingest.job;

/**
 * Los cinco pasos del mockup de «Cargar datos», en un solo lugar.
 */
final class JobSteps {

    /** Paso 1: el archivo llegó al servidor. */
    static final int RECEIVED = 1;

    /** Paso 2: extensión, tamaño y UTF-8 ({@code EFX-ING-001} a {@code 004}). */
    static final int UPLOAD = 2;

    /** Paso 3: lectura y validación contra el mapping. */
    static final int VALIDATION = 3;

    /** Paso 4: copia del catálogo, vectores y lotes en el índice temporal. */
    static final int IMPORT = 4;

    /** Paso 5: el alias pasa al índice nuevo. */
    static final int PUBLISH = 5;

    private JobSteps() {
    }
}
