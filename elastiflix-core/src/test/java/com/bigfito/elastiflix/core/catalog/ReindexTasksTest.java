package com.bigfito.elastiflix.core.catalog;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.function.Function;

import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ElastiflixException;
import com.bigfito.elastiflix.core.error.ErrorCode;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch.core.ReindexResponse;
import co.elastic.clients.elasticsearch.tasks.ElasticsearchTasksClient;
import co.elastic.clients.elasticsearch.tasks.GetTasksRequest;
import co.elastic.clients.elasticsearch.tasks.GetTasksResponse;
import co.elastic.clients.elasticsearch.tasks.TaskInfo;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La copia espera en un bucle de lecturas de {@code _tasks}, revisa el cuerpo de la tarea y cancela
 * antes de lanzar si no terminó.
 */
class ReindexTasksTest {

    private static final String TASK = "nodo:1";
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-10-18T03:04:05Z"), ZoneOffset.UTC);
    private static final String CLEAN = "{\"failures\":[],\"timed_out\":false}";

    private final ElasticsearchClient client = mock(ElasticsearchClient.class);
    private final ElasticsearchTasksClient tasks = mock(ElasticsearchTasksClient.class);

    @Test
    @DisplayName("cada lectura pide wait_for_completion y espera 20 s, por debajo del socket-timeout")
    void shouldAskEachReadToWaitTwentySeconds() throws Exception {
        startsTask(TASK);
        when(tasks.get(any(Function.class))).thenAnswer(invocation -> {
            assertWait(invocation.getArgument(0));
            return task(true, CLEAN, false);
        });

        new ReindexTasks(client, FIXED).copy("origen", "destino", () -> false);

        verify(tasks, never()).cancel(any(Function.class));
    }

    @Test
    @DisplayName("una lectura con timeout_exception sigue esperando, y termina con completed=true")
    void shouldKeepWaitingAfterATimeoutException() throws Exception {
        startsTask(TASK);
        when(tasks.get(any(Function.class)))
                .thenThrow(timeoutException())
                .thenReturn(task(true, CLEAN, false));

        new ReindexTasks(client, FIXED).copy("origen", "destino", () -> false);

        verify(tasks, times(2)).get(any(Function.class));
        verify(tasks, never()).cancel(any(Function.class));
    }

    @Test
    @DisplayName("completed=false también sigue esperando")
    void shouldKeepWaitingWhileTheTaskIsNotCompleted() throws Exception {
        startsTask(TASK);
        when(tasks.get(any(Function.class)))
                .thenReturn(task(false, null, false))
                .thenReturn(task(true, CLEAN, false));

        new ReindexTasks(client, FIXED).copy("origen", "destino", () -> false);

        verify(tasks, times(2)).get(any(Function.class));
    }

    @Test
    @DisplayName("failures no vacío en la respuesta de la tarea da EFX-ES-006")
    void shouldFailWhenTheTaskReportsFailures() throws Exception {
        startsTask(TASK);
        when(tasks.get(any(Function.class))).thenReturn(task(true,
                "{\"failures\":[{\"index\":\"destino\",\"id\":\"1\"}],\"timed_out\":false}", false));

        assertCode(() -> new ReindexTasks(client, FIXED).copy("origen", "destino", () -> false),
                ErrorCode.REINDEX_FAILED);
    }

    @Test
    @DisplayName("timed_out=true en la respuesta de la tarea da EFX-ES-006")
    void shouldFailWhenTheTaskTimedOut() throws Exception {
        startsTask(TASK);
        when(tasks.get(any(Function.class)))
                .thenReturn(task(true, "{\"failures\":[],\"timed_out\":true}", false));

        assertCode(() -> new ReindexTasks(client, FIXED).copy("origen", "destino", () -> false),
                ErrorCode.REINDEX_FAILED);
    }

    @Test
    @DisplayName("si la tarea trae error, EFX-ES-006")
    void shouldFailWhenTheTaskReportsAnError() throws Exception {
        startsTask(TASK);
        when(tasks.get(any(Function.class))).thenReturn(task(true, null, true));

        assertCode(() -> new ReindexTasks(client, FIXED).copy("origen", "destino", () -> false),
                ErrorCode.REINDEX_FAILED);
    }

    @Test
    @DisplayName("si vence el plazo de 30 minutos, cancela la tarea y da EFX-ES-006")
    void shouldCancelWhenTheDeadlinePasses() throws Exception {
        startsTask(TASK);
        when(tasks.get(any(Function.class))).thenThrow(timeoutException());

        assertCode(() -> new ReindexTasks(client, new SteppingClock(Duration.ofMinutes(11)))
                .copy("origen", "destino", () -> false), ErrorCode.REINDEX_FAILED);
        verify(tasks).cancel(any(Function.class));
    }

    @Test
    @DisplayName("si se corta la conexión, cancela la tarea y deja pasar el IOException")
    void shouldCancelWhenTheConnectionFails() throws Exception {
        startsTask(TASK);
        when(tasks.get(any(Function.class))).thenThrow(new IOException("socket timeout"));

        assertThatThrownBy(() -> new ReindexTasks(client, FIXED).copy("origen", "destino", () -> false))
                .isInstanceOf(IOException.class);
        verify(tasks).cancel(any(Function.class));
    }

    @Test
    @DisplayName("si la persona cancela durante la espera, cancela la tarea y da EFX-IMP-003")
    void shouldCancelWhenStopIsRequested() throws Exception {
        startsTask(TASK);

        assertCode(() -> new ReindexTasks(client, FIXED).copy("origen", "destino", () -> true),
                ErrorCode.IMPORT_CANCELLED);
        verify(tasks).cancel(any(Function.class));
        verify(tasks, never()).get(any(Function.class));
    }

    @Test
    @DisplayName("sin id de tarea da EFX-ES-006 y no da la copia por terminada")
    void shouldFailWhenThereIsNoTaskId() throws Exception {
        startsTask("");

        assertCode(() -> new ReindexTasks(client, FIXED).copy("origen", "destino", () -> false),
                ErrorCode.REINDEX_FAILED);
        verify(client, never()).tasks();
    }

    private void startsTask(String taskId) throws IOException {
        when(client.reindex(any(Function.class)))
                .thenReturn(ReindexResponse.of(builder -> builder.task(taskId)));
        when(client.tasks()).thenReturn(tasks);
    }

    private static void assertCode(ThrowingCall call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(ElastiflixException.class)
                .extracting(thrown -> ((ElastiflixException) thrown).errorCode())
                .isEqualTo(expected);
    }

    private static ElasticsearchException timeoutException() {
        return new ElasticsearchException("tasks.get", ErrorResponse.of(response -> response
                .status(408)
                .error(cause -> cause.type("timeout_exception").reason("Timed out waiting for completion"))));
    }

    /** Mockito entrega la función del cliente sin el tipo genérico. */
    @SuppressWarnings("unchecked")
    private static void assertWait(Object raw) {
        Function<GetTasksRequest.Builder, ObjectBuilder<GetTasksRequest>> request =
                (Function<GetTasksRequest.Builder, ObjectBuilder<GetTasksRequest>>) raw;
        GetTasksRequest built = request.apply(new GetTasksRequest.Builder()).build();

        assertThat(built.taskId()).isEqualTo(TASK);
        assertThat(built.waitForCompletion()).isTrue();
        assertThat(built.timeout().time()).isEqualTo("20s");
    }

    private static GetTasksResponse task(boolean completed, String body, boolean failed) {
        TaskInfo info = TaskInfo.taskInfoOf(builder -> builder
                .action("indices:data/write/reindex")
                .cancellable(true)
                .headers(Map.of())
                .id(1)
                .node("nodo")
                .runningTimeInNanos(1)
                .startTimeInMillis(1)
                .type("transport"));
        return GetTasksResponse.of(builder -> {
            builder.completed(completed).task(info);
            if (body != null) {
                builder.response(JsonData.fromJson(body));
            }
            if (failed) {
                builder.error(error -> error.type("reindex_failed").reason("fallo"));
            }
            return builder;
        });
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run() throws Exception;
    }

    /** Cada lectura del reloj avanza un paso: así vence el plazo sin esperar de verdad. */
    private static final class SteppingClock extends Clock {

        private final Duration step;
        private Instant now = Instant.parse("2026-10-18T03:04:05Z");

        SteppingClock(Duration step) {
            this.step = step;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            now = now.plus(step);
            return now;
        }
    }
}
