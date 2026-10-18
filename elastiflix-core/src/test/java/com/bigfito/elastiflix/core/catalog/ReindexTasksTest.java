package com.bigfito.elastiflix.core.catalog;

import java.util.Map;
import java.util.function.Function;

import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.ReindexResponse;
import co.elastic.clients.elasticsearch.tasks.ElasticsearchTasksClient;
import co.elastic.clients.elasticsearch.tasks.GetTasksRequest;
import co.elastic.clients.elasticsearch.tasks.GetTasksResponse;
import co.elastic.clients.elasticsearch.tasks.TaskInfo;
import co.elastic.clients.util.ObjectBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La copia no duerme: una lectura de {@code _tasks} espera con tope.
 */
class ReindexTasksTest {

    @Test
    @DisplayName("la lectura de la tarea pide wait_for_completion y un tope de 30 minutos")
    void shouldAskTheTaskToWaitUntilItFinishes() throws Exception {
        ElasticsearchClient client = clientReturning("nodo:1", task(true, false));

        new ReindexTasks(client).copy("origen", "destino");

        verify(client.tasks()).get(any(Function.class));
    }

    @Test
    @DisplayName("si la tarea trae error, EFX-ES-006")
    void shouldFailWhenTheTaskReportsAnError() throws Exception {
        ElasticsearchClient client = clientReturning("nodo:1", task(true, true));

        assertThatThrownBy(() -> new ReindexTasks(client).copy("origen", "destino"))
                .isInstanceOf(ElasticsearchAccessException.class)
                .extracting(thrown -> ((ElasticsearchAccessException) thrown).errorCode())
                .isEqualTo(ErrorCode.REINDEX_FAILED);
    }

    @Test
    @DisplayName("si el tope se agota y la tarea no terminó, EFX-ES-006")
    void shouldFailWhenTheTaskIsStillRunning() throws Exception {
        ElasticsearchClient client = clientReturning("nodo:1", task(false, false));

        assertThatThrownBy(() -> new ReindexTasks(client).copy("origen", "destino"))
                .isInstanceOf(ElasticsearchAccessException.class)
                .extracting(thrown -> ((ElasticsearchAccessException) thrown).errorCode())
                .isEqualTo(ErrorCode.REINDEX_FAILED);
    }

    @Test
    @DisplayName("sin id de tarea no consulta _tasks")
    void shouldSkipTheTaskReadWhenThereIsNoTaskId() throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        when(client.reindex(any(Function.class)))
                .thenReturn(ReindexResponse.of(builder -> builder.task("")));

        new ReindexTasks(client).copy("origen", "destino");

        verify(client, never()).tasks();
    }

    private static ElasticsearchClient clientReturning(String taskId, GetTasksResponse task) throws Exception {
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        ElasticsearchTasksClient tasks = mock(ElasticsearchTasksClient.class);
        when(client.reindex(any(Function.class)))
                .thenReturn(ReindexResponse.of(builder -> builder.task(taskId)));
        when(client.tasks()).thenReturn(tasks);
        when(tasks.get(any(Function.class))).thenAnswer(invocation -> {
            assertWait(invocation.getArgument(0));
            return task;
        });
        return client;
    }

    /** Mockito entrega la función del cliente sin el tipo genérico. */
    @SuppressWarnings("unchecked")
    private static void assertWait(Object raw) {
        Function<GetTasksRequest.Builder, ObjectBuilder<GetTasksRequest>> request =
                (Function<GetTasksRequest.Builder, ObjectBuilder<GetTasksRequest>>) raw;
        GetTasksRequest built = request.apply(new GetTasksRequest.Builder()).build();

        assertThat(built.taskId()).isEqualTo("nodo:1");
        assertThat(built.waitForCompletion()).isTrue();
        assertThat(built.timeout().time()).isEqualTo("30m");
    }

    private static GetTasksResponse task(boolean completed, boolean failed) {
        TaskInfo info = TaskInfo.taskInfoOf(builder -> builder
                .action("indices:data/write/reindex")
                .cancellable(false)
                .headers(Map.of())
                .id(1)
                .node("nodo")
                .runningTimeInNanos(1)
                .startTimeInMillis(1)
                .type("transport"));
        return GetTasksResponse.of(builder -> {
            builder.completed(completed).task(info);
            if (failed) {
                builder.error(error -> error.type("reindex_failed").reason("fallo"));
            }
            return builder;
        });
    }
}
