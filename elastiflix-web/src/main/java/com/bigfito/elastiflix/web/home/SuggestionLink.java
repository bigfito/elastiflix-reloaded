package com.bigfito.elastiflix.web.home;

/**
 * Una sugerencia fija del mockup, ya lista para pulsar.
 *
 * @param text  consulta
 * @param tag   etiqueta que describe el catálogo original
 * @param emoji icono del mockup
 * @param href  enlace a {@code /buscar} con los parámetros de ese modo
 */
public record SuggestionLink(String text, String tag, String emoji, String href) {
}
