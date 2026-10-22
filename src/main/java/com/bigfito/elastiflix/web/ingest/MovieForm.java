package com.bigfito.elastiflix.web.ingest;

import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;

/**
 * Resultado de leer el formulario del registro individual.
 */
public sealed interface MovieForm permits MovieForm.Read, MovieForm.Invalid {

    /**
     * Los campos se convirtieron: la película sigue a la validación del mapping.
     *
     * @param movie película sin vectores
     */
    record Read(Movie movie) implements MovieForm {
    }

    /**
     * Un número no se pudo convertir al tipo del campo.
     *
     * @param issue rechazo {@code EFX-ING-012} con el campo del formulario
     */
    record Invalid(ValidationIssue issue) implements MovieForm {
    }
}
