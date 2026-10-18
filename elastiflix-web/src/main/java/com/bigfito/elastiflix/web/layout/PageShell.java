package com.bigfito.elastiflix.web.layout;

/**
 * Lo que la cabecera y el pie necesitan en cada página.
 *
 * @param connectionLabel texto de la pastilla de conexión
 * @param indexLabel      alias y conteo
 * @param connected       si el catálogo respondió
 * @param tmdbNotice      atribución de TMDB
 */
public record PageShell(String connectionLabel, String indexLabel, boolean connected, String tmdbNotice) {
}
