package com.bigfito.elastiflix.web;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * Codifica una URL de búsqueda para que el signo más sobreviva en la consulta.
 */
public final class EncodedQuery {

    private EncodedQuery() {
    }

    /**
     * Codifica la ruta. El {@code +} pasa a {@code %2B}: si se deja literal, el servidor lo lee como espacio.
     *
     * @param builder ruta y parámetros ya armados
     * @return URL lista para un enlace
     */
    public static String of(UriComponentsBuilder builder) {
        return builder.build().encode().toUriString().replace("+", "%2B");
    }
}
