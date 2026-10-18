package com.bigfito.elastiflix.web.search;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.SearchValidationException;
import com.bigfito.elastiflix.search.query.FullTextSettings;
import com.bigfito.elastiflix.search.query.KnnSettings;
import com.bigfito.elastiflix.search.query.MatchType;
import com.bigfito.elastiflix.search.query.SearchFilters;
import com.bigfito.elastiflix.search.query.SearchMode;
import com.bigfito.elastiflix.search.query.SearchQuery;
import com.bigfito.elastiflix.search.query.TextField;
import com.bigfito.elastiflix.search.query.TextOperator;
import com.bigfito.elastiflix.search.query.SearchQueryValidator;
import com.bigfito.elastiflix.search.query.VectorField;

import org.springframework.stereotype.Component;

/**
 * Traduce la URL a la consulta que entiende {@code SearchService}.
 */
@Component
public class SearchQueryMapper {

    private static final int BAD_NUMBER = -1;
    private static final int TOO_MANY_CANDIDATES = 10_001;

    /**
     * Convierte el formulario. Un número ilegible se deja inválido para que falle la validación.
     *
     * @param form parámetros de la URL
     * @return consulta lista para validar
     */
    public SearchQuery toQuery(SearchForm form) {
        SearchMode mode = mode(form.mode());
        SearchQuery query = new SearchQuery(
                form.query(),
                mode,
                textSettings(mode, form),
                knnSettings(mode, form),
                filters(form),
                integer(form.page(), 1),
                integer(form.pageSize(), BAD_NUMBER));
        new SearchQueryValidator().validate(query);
        return query;
    }

    private static SearchMode mode(String name) {
        return switch (name) {
            case "vectorial" -> SearchMode.VECTOR;
            case "hibrida" -> SearchMode.HYBRID;
            case "fulltext" -> SearchMode.FULL_TEXT;
            default -> throw new SearchValidationException(ErrorCode.MISSING_MODE_SETTINGS);
        };
    }

    private static FullTextSettings textSettings(SearchMode mode, SearchForm form) {
        if (mode == SearchMode.VECTOR) {
            return null;
        }
        MatchType type = "multi_match".equals(form.type()) ? MatchType.MULTI_MATCH : MatchType.MATCH;
        TextOperator operator = "and".equals(form.operator()) ? TextOperator.AND : TextOperator.OR;
        return new FullTextSettings(type, field(type, form.field()), operator);
    }

    private static TextField field(MatchType type, String name) {
        if (type == MatchType.MULTI_MATCH) {
            return null;
        }
        if ("original_title".equals(name)) {
            return TextField.ORIGINAL_TITLE;
        }
        return TextField.TITLE;
    }

    private static KnnSettings knnSettings(SearchMode mode, SearchForm form) {
        if (mode == SearchMode.FULL_TEXT) {
            return null;
        }
        return new KnnSettings(vector(form.vector()), integer(form.k(), BAD_NUMBER), candidates(form.candidates()));
    }

    private static VectorField vector(String name) {
        for (VectorField field : VectorField.values()) {
            if (field.fieldName().equals(name)) {
                return field;
            }
        }
        return VectorField.PLOT_256;
    }

    private static int candidates(String raw) {
        int value = integer(raw, TOO_MANY_CANDIDATES);
        if (value == BAD_NUMBER) {
            return TOO_MANY_CANDIDATES;
        }
        return value;
    }

    private static int integer(String raw, int fallback) {
        String text = raw == null ? "" : raw.trim();
        if (!text.matches("-?\\d+")) {
            return fallback;
        }
        return Integer.parseInt(text);
    }

    private static SearchFilters filters(SearchForm form) {
        return new SearchFilters(
                adults(form.adult()),
                form.genres(),
                form.languages(),
                form.ratings(),
                date(form.from()),
                date(form.to()));
    }

    private static List<Boolean> adults(List<String> values) {
        List<Boolean> adults = new ArrayList<>();
        for (String value : values) {
            addAdult(adults, value);
        }
        return adults;
    }

    private static void addAdult(List<Boolean> adults, String value) {
        if ("true".equals(value)) {
            adults.add(Boolean.TRUE);
        }
        if ("false".equals(value)) {
            adults.add(Boolean.FALSE);
        }
    }

    private static LocalDate date(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
