package com.bigfito.elastiflix.web.layout;

/**
 * Lo que la cabecera, el pie y Acerca de necesitan en cada página.
 *
 * @param connectionLabel texto de la pastilla de conexión
 * @param indexLabel      alias y conteo del pie
 * @param connected       si el catálogo respondió
 * @param tmdbNotice      atribución de TMDB en inglés
 * @param tmdbNoticeEs    misma atribución, en español
 * @param alias           nombre del alias configurado
 * @param moviePhrase     frase con el conteo, o aviso si no hay conteo
 */
public record PageShell(
        String connectionLabel,
        String indexLabel,
        boolean connected,
        String tmdbNotice,
        String tmdbNoticeEs,
        String alias,
        String moviePhrase) {
}
