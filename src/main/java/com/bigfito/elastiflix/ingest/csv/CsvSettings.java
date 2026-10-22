package com.bigfito.elastiflix.ingest.csv;

/**
 * Separador del CSV y si quien sube el archivo confirma el encabezado.
 *
 * @param separator     coma, punto y coma o tabulador
 * @param headerPresent {@code false} rechaza el archivo sin leer filas
 */
public record CsvSettings(char separator, boolean headerPresent) {

    /** Coma, el separador por defecto. */
    public static final char COMMA = ',';

    /** Punto y coma. */
    public static final char SEMICOLON = ';';

    /** Tabulador. */
    public static final char TAB = '\t';

    /**
     * CSV habitual: coma y primera fila con los nombres.
     *
     * @return ajustes por defecto
     */
    public static CsvSettings withHeader() {
        return new CsvSettings(COMMA, true);
    }
}
