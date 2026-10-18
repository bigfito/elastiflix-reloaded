package com.bigfito.elastiflix.core.catalog;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Arma y reconoce índices {@code <alias>-<AAAAMMDD-HHMMSS>} en UTC.
 */
@Component
public class IndexNames {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss").withZone(ZoneOffset.UTC);

    /**
     * Nombre de un índice nuevo para ese alias, con la hora del reloj.
     *
     * @param alias alias del catálogo
     * @param clock reloj; la hora se escribe en UTC
     * @return nombre del índice
     */
    public String timestamped(String alias, Clock clock) {
        return alias + "-" + STAMP.format(clock.instant());
    }

    /**
     * Dice si el índice pertenece a ese alias y sigue el patrón de esta demo.
     *
     * @param alias     alias del catálogo
     * @param indexName nombre a comprobar
     * @return {@code true} solo si el nombre es {@code <alias>-<AAAAMMDD-HHMMSS>}
     */
    public boolean isOwnedBy(String alias, String indexName) {
        return indexName.matches(Pattern.quote(alias) + "-\\d{8}-\\d{6}");
    }
}
