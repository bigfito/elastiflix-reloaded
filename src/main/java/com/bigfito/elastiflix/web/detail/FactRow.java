package com.bigfito.elastiflix.web.detail;

import java.util.List;
import java.util.Locale;

import com.bigfito.elastiflix.core.text.Thousands;

/**
 * Fila de datos en los paneles informativos del detalle.
 *
 * @param label   etiqueta en español
 * @param field   nombre técnico del campo
 * @param value   valor formateado o «N/A»
 * @param href    enlace web si aplica
 * @param isLink  si debe renderizarse como enlace
 * @param isText  si debe renderizarse como texto normal
 * @param isEmpty si el valor está ausente («N/A»)
 */
public record FactRow(
        String label,
        String field,
        String value,
        String href,
        boolean isLink,
        boolean isText,
        boolean isEmpty) {

    /** Texto de un campo sin valor: ausente, {@code null}, texto en blanco o lista vacía. */
    static final String NOT_AVAILABLE = "N/A";

    /**
     * Devuelve el valor, o «N/A» si está ausente o en blanco.
     *
     * @param value valor ya formateado; puede ser nulo
     * @return el valor o «N/A»
     */
    static String orNotAvailable(String value) {
        return value == null || value.isBlank() ? NOT_AVAILABLE : value;
    }

    /**
     * Fila con texto simple.
     *
     * @param label etiqueta
     * @param field campo
     * @param value valor
     * @return fila armada
     */
    public static FactRow text(String label, String field, String value) {
        if (value == null || value.isBlank()) {
            return empty(label, field);
        }
        return new FactRow(label, field, value, "", false, true, false);
    }

    /**
     * Fila con enlace interactivo.
     *
     * @param label etiqueta
     * @param field campo
     * @param value texto visible
     * @param href  destino
     * @return fila armada
     */
    public static FactRow link(String label, String field, String value, String href) {
        if (value == null || value.isBlank() || href == null || href.isBlank()) {
            return empty(label, field);
        }
        return new FactRow(label, field, value, href, true, false, false);
    }

    /**
     * Fila vacía («N/A»).
     *
     * @param label etiqueta
     * @param field campo
     * @return fila vacía
     */
    public static FactRow empty(String label, String field) {
        return new FactRow(label, field, NOT_AVAILABLE, "", false, false, true);
    }

    /**
     * Fila con importe monetario en USD con separador de miles según el idioma,
     * o «N/A» si es nulo o menor/igual a cero.
     *
     * @param label  etiqueta
     * @param field  campo
     * @param amount monto
     * @param locale idioma para el separador de miles
     * @return fila formateada
     */
    public static FactRow money(String label, String field, Double amount, Locale locale) {
        if (amount == null || amount <= 0) {
            return empty(label, field);
        }
        return text(label, field, Thousands.of(Math.round(amount), locale) + " USD");
    }

    /**
     * Fila con valor booleano localizado.
     *
     * @param label   etiqueta
     * @param field   campo
     * @param flag    booleano
     * @param yesText texto afirmativo
     * @param noText  texto negativo
     * @return fila formateada
     */
    public static FactRow yesNo(String label, String field, Boolean flag, String yesText, String noText) {
        if (flag == null) {
            return empty(label, field);
        }
        return text(label, field, flag ? yesText : noText);
    }

    /**
     * Fila con lista de elementos separados por coma.
     *
     * @param label etiqueta
     * @param field campo
     * @param items lista de textos
     * @return fila formateada
     */
    public static FactRow list(String label, String field, List<String> items) {
        if (items == null || items.isEmpty()) {
            return empty(label, field);
        }
        return text(label, field, String.join(", ", items));
    }
}
