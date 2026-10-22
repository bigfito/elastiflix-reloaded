package com.bigfito.elastiflix.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Palabra clave anidada de una película.
 *
 * @param id   id numérico de la palabra clave
 * @param name texto de la palabra clave
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MovieKeyword(Long id, String name) {
}
