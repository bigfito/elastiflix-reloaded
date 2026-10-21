package com.bigfito.elastiflix.web.about;

/**
 * Una fila de «Acerca de» › «El catálogo»: un campo del mapping, su tipo, qué guarda y dónde lo usa la demo.
 *
 * @param name       nombre en el mapping; los subcampos de un objeto anidado van con su padre, por ejemplo
 *                   {@code keywords.name}
 * @param type       tipo del mapping, por ejemplo {@code keyword} o {@code dense_vector}
 * @param contentKey clave del texto «Contenido»
 * @param useKey     clave del texto «Uso»
 */
public record CatalogField(String name, String type, String contentKey, String useKey) {

    /**
     * Dice si es un subcampo de un objeto anidado; la tabla lo muestra con sangría.
     *
     * @return {@code true} si el nombre lleva el de su padre
     */
    public boolean subfield() {
        return name.contains(".");
    }
}
