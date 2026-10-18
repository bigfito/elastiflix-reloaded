package com.bigfito.elastiflix.web.home;

import java.util.List;

/**
 * Sugerencias de full-text, con las etiquetas fijas de {@code Main.dc.html}.
 */
final class LexicalSuggestions {

    private static final String OR = "or";
    private static final String AND = "and";

    private LexicalSuggestions() {
    }

    /**
     * Los seis conjuntos: match sobre cada campo y multi-match, en OR y en AND.
     *
     * @return conjuntos, el primero visible
     */
    static List<SuggestionSet> all() {
        return List.of(
                title(OR, true),
                title(AND, false),
                original(OR),
                original(AND),
                multi(OR),
                multi(AND));
    }

    private static SuggestionSet title(String operator, boolean visible) {
        String key = "fulltext-match-title-" + operator;
        return new SuggestionSet(key, visible, List.of(
                matchTitle("Spirited Away", "título traducido", "🐉", operator),
                matchTitle("dark knight", byOp(operator, "OR: 40 resultados", "AND: 3 resultados"),
                        "🦇", operator),
                matchTitle("love story", byOp(operator, "OR: 65 resultados", "AND: 1 resultado"),
                        "💘", operator)));
    }

    private static SuggestionSet original(String operator) {
        String key = "fulltext-match-original_title-" + operator;
        return new SuggestionSet(key, false, List.of(
                matchOriginal("Kolejne 365 dni", "título original · en title: 0", "📅", operator),
                matchOriginal("el secreto", byOp(operator, "OR: 16 resultados", "AND: 1 resultado"),
                        "🔍", operator),
                matchOriginal("le fabuleux destin",
                        byOp(operator, "OR: 14 resultados", "AND: 1 resultado"), "🥐", operator)));
    }

    private static SuggestionSet multi(String operator) {
        String both = "palabras en campos distintos";
        String key = "fulltext-multi_match-" + operator;
        return new SuggestionSet(key, false, List.of(
                multiMatch("heaven cielo", byOp(operator, "OR: 6 resultados", "AND: 0 · " + both),
                        "☁️", operator),
                multiMatch("next kolejne", byOp(operator, "OR: 11 resultados", "AND: 0 · " + both),
                        "⏭️", operator),
                multiMatch("parasite 기생충", byOp(operator, "OR: 1 resultado", "AND: 0 · " + both),
                        "🏠", operator)));
    }

    private static SuggestionLink matchTitle(String text, String tag, String emoji, String operator) {
        return linked(text, tag, emoji, SearchHref.fullText(text, "match", "title", operator));
    }

    private static SuggestionLink matchOriginal(String text, String tag, String emoji, String operator) {
        return linked(text, tag, emoji, SearchHref.fullText(text, "match", "original_title", operator));
    }

    private static SuggestionLink multiMatch(String text, String tag, String emoji, String operator) {
        return linked(text, tag, emoji, SearchHref.fullText(text, "multi_match", "", operator));
    }

    private static SuggestionLink linked(String text, String tag, String emoji, String href) {
        return new SuggestionLink(text, tag, emoji, href);
    }

    private static String byOp(String operator, String orTag, String andTag) {
        if (AND.equals(operator)) {
            return andTag;
        }
        return orTag;
    }
}
