package com.bigfito.elastiflix.web.about;

/**
 * Una fila de «Acerca de» › «El catálogo»: un campo del mapping, su tipo, qué guarda, dónde lo usa la demo y si un
 * archivo de carga tiene que traerlo.
 *
 * @param name       nombre en el mapping; los subcampos de un objeto anidado van con su padre, por ejemplo
 *                   {@code keywords.name}
 * @param type       tipo del mapping, por ejemplo {@code keyword} o {@code dense_vector}
 * @param contentKey clave del texto «Contenido»
 * @param useKey     clave del texto «Uso»
 * @param jsonKey    clave del texto «Obligatorio en .json»
 * @param csvKey     clave del texto «Obligatorio en .csv»
 */
public record CatalogField(
        String name, String type, String contentKey, String useKey, String jsonKey, String csvKey) {

    /** Clave del «Sí»: la celda va resaltada. */
    public static final String REQUIRED_KEY = "about.catalog.req.yes";

    /**
     * Dice si es un subcampo de un objeto anidado; la tabla lo muestra con sangría.
     *
     * @return {@code true} si el nombre lleva el de su padre
     */
    public boolean subfield() {
        return name.contains(".");
    }

    /**
     * Dice si un archivo de carga tiene que traer este campo (en .json y en .csv es el mismo conjunto).
     *
     * @return {@code true} para {@code id}, {@code title} y {@code plot}
     */
    public boolean required() {
        return REQUIRED_KEY.equals(jsonKey);
    }
}
