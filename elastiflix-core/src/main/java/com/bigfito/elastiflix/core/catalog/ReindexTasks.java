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
 * Copia un índice con {@code _reindex} en segundo plano y espera la tarea.
 *
 * <p>{@code wait_for_completion: true} puede cortar la conexión en un catálogo grande. Aquí la
 * llamada vuelve enseguida y se consulta {@code _tasks} hasta que la copia termina.</p>
 */
final class ReindexTasks {

    private static final long PAUSE_MILLIS = 500L;
    private static final int MAX_POLLS = 3600;
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
        for (int poll = 0; poll < MAX_POLLS; poll++) {
            if (finished(client.tasks().get(request -> request.taskId(taskId)))) {
                return;
            }
            pause();
        }
        throw new ElasticsearchAccessException(ErrorCode.REINDEX_FAILED);
    }

    private static boolean finished(GetTasksResponse task) {
        if (task.error() != null) {
            throw new ElasticsearchAccessException(ErrorCode.REINDEX_FAILED);
        }
        return task.completed();
    }

    private static void pause() {
        try {
            Thread.sleep(PAUSE_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ElasticsearchAccessException(ErrorCode.REINDEX_FAILED, exception);
        }
    }
}
