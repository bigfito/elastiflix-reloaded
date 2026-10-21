package com.bigfito.elastiflix.web.layout;

import java.util.Locale;

import com.bigfito.elastiflix.core.text.Thousands;
import com.bigfito.elastiflix.web.locale.UiMessages;

/**
 * Textos de la cabecera y del pie, compartidos por todas las páginas, en el idioma de la persona.
 *
 * <p>Las cifras se agrupan con {@link Thousands}, el mismo helper de «Cargar datos» y de Resultados.</p>
 */
public final class CatalogTexts {

    /** Aviso oficial de atribución de TMDB; se muestra igual en los dos idiomas. */
    public static final String TMDB =
            "This product uses the TMDB API but is not endorsed or certified by TMDB.";

    /** Traducción del aviso de TMDB; también se muestra en los dos idiomas. */
    public static final String TMDB_ES =
            "Este producto usa la API de TMDB, pero no está avalado ni certificado por TMDB.";

    private CatalogTexts() {
    }

    /**
     * Pastilla de conexión: conectado, o sin conexión con el código EFX.
     *
     * @param connected si Elasticsearch respondió
     * @param errorCode código de la falla, o {@code null}
     * @param locale    idioma
     * @param messages  catálogo de mensajes
     * @return texto de la pastilla
     */
    public static String connection(boolean connected, String errorCode, Locale locale, UiMessages messages) {
        if (connected) {
            return messages.getFor(locale, "layout.catalog.connected");
        }
        String cause = errorCode == null || errorCode.isBlank() ? "Elasticsearch Serverless" : errorCode;
        return messages.getFor(locale, "layout.catalog.disconnected", cause);
    }

    /**
     * Línea del pie, por ejemplo {@code Índice: elastiflix-reloaded · 6 899 películas}.
     *
     * @param alias    alias del catálogo
     * @param count    documentos, o {@code null} si no se pudo contar
     * @param locale   idioma
     * @param messages catálogo de mensajes
     * @return la línea del pie
     */
    public static String indexLine(String alias, Long count, Locale locale, UiMessages messages) {
        return messages.getFor(locale, "layout.catalog.index", alias,
                counted(count, "layout.catalog.movies", locale, messages));
    }

    /**
     * Conteo del catálogo con su unidad, por ejemplo {@code 6 899 películas}; lo usa el aviso de bulk.
     *
     * @param count    documentos, o {@code null} si no se pudo contar
     * @param locale   idioma
     * @param messages catálogo de mensajes
     * @return el conteo
     */
    public static String movies(Long count, Locale locale, UiMessages messages) {
        return counted(count, "layout.catalog.movies", locale, messages);
    }

    /**
     * Frase de «Acerca de» › «El catálogo», por ejemplo {@code Tiene 6 899 películas}.
     *
     * @param count    documentos, o {@code null} si no se pudo contar
     * @param locale   idioma
     * @param messages catálogo de mensajes
     * @return la frase
     */
    public static String moviePhrase(Long count, Locale locale, UiMessages messages) {
        return counted(count, "layout.catalog.phrase", locale, messages);
    }

    private static String counted(Long count, String key, Locale locale, UiMessages messages) {
        if (count == null) {
            return messages.getFor(locale, key + ".unknown");
        }
        if (count == 1L) {
            return messages.getFor(locale, key + ".one");
        }
        return messages.getFor(locale, key + ".many", Thousands.of(count, locale));
    }
}
