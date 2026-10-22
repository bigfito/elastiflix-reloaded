package com.bigfito.elastiflix.web.detail;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import org.springframework.web.util.UriUtils;

/**
 * Enlaces externos que vienen de los datos del catálogo: el {@code homepage} y la página de IMDb.
 *
 * <p>Solo se enlazan direcciones {@code http} o {@code https}: un {@code homepage} con {@code javascript:} u otro
 * esquema se trata como vacío.</p>
 */
final class ExternalLinks {

    private static final Set<String> WEB_SCHEMES = Set.of("http", "https");
    private static final String IMDB_TITLE = "https://www.imdb.com/title/";

    private ExternalLinks() {
    }

    /**
     * Devuelve la página del título en IMDb, o vacío si la película no tiene {@code imdb_id}.
     *
     * <p>El id va como un segmento codificado, para que un valor raro no cambie la ruta.</p>
     *
     * @param imdbId identificador de IMDb, por ejemplo {@code tt1375666}; puede ser nulo
     * @return {@code https://www.imdb.com/title/<imdb_id>/}, o {@code ""}
     */
    static String imdb(String imdbId) {
        if (imdbId == null || imdbId.isBlank()) {
            return "";
        }
        return IMDB_TITLE + UriUtils.encodePathSegment(imdbId.strip(), StandardCharsets.UTF_8) + "/";
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
