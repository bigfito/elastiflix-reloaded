package com.bigfito.elastiflix.eval;

import java.util.Map;

/**
 * Una consulta del conjunto de evaluación y sus juicios graduados.
 *
 * <p>Es una línea de {@code eval/judgments.jsonl}. Los juicios van por {@code id} de película, no por índice:
 * el índice concreto cambia con cada carga bulk y el harness lo resuelve en cada corrida.</p>
 *
 * @param id      identificador estable, por ejemplo {@code es-plot-01}
 * @param lang    idioma de la consulta: {@code es} o {@code en}
 * @param kind    {@code plot} (describe la trama sin palabras del título) o {@code title} (busca por título)
 * @param text    texto tal como lo escribiría un usuario
 * @param target  película que la consulta busca; siempre tiene calificación 3
 * @param ratings calificación de 0 a 3 de cada película juzgada
 */
public record Judgment(
        String id, String lang, String kind, String text, String target, Map<String, Integer> ratings) {

    /** Calificación de la película buscada. */
    public static final int TARGET_RATING = 3;

    /**
     * Copia los juicios para que el registro no cambie si alguien modifica el mapa original.
     */
    public Judgment {
        ratings = Map.copyOf(ratings);
    }
}
