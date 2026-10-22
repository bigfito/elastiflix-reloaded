package com.bigfito.elastiflix.search.facet;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;

/**
 * Pasa las agregaciones de full-text a conteos y a fechas.
 */
final class AggregationFacets {

    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String ONE = "1";
    private static final String ZERO = "0";
    private static final String DAY = "\\d{4}-\\d{2}-\\d{2}";

    private AggregationFacets() {
    }

    /**
     * Cuenta los buckets de una agregación {@code terms}.
     *
     * @param aggregate agregación, o {@code null}
     * @return etiqueta y cantidad
     */
    static Map<String, Long> terms(Aggregate aggregate) {
        Map<String, Long> counts = Map.of();
        if (aggregate != null && aggregate.isSterms()) {
            counts = strings(aggregate.sterms());
        }
        if (aggregate != null && aggregate.isLterms()) {
            counts = longs(aggregate.lterms());
        }
        return counts;
    }

    /**
     * Igual que {@link #terms(Aggregate)}, con las claves {@code 1} y {@code 0} como
     * {@code true} y {@code false}.
     *
     * @param aggregate agregación de {@code adult}, o {@code null}
     * @return etiquetas del índice
     */
    static Map<String, Long> adult(Aggregate aggregate) {
        Map<String, Long> counts = new HashMap<>();
        for (Map.Entry<String, Long> entry : terms(aggregate).entrySet()) {
            put(counts, adultLabel(entry.getKey()), entry.getValue());
        }
        return counts;
    }

    /**
     * Lee el mínimo o el máximo de {@code release_date}.
     *
     * @param aggregate agregación {@code min} o {@code max}, o {@code null}
     * @return la fecha, o {@code null} si no hay
     */
    static LocalDate date(Aggregate aggregate) {
        if (aggregate != null && aggregate.isMin()) {
            return parse(aggregate.min().value(), aggregate.min().valueAsString());
        }
        if (aggregate != null && aggregate.isMax()) {
            return parse(aggregate.max().value(), aggregate.max().valueAsString());
        }
        return null;
    }

    private static Map<String, Long> strings(StringTermsAggregate aggregate) {
        Map<String, Long> counts = new HashMap<>();
        if (aggregate.buckets().isArray()) {
            for (StringTermsBucket bucket : aggregate.buckets().array()) {
                put(counts, bucket.key().stringValue(), bucket.docCount());
            }
        }
        return counts;
    }

    private static Map<String, Long> longs(LongTermsAggregate aggregate) {
        Map<String, Long> counts = new HashMap<>();
        if (aggregate.buckets().isArray()) {
            for (LongTermsBucket bucket : aggregate.buckets().array()) {
                put(counts, Long.toString(bucket.key()), bucket.docCount());
            }
        }
        return counts;
    }

    private static String adultLabel(String key) {
        if (TRUE.equals(key) || ONE.equals(key)) {
            return TRUE;
        }
        if (FALSE.equals(key) || ZERO.equals(key)) {
            return FALSE;
        }
        return key;
    }

    private static LocalDate parse(Double value, String asString) {
        if (asString != null && asString.matches(DAY)) {
            return LocalDate.parse(asString);
        }
        if (value == null || value.isNaN() || value.isInfinite()) {
            return null;
        }
        return Instant.ofEpochMilli(value.longValue()).atZone(ZoneOffset.UTC).toLocalDate();
    }

    private static void put(Map<String, Long> counts, String label, long count) {
        if (label != null && !label.isBlank()) {
            counts.merge(label, count, Long::sum);
        }
    }
}
