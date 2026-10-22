package com.bigfito.elastiflix.core.mapping;

import java.util.List;

/**
 * Un campo de primer nivel del mapping, con sus subcampos si es {@code nested}.
 *
 * @param name         nombre en el índice
 * @param type         tipo de Elasticsearch
 * @param format       formato, si el campo es una fecha; si no, {@code null}
 * @param dimensions   dimensiones, si el campo es un vector; si no, {@code null}
 * @param nestedFields nombres de los subcampos, vacía si el campo no es {@code nested}
 */
public record FieldMapping(
        String name,
        String type,
        String format,
        Integer dimensions,
        List<String> nestedFields) {
}
