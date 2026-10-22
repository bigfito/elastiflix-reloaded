package com.bigfito.elastiflix.ingest.job;

import java.util.function.BooleanSupplier;

/**
 * Datos de una importación que ya pasó la validación.
 *
 * @param alias     alias del catálogo
 * @param previous  índice concreto que está detrás del alias, o {@code null} en la primera carga
 * @param temp      índice temporal
 * @param source    lotes de películas
 * @param counts    conteos del archivo
 * @param listener  quien recibe el progreso
 * @param cancelled {@code true} cuando alguien pidió cancelar
 */
record ImportContext(
        String alias,
        String previous,
        String temp,
        MovieBatchSource source,
        ImportCounts counts,
        ImportListener listener,
        BooleanSupplier cancelled) {
}
