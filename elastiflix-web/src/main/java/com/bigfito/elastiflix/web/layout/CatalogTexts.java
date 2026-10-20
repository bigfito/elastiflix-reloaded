package com.bigfito.elastiflix.web.layout;

import java.util.Locale;

import com.bigfito.elastiflix.core.text.Thousands;
import com.bigfito.elastiflix.web.locale.SupportedLocale;

/**
 * Textos fijos de la cabecera y del pie, compartidos por todas las páginas.
 *
 * <p>Las cifras se agrupan con {@link Thousands}, el mismo helper de «Cargar datos» y de Resultados.</p>
 */
public final class CatalogTexts {

    /** Pastilla del header cuando el catálogo responde (español). */
    public static final String CONNECTED = "Conectado · Elasticsearch Serverless";

    /** Pastilla del header cuando el catálogo responde (inglés). */
    public static final String CONNECTED_EN = "Connected · Elasticsearch Serverless";

    /** Atribución de TMDB, solo texto, en el pie de cada página. */
    public static final String TMDB =
            "This product uses the TMDB API but is not endorsed or certified by TMDB.";

    /** Traducción de la atribución, en la sección Créditos de Acerca de. */
    public static final String TMDB_ES =
            "Este producto usa la API de TMDB, pero no está avalado ni certificado por TMDB.";

    private CatalogTexts() {
    }

    /**
     * Pastilla del header cuando el catálogo responde, según el idioma.
     *
     * @param locale idioma activo
     * @return texto de la pastilla
     */
    public static String connected(Locale locale) {
        return SupportedLocale.isEnglish(locale) ? CONNECTED_EN : CONNECTED;
    }

    /**
     * Línea del pie con el alias y el conteo, con espacio de millar (español).
     *
     * @param alias nombre del alias
     * @param count documentos, o {@code null} si no hay conteo
     * @return por ejemplo {@code Índice: elastiflix-reloaded · 6 899 películas}
     */
    public static String indexLine(String alias, Long count) {
        return indexLine(alias, count, SupportedLocale.ES);
    }

    /**
     * Línea del pie con el alias y el conteo, según el idioma.
     *
     * @param alias  nombre del alias
     * @param count  documentos, o {@code null} si no hay conteo
     * @param locale idioma activo
     * @return texto del pie
     */
    public static String indexLine(String alias, Long count, Locale locale) {
        if (SupportedLocale.isEnglish(locale)) {
            return "Index: " + alias + " · " + moviesEn(count, locale);
        }
        return "Índice: " + alias + " · " + moviesEs(count, locale);
    }

    /**
     * Pastilla cuando el catálogo no respondió (español).
     *
     * @param errorCode código {@code EFX-*}, o vacío
     * @return texto de la pastilla
     */
    public static String disconnected(String errorCode) {
        return disconnected(errorCode, SupportedLocale.ES);
    }

    /**
     * Pastilla cuando el catálogo no respondió, según el idioma.
     *
     * @param errorCode código {@code EFX-*}, o vacío
     * @param locale    idioma activo
     * @return texto de la pastilla
     */
    public static String disconnected(String errorCode, Locale locale) {
        String prefix = SupportedLocale.isEnglish(locale) ? "Disconnected · " : "Sin conexión · ";
        if (errorCode == null || errorCode.isBlank()) {
            return prefix + "Elasticsearch Serverless";
        }
        return prefix + errorCode;
    }

    /**
     * Frase del conteo para Acerca de (español).
     *
     * @param count documentos, o {@code null} si no hay conteo
     * @return por ejemplo {@code Tiene 6 899 películas.}
     */
    public static String moviePhrase(Long count) {
        return moviePhrase(count, SupportedLocale.ES);
    }

    /**
     * Frase del conteo para Acerca de, según el idioma.
     *
     * @param count  documentos, o {@code null} si no hay conteo
     * @param locale idioma activo
     * @return frase en español o en inglés
     */
    public static String moviePhrase(Long count, Locale locale) {
        if (SupportedLocale.isEnglish(locale)) {
            return moviePhraseEn(count, locale);
        }
        return moviePhraseEs(count, locale);
    }

    private static String moviePhraseEs(Long count, Locale locale) {
        if (count == null) {
            return "El conteo no está disponible.";
        }
        if (count == 1L) {
            return "Tiene 1 película.";
        }
        return "Tiene " + Thousands.of(count, locale) + " películas.";
    }

    private static String moviePhraseEn(Long count, Locale locale) {
        if (count == null) {
            return "The count is unavailable.";
        }
        if (count == 1L) {
            return "It has 1 movie.";
        }
        return "It has " + Thousands.of(count, locale) + " movies.";
    }

    private static String moviesEs(Long count, Locale locale) {
        if (count == null) {
            return "conteo no disponible";
        }
        if (count == 1L) {
            return "1 película";
        }
        return Thousands.of(count, locale) + " películas";
    }

    private static String moviesEn(Long count, Locale locale) {
        if (count == null) {
            return "count unavailable";
        }
        if (count == 1L) {
            return "1 movie";
        }
        return Thousands.of(count, locale) + " movies";
    }
}
