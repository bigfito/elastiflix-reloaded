package com.bigfito.elastiflix.ingest.job;

import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.mapping.ValidationReport;

/**
 * El registro se guardó, o el informe dice por qué no.
 */
public sealed interface SaveResult permits SaveResult.Saved, SaveResult.Rejected {

    /**
     * La película ya está en el catálogo, con sus vectores.
     *
     * @param movie película guardada
     */
    record Saved(Movie movie) implements SaveResult {
    }

    /**
     * Hay errores de campos. No se escribió nada.
     *
     * @param report errores del registro
     */
    record Rejected(ValidationReport report) implements SaveResult {
    }
}
