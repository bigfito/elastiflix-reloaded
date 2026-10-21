package com.bigfito.elastiflix.web.home;

import java.util.List;
import java.util.Locale;

import com.bigfito.elastiflix.web.locale.SupportedLocale;

/**
 * Sugerencias de full-text. Los conteos OR y AND se midieron el 2026-10-21 sobre 6 899 películas; son los
 * mismos en los dos idiomas.
 */
final class LexicalSuggestions {

    private static final String OR = "or";
    private static final String AND = "and";

    private LexicalSuggestions() {
    }

    /**
     * Los seis conjuntos: match sobre cada campo y multi-match, en OR y en AND.
     *
     * @param locale idioma destino
     * @return conjuntos, el primero visible
     */
    static List<SuggestionSet> all(Locale locale) {
        boolean en = SupportedLocale.isEnglish(locale);
        return List.of(
                title(OR, true, en),
                title(AND, false, en),
                original(OR, en),
                original(AND, en),
                multi(OR, en),
                multi(AND, en));
    }

    private static SuggestionSet title(String operator, boolean visible, boolean en) {
        String key = "fulltext-match-title-" + operator;
        String translatedTag = en ? "translated title" : "título traducido";
        String darkOr = en ? "OR: 57 results" : "OR: 57 resultados";
        String darkAnd = en ? "AND: 5 results" : "AND: 5 resultados";
        String loveOr = en ? "OR: 107 results" : "OR: 107 resultados";
        String loveAnd = en ? "AND: 3 results" : "AND: 3 resultados";
        return new SuggestionSet(key, visible, List.of(
                matchTitle("Spirited Away", translatedTag, "🐉", operator),
                matchTitle("dark knight", byOp(operator, darkOr, darkAnd), "🦇", operator),
                matchTitle("love story", byOp(operator, loveOr, loveAnd), "💘", operator)));
    }

    private static SuggestionSet original(String operator, boolean en) {
        String key = "fulltext-match-original_title-" + operator;
        String origPrefix = en ? "original title · in title: " : "título original · en title: ";
        String secOr = en ? "OR: 35 results" : "OR: 35 resultados";
        String secAnd = en ? "AND: 2 results" : "AND: 2 resultados";
        String fabOr = en ? "OR: 26 results" : "OR: 26 resultados";
        String fabAnd = en ? "AND: 1 result" : "AND: 1 resultado";
        return new SuggestionSet(key, false, List.of(
                matchOriginal("Kolejne 365 dni", origPrefix + byOp(operator, "3", "0"), "📅", operator),
                matchOriginal("el secreto", byOp(operator, secOr, secAnd), "🔍", operator),
                matchOriginal("le fabuleux destin", byOp(operator, fabOr, fabAnd), "🥐", operator)));
    }

    private static SuggestionSet multi(String operator, boolean en) {
        String both = en ? "words in different fields" : "palabras en campos distintos";
        String key = "fulltext-multi_match-" + operator;
        String heavenOr = en ? "OR: 10 results" : "OR: 10 resultados";
        String nextOr = en ? "OR: 15 results" : "OR: 15 resultados";
        String paraOr = en ? "OR: 1 result" : "OR: 1 resultado";
        return new SuggestionSet(key, false, List.of(
                multiMatch("heaven cielo", byOp(operator, heavenOr, "AND: 0 · " + both), "☁️", operator),
                multiMatch("next kolejne", byOp(operator, nextOr, "AND: 0 · " + both), "⏭️", operator),
                multiMatch("parasite 기생충", byOp(operator, paraOr, "AND: 0 · " + both), "🏠", operator)));
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
