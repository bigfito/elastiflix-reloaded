package com.bigfito.elastiflix.web.home;

import java.util.List;

/**
 * Sugerencias de full-text. Los conteos OR y AND se midieron el 2026-10-21 sobre 6 899 películas; son los
 * mismos en los dos idiomas, igual que las consultas. Las etiquetas salen del catálogo de mensajes.
 */
final class LexicalSuggestions {

    private static final String OR = "or";
    private static final String AND = "and";

    private LexicalSuggestions() {
    }

    /**
     * Los seis conjuntos: match sobre cada campo y multi-match, en OR y en AND.
     *
     * @param tags etiquetas en el idioma de la persona
     * @return conjuntos, el primero visible
     */
    static List<SuggestionSet> all(SuggestionTags tags) {
        return List.of(
                title(OR, true, tags),
                title(AND, false, tags),
                original(OR, tags),
                original(AND, tags),
                multi(OR, tags),
                multi(AND, tags));
    }

    private static SuggestionSet title(String operator, boolean visible, SuggestionTags tags) {
        return new SuggestionSet("fulltext-match-title-" + operator, visible, List.of(
                matchTitle("Spirited Away", tags.fixed("translatedTitle"), "🐉", operator),
                matchTitle("dark knight", tags.count(operator, "57", "5"), "🦇", operator),
                matchTitle("love story", tags.count(operator, "107", "3"), "💘", operator)));
    }

    private static SuggestionSet original(String operator, SuggestionTags tags) {
        String inTitle = AND.equals(operator) ? "0" : "3";
        return new SuggestionSet("fulltext-match-original_title-" + operator, false, List.of(
                matchOriginal("Kolejne 365 dni", tags.originalTitle(inTitle), "📅", operator),
                matchOriginal("el secreto", tags.count(operator, "35", "2"), "🔍", operator),
                matchOriginal("le fabuleux destin", tags.count(operator, "26", "1"), "🥐", operator)));
    }

    private static SuggestionSet multi(String operator, SuggestionTags tags) {
        return new SuggestionSet("fulltext-multi_match-" + operator, false, List.of(
                multiMatch("heaven cielo", tags.multi(operator, "10"), "☁️", operator),
                multiMatch("next kolejne", tags.multi(operator, "15"), "⏭️", operator),
                multiMatch("parasite 기생충", tags.multi(operator, "1"), "🏠", operator)));
    }

    private static SuggestionLink matchTitle(String text, String tag, String emoji, String operator) {
        return new SuggestionLink(text, tag, emoji, SearchHref.fullText(text, "match", "title", operator));
    }

    private static SuggestionLink matchOriginal(String text, String tag, String emoji, String operator) {
        return new SuggestionLink(text, tag, emoji, SearchHref.fullText(text, "match", "original_title", operator));
    }

    private static SuggestionLink multiMatch(String text, String tag, String emoji, String operator) {
        return new SuggestionLink(text, tag, emoji, SearchHref.fullText(text, "multi_match", "", operator));
    }
}
