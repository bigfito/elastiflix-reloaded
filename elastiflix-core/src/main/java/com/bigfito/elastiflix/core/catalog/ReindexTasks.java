package com.bigfito.elastiflix.core.catalog;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import com.bigfito.elastiflix.core.error.ElasticsearchAccessException;
import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.ImportException;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.ReindexResponse;
import co.elastic.clients.elasticsearch.tasks.GetTasksResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpMappingException;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copia un índice con {@code _reindex} y espera la tarea en {@code _tasks}.
 *
 * <p>La copia pide {@code wait_for_completion: false}. Después lee {@code _tasks} en un bucle: cada
 * lectura espera en el servidor hasta 20 s, por debajo del socket-timeout de 30 s del cliente, así que
 * la espera del servidor hace de pausa y este código no duerme. El plazo total es de 30 minutos.</p>
 *
 * <p>Si vence el plazo, se corta la conexión, llega una respuesta ilegible o se pide parar, primero cancela
 * la tarea y después lanza. Así quien borre el índice de destino no lo hace mientras el reindex sigue
 * escribiendo en él. La cancelación no espera en la misma llamada (podría pasar del socket-timeout): pide
 * {@code wait_for_completion=false} y confirma con hasta {@value #CANCEL_READS} lecturas de {@code _tasks}
 * de 20 s.</p>
 */
final class ReindexTasks {

    /** Plazo total de la copia. */
    static final Duration TOTAL_LIMIT = Duration.ofMinutes(30);

    /** Espera de cada lectura de {@code _tasks}; menor que el socket-timeout del cliente. */
    static final String READ_LIMIT = "20s";

    /** Lecturas de {@code _tasks} que confirman la cancelación antes de rendirse con un aviso. */
    static final int CANCEL_READS = 3;

    private static final String TIMEOUT_TYPE = "timeout_exception";
    private static final Logger LOG = LoggerFactory.getLogger(ReindexTasks.class);

    private final ElasticsearchClient client;
    private final Clock clock;

    ReindexTasks(ElasticsearchClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    /**
     * Copia {@code source} en {@code target}, vectores incluidos, y espera a que termine.
     *
     * @param source índice de origen
     * @param target índice nuevo, ya creado
     * @param stop   se consulta antes de cada lectura; si dice sí, cancela y lanza {@code EFX-IMP-003}
     * @throws IOException si el cliente no puede hablar con Elasticsearch (la tarea ya se canceló)
     */
    void copy(String source, String target, BooleanSupplier stop) throws IOException {
        String taskId = start(source, target);
        LOG.info("Reindex de {} hacia {} en la tarea {}", source, target, taskId);
        GetTasksResponse finished;
        try {
            finished = waitFor(taskId, stop);
        } catch (IOException | ElasticsearchException | JsonpMappingException exception) {
            cancel(taskId);
            throw exception;
        }
        requireClean(finished);
    }

    private String start(String source, String target) throws IOException {
        ReindexResponse started = client.reindex(request -> request
                .source(origin -> origin.index(source).sourceFields(CatalogRequests.withVectors()))
                .dest(destination -> destination.index(target))
                .waitForCompletion(false));
        String taskId = started.task();
        if (taskId == null || taskId.isBlank()) {
            LOG.error("{}: el reindex hacia {} no devolvió id de tarea", ErrorCode.REINDEX_FAILED.code(), target);
            throw new ElasticsearchAccessException(ErrorCode.REINDEX_FAILED);
        }
        return taskId;
    }

    private GetTasksResponse waitFor(String taskId, BooleanSupplier stop) throws IOException {
        Instant deadline = clock.instant().plus(TOTAL_LIMIT);
        Optional<GetTasksResponse> finished = Optional.empty();
        while (finished.isEmpty()) {
            requireTimeLeft(taskId, stop, deadline);
            finished = read(taskId).filter(GetTasksResponse::completed);
        }
        return finished.get();
    }

    private void requireTimeLeft(String taskId, BooleanSupplier stop, Instant deadline) {
        if (stop.getAsBoolean()) {
            cancel(taskId);
            throw new ImportException(ErrorCode.IMPORT_CANCELLED);
        }
        if (!clock.instant().isBefore(deadline)) {
            cancel(taskId);
            LOG.error("{}: la tarea {} no terminó en {}", ErrorCode.REINDEX_FAILED.code(), taskId, TOTAL_LIMIT);
            throw new ElasticsearchAccessException(ErrorCode.REINDEX_FAILED);
        }
    }

    /** Una lectura que agota su espera responde {@code timeout_exception}: la tarea sigue corriendo. */
    private Optional<GetTasksResponse> read(String taskId) throws IOException {
        try {
            return Optional.of(client.tasks().get(request -> request
                    .taskId(taskId)
                    .waitForCompletion(true)
                    .timeout(limit -> limit.time(READ_LIMIT))));
        } catch (ElasticsearchException exception) {
            if (exception.error() != null && TIMEOUT_TYPE.equals(exception.error().type())) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    /**
     * Pide cancelar la tarea sin esperar en la misma llamada y confirma que paró con lecturas cortas de
     * {@code _tasks}. Es un intento: si falla o no se confirma, avisa con el id de la tarea y sigue.
     */
    private void cancel(String taskId) {
        try {
            client.tasks().cancel(request -> request.taskId(taskId).waitForCompletion(false));
            if (confirmStopped(taskId)) {
                LOG.info("Se canceló la tarea de reindex {}", taskId);
            } else {
                LOG.warn("No se confirmó la cancelación de la tarea de reindex {} tras {} lecturas", taskId,
                        CANCEL_READS);
            }
        } catch (IOException | ElasticsearchException | JsonpMappingException exception) {
            LOG.warn("No se pudo cancelar la tarea de reindex {} tipo={}", taskId,
                    exception.getClass().getSimpleName());
        }
    }

    private boolean confirmStopped(String taskId) throws IOException {
        for (int attempt = 1; attempt <= CANCEL_READS; attempt++) {
            if (read(taskId).filter(GetTasksResponse::completed).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static void requireClean(GetTasksResponse task) {
        if (task.error() != null || hasFailures(task.response())) {
            LOG.error("{}: la tarea de reindex terminó con error, fallas o timed_out", ErrorCode.REINDEX_FAILED.code());
            throw new ElasticsearchAccessException(ErrorCode.REINDEX_FAILED);
        }
    }

    private static boolean hasFailures(JsonData response) {
        if (response == null || response.toJson().getValueType() != JsonValue.ValueType.OBJECT) {
            return false;
        }
        JsonObject body = response.toJson().asJsonObject();
        boolean timedOut = body.getBoolean("timed_out", false);
        JsonValue failures = body.get("failures");
        boolean failed = failures != null && failures.getValueType() == JsonValue.ValueType.ARRAY
                && !failures.asJsonArray().isEmpty();
        return timedOut || failed;
    }
}
