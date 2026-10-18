package com.bigfito.elastiflix.ingest.job;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Qué trabajos se olvidan, para que la memoria de la JVM no crezca con cada archivo.
 *
 * <p>Se conservan los últimos {@value #KEEP_FINISHED} trabajos en estado final. Un trabajo
 * {@code valido} que nadie importa ni consulta en {@link #IDLE_VALID} se cancela, porque retiene la lista
 * de películas. Un trabajo {@code importando}, {@code recibido} o {@code validando} nunca se toca: su hilo
 * todavía lo usa.</p>
 */
final class JobRetention {

    /** Trabajos en estado final que se conservan. */
    static final int KEEP_FINISHED = 20;

    /** Tiempo sin actividad tras el cual un trabajo {@code valido} se cancela. */
    static final Duration IDLE_VALID = Duration.ofMinutes(30);

    private JobRetention() {
    }

    /**
     * Trabajos {@code valido} sin actividad desde hace más de {@link #IDLE_VALID}.
     *
     * @param jobs     todos los trabajos
     * @param activity última actividad de cada trabajo, por id
     * @param now      hora UTC
     * @return los que hay que cancelar
     */
    static List<ImportWork> idleValid(Collection<ImportWork> jobs, Map<String, Instant> activity, Instant now) {
        List<ImportWork> idle = new ArrayList<>();
        Instant limit = now.minus(IDLE_VALID);
        for (ImportWork work : jobs) {
            if (work.status() == JobStatus.VALID && lastActivity(work, activity).isBefore(limit)) {
                idle.add(work);
            }
        }
        return idle;
    }

    /**
     * Trabajos en estado final que sobran: los más viejos, por última actividad.
     *
     * @param jobs     todos los trabajos
     * @param activity última actividad de cada trabajo, por id
     * @return los que hay que olvidar; vacío si hay {@value #KEEP_FINISHED} o menos
     */
    static List<ImportWork> overflow(Collection<ImportWork> jobs, Map<String, Instant> activity) {
        List<ImportWork> finished = new ArrayList<>();
        for (ImportWork work : jobs) {
            if (work.status().terminal()) {
                finished.add(work);
            }
        }
        if (finished.size() <= KEEP_FINISHED) {
            return List.of();
        }
        finished.sort(Comparator.comparing(work -> lastActivity(work, activity)));
        return List.copyOf(finished.subList(0, finished.size() - KEEP_FINISHED));
    }

    private static Instant lastActivity(ImportWork work, Map<String, Instant> activity) {
        return activity.getOrDefault(work.id(), Instant.EPOCH);
    }
}
