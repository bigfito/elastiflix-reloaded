package com.bigfito.elastiflix.ingest.mapping;

/**
 * Escribe cifras con un espacio cada tres dígitos, como los mockups: {@code 4 322}.
 */
public final class Thousands {

    private static final int GROUP = 3;

    private Thousands() {
    }

    /**
     * Agrupa los dígitos de a tres desde la derecha.
     *
     * @param value cifra entera
     * @return texto con espacios, por ejemplo {@code 12 345}
     */
    public static String of(long value) {
        String digits = Long.toString(Math.abs(value));
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < digits.length(); index++) {
            if (index > 0 && (digits.length() - index) % GROUP == 0) {
                text.append(' ');
            }
            text.append(digits.charAt(index));
        }
        return value < 0 ? "-" + text : text.toString();
    }
}
