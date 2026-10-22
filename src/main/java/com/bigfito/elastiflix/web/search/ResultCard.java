package com.bigfito.elastiflix.web.search;

import java.util.List;

/**
 * Una película lista para la plantilla.
 *
 * @param id            identificador de la película
 * @param rank          posición en el resultado, desde 1
 * @param title         título
 * @param meta          título original, fecha, duración y géneros
 * @param plot          sinopsis
 * @param votes         calificación y número de votos
 * @param posterUrl     URL del CDN, o vacía si no hay póster
 * @param scores        puntuaciones del modo
 * @param rrf           puntuación RRF, o vacía fuera de la híbrida
 * @param detailHref    enlace a la página de detalle con la búsqueda preservada
 */
public record ResultCard(
        String id,
        int rank,
        String title,
        String meta,
        String plot,
        String votes,
        String posterUrl,
        List<ScoreLine> scores,
        String rrf,
        String detailHref) {

    /**
     * Copia las puntuaciones.
     */
    public ResultCard {
        scores = List.copyOf(scores);
    }
}
