package com.bigfito.elastiflix.web.detail;

/**
 * Píldora de metadatos rápidos en la cabecera del detalle.
 *
 * @param field nombre técnico del campo
 * @param icon  trazado SVG del icono
 * @param value valor visible
 */
public record DetailPill(String field, String icon, String value) {
}
