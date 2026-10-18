package com.bigfito.elastiflix.core.catalog;

import java.io.IOException;

import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.ReindexResponse;
import co.elastic.clients.elasticsearch.tasks.GetTasksResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copia un índice con {@code _reindex} y espera la tarea.
 *
 * <p>La copia pide {@code wait_for_completion: false}, porque una espera larga dentro de
 * {@code _reindex} puede cortar la conexión. Después, una sola lectura de {@code _tasks} pide
 * {@code wait_for_completion: true} con un tope de 30 minutos. Este código no duerme.</p>
 */
final class ReindexTasks {

    /** Tope de la lectura de {@code _tasks}. El mismo margen que 3600 pausas de medio segundo. */
    private static final String WAIT_LIMIT = "30m";
    private static final Logger LOG = LoggerFactory.getLogger(ReindexTasks.class);

    private final ElasticsearchClient client;

    ReindexTasks(ElasticsearchClient client) {
        this.client = client;
    }

    /**
     * Copia {@code source} en {@code target}, vectores incluidos.
     *
     * @param source índice de origen
     * @param target índice nuevo, ya creado
     * @throws IOException si el cliente no puede hablar con Elasticsearch
     */
    void copy(String source, String target) throws IOException {
        ReindexResponse started = client.reindex(request -> request
                .source(origin -> origin.index(source).sourceFields(CatalogRequests.withVectors()))
                .dest(destination -> destination.index(target))
                .waitForCompletion(false));
        await(started.task(), source, target);
    }

    private void await(String taskId, String source, String target) throws IOException {
        if (taskId == null || taskId.isBlank()) {
            return;
        }
        LOG.info("Reindex de {} hacia {} en la tarea {}", source, target, taskId);
        requireFinished(client.tasks().get(request -> request
                .taskId(taskId)
                .waitForCompletion(true)
                .timeout(limit -> limit.time(WAIT_LIMIT))));
    }

    private static void requireFinished(GetTasksResponse task) {
        if (task.error() != null || !task.completed()) {
            throw new ElasticsearchAccessException(ErrorCode.REINDEX_FAILED);
        }
    }
}
