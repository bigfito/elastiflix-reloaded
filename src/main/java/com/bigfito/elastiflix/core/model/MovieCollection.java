package com.bigfito.elastiflix.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Colección a la que pertenece una película. Puede faltar.
 *
 * @param id           id numérico de la colección
 * @param name         nombre de la colección
 * @param backdropPath ruta del fondo, si viene
 * @param posterPath   ruta del póster, si viene
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MovieCollection(Long id, String name, String backdropPath, String posterPath) {
}
