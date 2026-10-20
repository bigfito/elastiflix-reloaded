package com.bigfito.elastiflix.web.home;

import java.util.List;
import java.util.Locale;

import com.bigfito.elastiflix.web.locale.SupportedLocale;

/**
 * Sugerencias de full-text, con las etiquetas fijas de {@code Main.dc.html} y {@code Main-en.dc.html}.
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
        String darkOr = en ? "OR: 40 results" : "OR: 40 resultados";
        String darkAnd = en ? "AND: 3 results" : "AND: 3 resultados";
        String loveOr = en ? "OR: 65 results" : "OR: 65 resultados";
        String loveAnd = en ? "AND: 1 result" : "AND: 1 resultado";
        return new SuggestionSet(key, visible, List.of(
                matchTitle("Spirited Away", translatedTag, "🐉", operator),
                matchTitle("dark knight", byOp(operator, darkOr, darkAnd), "🦇", operator),
                matchTitle("love story", byOp(operator, loveOr, loveAnd), "💘", operator)));
    }

    private static SuggestionSet original(String operator, boolean en) {
        String key = "fulltext-match-original_title-" + operator;
        String origTag = en ? "original title · in title: 0" : "título original · en title: 0";
        String secOr = en ? "OR: 16 results" : "OR: 16 resultados";
        String fabOr = en ? "OR: 14 results" : "OR: 14 resultados";
        String oneAnd = en ? "AND: 1 result" : "AND: 1 resultado";
        return new SuggestionSet(key, false, List.of(
                matchOriginal("Kolejne 365 dni", origTag, "📅", operator),
                matchOriginal("el secreto", byOp(operator, secOr, oneAnd), "🔍", operator),
                matchOriginal("le fabuleux destin", byOp(operator, fabOr, oneAnd), "🥐", operator)));
    }

    private static SuggestionSet multi(String operator, boolean en) {
        String both = en ? "words in different fields" : "palabras en campos distintos";
        String key = "fulltext-multi_match-" + operator;
        String heavenOr = en ? "OR: 6 results" : "OR: 6 resultados";
        String nextOr = en ? "OR: 11 results" : "OR: 11 resultados";
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
