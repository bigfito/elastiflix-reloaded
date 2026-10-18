package com.bigfito.elastiflix.core.catalog;

import java.util.List;

import com.bigfito.elastiflix.core.model.Movie;

/**
 * Operaciones del catálogo, sin los detalles del cliente de Elasticsearch.
 */
public interface CatalogIndex {

    /**
     * Dice si el nombre es un alias, un índice concreto, no existe, o apunta a varios índices.
     *
     * @param name alias o índice
     * @return una de las cuatro formas de {@link CatalogName}
     */
    CatalogName describe(String name);

    /**
     * Crea un índice con el mapping empaquetado.
     *
     * @param indexName nombre del índice nuevo
     */
    void createWithMapping(String indexName);

    /**
     * Copia los documentos de un índice a otro y espera a que termine.
     *
     * @param source índice de origen
     * @param target índice de destino
     */
    void reindex(String source, String target);

    /**
     * Hace visibles los documentos recién escritos.
     *
     * @param name índice o alias
     */
    void refresh(String name);

    /**
     * Cuenta los documentos.
     *
     * @param name índice o alias
     * @return cantidad de documentos
     */
    long count(String name);

    /**
     * Publica el alias en una sola llamada: lo agrega al índice nuevo y borra el anterior.
     *
     * @param alias         alias del catálogo
     * @param newIndex      índice que se queda con el alias
     * @param previousIndex índice que se borra en la misma llamada
     */
    void publish(String alias, String newIndex, String previousIndex);

    /**
     * Borra un índice que esta demo creó.
     *
     * @param indexName índice concreto
     */
    void deleteIndex(String indexName);

    /**
     * Lista los índices {@code <alias>-<AAAAMMDD-HHMMSS>} de ese alias.
     *
     * @param alias alias del catálogo
     * @return nombres, ordenados; lista vacía si no hay
     */
    List<String> listOwnedIndexes(String alias);

    /**
     * Lee esas películas con sus vectores.
     *
     * @param name índice o alias
     * @param ids  identificadores de documento
     * @return películas encontradas
     */
    List<Movie> readWithVectors(String name, List<String> ids);

    /**
     * Lee películas con sus vectores, hasta el tope.
     *
     * @param name  índice o alias
     * @param limit máximo de documentos
     * @return películas leídas
     */
    List<Movie> readAllWithVectors(String name, int limit);
}
