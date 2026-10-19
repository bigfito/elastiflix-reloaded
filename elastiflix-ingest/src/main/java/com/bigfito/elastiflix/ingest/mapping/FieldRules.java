package com.bigfito.elastiflix.ingest.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Dice la forma de cada uno de los 33 campos del catálogo.
 */
final class FieldRules {

    static final Set<String> COLLECTION = Set.of(
            "id", "name", "backdrop_path", "poster_path");
    static final Set<String> KEYWORD = Set.of("id", "name");

    private static final Map<String, FieldKind> KINDS = kinds();

    private FieldRules() {
    }

    static boolean known(String name) {
        return KINDS.containsKey(name);
    }

    static boolean required(String name) {
        return FieldNames.REQUIRED.contains(name);
    }

    static FieldKind kind(String name) {
        return KINDS.get(name);
    }

    private static Map<String, FieldKind> kinds() {
        Map<String, FieldKind> map = new HashMap<>();
        texts(map);
        numbers(map);
        groups(map);
        return Map.copyOf(map);
    }

    private static void texts(Map<String, FieldKind> map) {
        map.put("title", FieldKind.TEXT);
        map.put("original_title", FieldKind.TEXT);
        map.put("overview", FieldKind.TEXT);
        map.put("plot", FieldKind.TEXT);
        map.put("poster_path", FieldKind.TEXT);
        map.put(FieldNames.ID, FieldKind.KEYWORD);
        map.put("imdb_id", FieldKind.KEYWORD);
        map.put("original_language", FieldKind.KEYWORD);
        map.put("status", FieldKind.KEYWORD);
        map.put("tagline", FieldKind.KEYWORD);
        map.put("homepage", FieldKind.KEYWORD);
        map.put("backdrop_path", FieldKind.KEYWORD);
        map.put("rating", FieldKind.KEYWORD);
    }

    private static void numbers(Map<String, FieldKind> map) {
        map.put("adult", FieldKind.BOOLEAN);
        map.put("video", FieldKind.BOOLEAN);
        map.put("runtime", FieldKind.INTEGER);
        map.put("vote_count", FieldKind.INTEGER);
        map.put("budget", FieldKind.DECIMAL);
        map.put("revenue", FieldKind.DECIMAL);
        map.put("popularity", FieldKind.DECIMAL);
        map.put("vote_average", FieldKind.DECIMAL);
        map.put("user_score", FieldKind.DECIMAL);
        map.put("release_date", FieldKind.DATE);
    }

    private static void groups(Map<String, FieldKind> map) {
        map.put("genres", FieldKind.TEXT_LIST);
        map.put("cast", FieldKind.TEXT_LIST);
        map.put("production_companies", FieldKind.TEXT_LIST);
        map.put("production_countries", FieldKind.TEXT_LIST);
        map.put("spoken_languages", FieldKind.TEXT_LIST);
        map.put("belongs_to_collection", FieldKind.COLLECTION);
        map.put("keywords", FieldKind.KEYWORDS);
        for (String vector : VectorFields.NAMES) {
            map.put(vector, FieldKind.VECTOR);
        }
    }
}
