package com.bigfito.elastiflix.web.detail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Enlaces externos que vienen de los datos del catálogo. Solo se enlazan direcciones {@code http} o
 * {@code https}: un {@code homepage} con {@code javascript:} u otro esquema se trata como vacío.
 */
final class ExternalLinks {

    private static final Set<String> WEB_SCHEMES = Set.of("http", "https");

    private ExternalLinks() {
    }

    /**
     * Devuelve la dirección lista para un {@code href} si es web, o vacío si no se debe enlazar.
     *
     * @param url valor del documento; puede ser nulo
     * @return la dirección sin espacios en los extremos, o {@code ""}
     */
    static String webOrEmpty(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String candidate = url.strip();
        URI uri;
        try {
            uri = new URI(candidate);
        } catch (URISyntaxException exception) {
            // Un valor que no es una URI no se enlaza: la ficha lo muestra como «N/A».
            return "";
        }
        String scheme = uri.getScheme();
        boolean web = scheme != null && WEB_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT));
        return web && uri.getRawAuthority() != null ? candidate : "";
    }
}
