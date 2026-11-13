package com.bigfito.elastiflix.core.catalog;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;

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
     * <p>Si la copia no termina en el plazo, se corta la conexión o {@code stop} dice sí, cancela la
     * tarea de reindex <b>antes</b> de lanzar: quien borre después el destino no compite con ella.</p>
     *
     * @param source índice de origen
     * @param target índice de destino
     * @param stop   se consulta durante la espera; si dice sí, lanza {@code EFX-IMP-003}
     */
    void reindex(String source, String target, BooleanSupplier stop);

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
     * Publica el alias en una sola llamada: lo agrega al índice nuevo y borra el anterior. Sin índice anterior (el
     * catálogo todavía no existe), solo lo agrega.
     *
     * <p>Pide {@code timeout} de 10 s, por debajo del socket-timeout del cliente ({@code master_timeout} no existe
     * en Serverless).
     * Si falla, la causa dice si fue un rechazo 4xx o un corte que pudo dejar la operación aplicada.</p>
     *
     * @param alias         alias del catálogo
     * @param newIndex      índice que se queda con el alias
     * @param previousIndex índice que se borra en la misma llamada, o {@code null} en la primera publicación
     */
    void publish(String alias, String newIndex, String previousIndex);

    /**
     * Borra un índice que esta demo creó.
     *
     * @param indexName índice concreto
     */
    void deleteIndex(String indexName);

    /**
     * Lista los índices {@code <alias>-<AAAAMMDD-HHMMSS>} de ese alias que ningún otro alias usa.
     *
     * <p>Un índice con un alias ajeno queda fuera aunque tenga el nombre: puede ser el catálogo de otro alias. Por
     * ejemplo, el índice de staging que se publicó en producción, que la limpieza de huérfanos de staging no debe
     * borrar.</p>
     *
     * @param alias alias del catálogo
     * @return nombres, ordenados; lista vacía si no hay
     */
    List<String> listOwnedIndexes(String alias);

    /**
     * Lee esas películas con sus vectores.
     *
     * <p>Lo usan las pruebas de integración (de core e ingest) para comprobar los vectores guardados.</p>
     *
     * @param name índice o alias
     * @param ids  identificadores de documento
     * @return películas encontradas
     */
    List<Movie> readWithVectors(String name, List<String> ids);

    /**
     * Lee películas con sus vectores, hasta el tope.
     *
     * <p>Lo usan las pruebas de integración (de core e ingest) para comprobar los vectores guardados.</p>
     *
     * @param name  índice o alias
     * @param limit máximo de documentos
     * @return películas leídas
     */
    List<Movie> readAllWithVectors(String name, int limit);

    /**
     * Dice cuáles de esos {@code _id} ya están. No pide el documento.
     *
     * <p>Pregunta de 500 en 500. Una lista vacía no llama al clúster.</p>
     *
     * @param name índice o alias
     * @param ids  identificadores a consultar
     * @return los que ya existen
     */
    Set<String> existingIds(String name, List<String> ids);

    /**
     * Escribe el lote en el índice concreto. {@code _id} es {@code id}. Reemplaza si ya estaba.
     *
     * <p>No hace {@code refresh}. Si un ítem viene rechazado, lanza {@code EFX-IMP-001}.</p>
     *
     * @param indexName índice concreto
     * @param movies    películas del lote
     */
    void writeAll(String indexName, List<Movie> movies);

    /**
     * Escribe el lote por el alias con {@code require_alias=true} (modo append). {@code _id} es {@code id}; reemplaza
     * si ya estaba. No lanza por los ítems rechazados: los cuenta, porque los aceptados ya quedaron escritos.
     *
     * @param alias  alias del catálogo; si no es un alias, Elasticsearch rechaza cada ítem y no crea un índice
     * @param movies películas con sus vectores
     * @return aceptados y rechazados
     */
    AppendResult appendAll(String alias, List<Movie> movies);

    /**
     * Indexa una película en el alias, con {@code refresh=wait_for}, y reemplaza ese {@code _id}.
     *
     * <p>Pide {@code require_alias=true}: si el alias no existe, Elasticsearch rechaza la escritura en lugar
     * de crear un índice concreto con ese nombre.</p>
     *
     * @param name  alias del catálogo
     * @param movie película ya vectorizada
     */
    void indexOne(String name, Movie movie);

    /**
     * Lee una película por su identificador, excluyendo los vectores de embeddings.
     *
     * <p>Excluye {@code plot_256}, {@code plot_512} y {@code plot_1024} para aligerar la transferencia.</p>
     *
     * @param name índice o alias del catálogo
     * @param id   identificador único del documento
     * @return la película encontrada, o vacío si no existe
     */
    Optional<Movie> findById(String name, String id);
}
