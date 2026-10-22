package com.bigfito.elastiflix.search.transport;

import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.MsearchRequest;
import co.elastic.clients.elasticsearch.core.MsearchResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;

/**
 * Ejecuta {@code _search}, {@code _msearch} y {@code _count}. Las estrategias no llaman al cliente.
 */
public interface SearchTransport {

    /**
     * Ejecuta una búsqueda.
     *
     * @param request      petición ya armada
     * @param documentType clase del documento, normalmente la película
     * @param <T>          tipo del documento
     * @return la respuesta de Elasticsearch
     */
    <T> SearchResponse<T> search(SearchRequest request, Class<T> documentType);

    /**
     * Ejecuta varias búsquedas en una sola llamada.
     *
     * @param request      petición con las tres partes de la híbrida
     * @param documentType clase del documento
     * @param <T>          tipo del documento
     * @return una respuesta por cada búsqueda
     */
    <T> MsearchResponse<T> msearch(MsearchRequest request, Class<T> documentType);

    /**
     * Cuenta documentos que cumplen la consulta, sin devolverlos.
     *
     * @param request consulta de conteo
     * @return el total
     */
    long count(CountRequest request);
}
