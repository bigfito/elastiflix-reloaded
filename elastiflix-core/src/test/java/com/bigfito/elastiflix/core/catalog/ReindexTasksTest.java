package com.bigfito.elastiflix.core.catalog;

import java.util.Map;
import java.util.function.Function;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.ReindexResponse;
import co.elastic.clients.elasticsearch.tasks.ElasticsearchTasksClient;
import co.elastic.clients.elasticsearch.tasks.GetTasksResponse;
import co.elastic.clients.elasticsearch.tasks.TaskInfo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La copia no espera dentro de la llamada de reindex: consulta la tarea hasta que termina.
 */
class ReindexTasksTest {

    @Test
    @DisplayName("un reindex en segundo plano se da por listo cuando la tarea completed es true")
    void shouldWaitUntilTheTaskIsCompleted() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        ElasticsearchTasksClient tasks = mock(ElasticsearchTasksClient.class);
        when(client.reindex(any(Function.class)))
                .thenReturn(ReindexResponse.of(builder -> builder.task("nodo:1")));
        when(client.tasks()).thenReturn(tasks);
        when(tasks.get(any(Function.class)))
                .thenReturn(task(false))
                .thenReturn(task(true));

        new ReindexTasks(client).copy("origen", "destino");

        verify(tasks, times(2)).get(any(Function.class));
    }

    private static GetTasksResponse task(boolean completed) {
        TaskInfo info = TaskInfo.taskInfoOf(builder -> builder
                .action("indices:data/write/reindex")
                .cancellable(false)
                .headers(Map.of())
                .id(1)
                .node("nodo")
                .runningTimeInNanos(1)
                .startTimeInMillis(1)
                .type("transport"));
        return GetTasksResponse.of(builder -> builder.completed(completed).task(info));
    }
}
