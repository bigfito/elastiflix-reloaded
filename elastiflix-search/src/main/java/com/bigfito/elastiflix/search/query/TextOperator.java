package com.bigfito.elastiflix.search.query;

/**
 * Si basta una palabra de la consulta o hacen falta todas.
 */
public enum TextOperator {

    /** Basta una palabra. */
    OR("or"),

    /** Hacen falta todas, y con multi-match en el mismo campo. */
    AND("and");

    private final String elasticsearchOperator;

    TextOperator(String elasticsearchOperator) {
        this.elasticsearchOperator = elasticsearchOperator;
    }

    /**
     * Valor que entiende Elasticsearch: {@code or} o {@code and}.
     *
     * @return el operador en minúsculas
     */
    public String elasticsearchOperator() {
        return elasticsearchOperator;
    }
}
