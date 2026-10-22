package com.bigfito.elastiflix.core.catalog;

/**
 * Qué es un nombre dentro del catálogo.
 *
 * <p>No lanza si el nombre no existe: quien migra o pide el estado decide el código de error.</p>
 */
public sealed interface CatalogName
        permits CatalogName.Alias, CatalogName.ConcreteIndex, CatalogName.Missing, CatalogName.Ambiguous {

    /**
     * Nombre que se consultó.
     *
     * @return alias o índice
     */
    String name();

    /**
     * El nombre es un alias de un solo índice.
     *
     * @param name          alias
     * @param concreteIndex índice concreto al que apunta
     */
    record Alias(String name, String concreteIndex) implements CatalogName {
    }

    /**
     * El nombre es un índice concreto, no un alias.
     *
     * @param name índice concreto
     */
    record ConcreteIndex(String name) implements CatalogName {
    }

    /**
     * No hay alias ni índice con ese nombre.
     *
     * @param name nombre consultado
     */
    record Missing(String name) implements CatalogName {
    }

    /**
     * El alias apunta a más de un índice.
     *
     * @param name alias
     */
    record Ambiguous(String name) implements CatalogName {
    }
}
