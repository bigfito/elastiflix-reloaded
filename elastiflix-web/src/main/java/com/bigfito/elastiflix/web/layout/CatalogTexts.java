package com.bigfito.elastiflix.web.layout;

/**
 * Textos fijos de la cabecera y del pie, compartidos por todas las páginas.
 */
public final class CatalogTexts {

    /** Pastilla del header cuando el catálogo responde. */
    public static final String CONNECTED = "Conectado · Elasticsearch Serverless";

    /** Atribución de TMDB, solo texto, en el pie de cada página. */
    public static final String TMDB =
            "This product uses the TMDB API but is not endorsed or certified by TMDB.";

    /** Traducción de la atribución, en la sección Créditos de Acerca de. */
    public static final String TMDB_ES =
            "Este producto usa la API de TMDB, pero no está avalado ni certificado por TMDB.";

    private static final int GROUP = 3;

    private CatalogTexts() {
    }

    /**
     * Línea del pie con el alias y el conteo, con espacio de millar.
     *
     * @param alias nombre del alias
     * @param count documentos, o {@code null} si no hay conteo
     * @return por ejemplo {@code Índice: elastiflix-reloaded · 4 322 películas}
     */
    public static String indexLine(String alias, Long count) {
        return "Índice: " + alias + " · " + movies(count);
    }

    /**
     * Pastilla cuando el catálogo no respondió.
     *
     * @param errorCode código {@code EFX-*}, o vacío
     * @return texto de la pastilla
     */
    public static String disconnected(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return "Sin conexión · Elasticsearch Serverless";
        }
        return "Sin conexión · " + errorCode;
    }

    /**
     * Frase del conteo para Acerca de.
     *
     * @param count documentos, o {@code null} si no hay conteo
     * @return por ejemplo {@code Tiene 4 322 películas.}
     */
    public static String moviePhrase(Long count) {
        if (count == null) {
            return "El conteo no está disponible.";
        }
        if (count == 1L) {
            return "Tiene 1 película.";
        }
        return "Tiene " + grouped(count) + " películas.";
    }

    private static String movies(Long count) {
        if (count == null) {
            return "sin películas";
        }
        if (count == 1L) {
            return "1 película";
        }
        return grouped(count) + " películas";
    }

    private static String grouped(long value) {
        String digits = Long.toString(value);
        StringBuilder text = new StringBuilder();
        int lead = digits.length() % GROUP;
        int index = 0;
        if (lead > 0) {
            text.append(digits, 0, lead);
            index = lead;
        }
        return rest(digits, text, index);
    }

    private static String rest(String digits, StringBuilder text, int index) {
        while (index < digits.length()) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(digits, index, index + GROUP);
            index = index + GROUP;
        }
        return text.toString();
    }
}
