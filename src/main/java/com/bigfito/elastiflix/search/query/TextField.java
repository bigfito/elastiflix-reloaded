package com.bigfito.elastiflix.search.query;

/**
 * Campo de título que usa {@code match}. {@code multi_match} no elige campo.
 */
public enum TextField {

    /** Título traducido o de exhibición. */
    TITLE("title"),

    /** Título en el idioma original. */
    ORIGINAL_TITLE("original_title");

    private final String fieldName;

    TextField(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * Nombre del campo en el índice.
     *
     * @return {@code title} u {@code original_title}
     */
    public String fieldName() {
        return fieldName;
    }
}
