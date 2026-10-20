package com.bigfito.elastiflix.web.layout;

/**
 * Lo que la cabecera, el selector de idioma, el pie y Acerca de necesitan en cada página.
 *
 * @param connectionLabel texto de la pastilla de conexión
 * @param indexLabel      alias y conteo del pie
 * @param connected       si el catálogo respondió
 * @param tmdbNotice      atribución de TMDB en inglés
 * @param tmdbNoticeEs    misma atribución, en español
 * @param alias           nombre del alias configurado
 * @param moviePhrase     frase con el conteo, o aviso si no hay conteo
 * @param lang            código de idioma activo ({@code es} o {@code en})
 * @param esHref          enlace del botón {@code ES} conservando los parámetros actuales
 * @param enHref          enlace del botón {@code EN} conservando los parámetros actuales
 */
public record PageShell(
        String connectionLabel,
        String indexLabel,
        boolean connected,
        String tmdbNotice,
        String tmdbNoticeEs,
        String alias,
        String moviePhrase,
        String lang,
        String esHref,
        String enHref) {
}
