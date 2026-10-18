package com.bigfito.elastiflix.web.search;

/**
 * Una puntuación de la tarjeta.
 *
 * @param label    BM25, similitud o RRF
 * @param value    número, o un guion si no hay
 * @param position puesto en su lista; vacío fuera de la híbrida
 */
public record ScoreLine(String label, String value, String position) {
}
