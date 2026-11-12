package com.bigfito.elastiflix.search.query;

/**
 * Campo de texto que usa {@code match}. {@code multi_match} no elige campo: busca en los dos títulos.
 */
public enum TextField {

    /** Título traducido o de exhibición. */
    TITLE("title"),

    /** Título en el idioma original. */
    ORIGINAL_TITLE("original_title"),

    /** Sinopsis en inglés, la misma de la que salen los vectores. */
    PLOT("plot");

    private final String fieldName;

    TextField(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * Nombre del campo en el índice.
     *
     * @return {@code title}, {@code original_title} o {@code plot}
     */
    public String fieldName() {
        return fieldName;
    }
}
