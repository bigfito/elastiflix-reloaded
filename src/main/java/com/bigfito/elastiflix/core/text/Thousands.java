package com.bigfito.elastiflix.core.text;

import java.util.Locale;

/**
 * Escribe cifras con un separador cada tres dígitos, como los mockups: espacio en español ({@code 4 322})
 * y coma en inglés ({@code 4,322}). La usan la búsqueda, la carga y la web.
 */
public final class Thousands {

    private static final int GROUP = 3;
    private static final char SPACE = ' ';
    private static final char COMMA = ',';

    private Thousands() {
    }

    /**
     * Agrupa los dígitos de a tres desde la derecha con espacio (formato en español).
     *
     * @param value cifra entera
     * @return texto con espacios, por ejemplo {@code 12 345}
     */
    public static String of(long value) {
        return format(value, SPACE);
    }

    /**
     * Agrupa los dígitos de a tres desde la derecha según el idioma: coma en inglés ({@code en}) y
     * espacio en español.
     *
     * @param value  cifra entera
     * @param locale idioma activo (si es {@code null} o distinto de {@code en}, usa espacio)
     * @return texto con separador de miles
     */
    public static String of(long value, Locale locale) {
        char separator = locale != null && "en".equalsIgnoreCase(locale.getLanguage()) ? COMMA : SPACE;
        return format(value, separator);
    }

    private static String format(long value, char separator) {
        String digits = value < 0 ? Long.toString(value).substring(1) : Long.toString(value);
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < digits.length(); index++) {
            if (index > 0 && (digits.length() - index) % GROUP == 0) {
                text.append(separator);
            }
            text.append(digits.charAt(index));
        }
        return value < 0 ? "-" + text : text.toString();
    }
}
