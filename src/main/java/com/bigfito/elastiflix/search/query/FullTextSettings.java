package com.bigfito.elastiflix.search.query;

/**
 * Ajustes de la parte léxica. En modo vectorial no se envían.
 *
 * @param type     {@code match} de un campo, o {@code multi_match} de los dos
 * @param field    campo de {@code match}; {@code null} cuando el tipo es {@code multi_match}
 * @param operator si basta una palabra o hacen falta todas
 */
public record FullTextSettings(MatchType type, TextField field, TextOperator operator) {
}
