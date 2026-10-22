package com.bigfito.elastiflix.web.layout;

/**
 * Lo que la cabecera, el selector de idioma, el pie y Acerca de necesitan en cada página.
 *
 * @param connectionLabel texto de la pastilla de conexión
 * @param indexLabel      alias y conteo del pie
 * @param connected       si el catálogo respondió
 * @param catalogMissing  si Elasticsearch respondió pero el alias todavía no existe (Requerimiento 7)
 * @param tmdbNotice      atribución de TMDB en inglés
 * @param tmdbNoticeEs    misma atribución, en español
 * @param alias           nombre del alias configurado
 * @param moviePhrase     frase con el conteo, o aviso si no hay conteo
 * @param movies          conteo del catálogo con su unidad, por ejemplo {@code 6 899 películas}
 * @param lang            código de idioma activo ({@code es} o {@code en})
 * @param esHref          enlace del botón {@code ES} conservando los parámetros actuales
 * @param enHref          enlace del botón {@code EN} conservando los parámetros actuales
 */
public record PageShell(
        String connectionLabel,
        String indexLabel,
        boolean connected,
        boolean catalogMissing,
        String tmdbNotice,
        String tmdbNoticeEs,
        String alias,
        String moviePhrase,
        String movies,
        String lang,
        String esHref,
        String enHref) {
}
