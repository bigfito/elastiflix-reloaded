package com.bigfito.elastiflix.ingest.mapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Fecha estricta {@code yyyy-MM-dd}.
 */
final class Dates {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    private Dates() {
    }

    static boolean valid(String text) {
        try {
            LocalDate.parse(text, DATE);
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }
}
