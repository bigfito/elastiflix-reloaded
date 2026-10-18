package com.bigfito.elastiflix.web.ingest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.model.Movie;
import com.bigfito.elastiflix.ingest.mapping.ValidationIssue;

import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Arma una película desde el formulario. Los vectores no se capturan: los genera la carga.
 */
@Component
public class MovieForms {

    private final JsonMapper json = JsonMapper.builder().build();

    /**
     * Lee los campos del formulario. Un texto vacío no se envía.
     *
     * <p>Un número que no cabe en el tipo del campo ({@code vote_count=1e3}, un {@code runtime} de 11 dígitos)
     * no tumba la página: vuelve como rechazo {@code EFX-ING-012} con el nombre del campo.</p>
     *
     * @param form parámetros de {@code POST /cargar/registro}
     * @return la película sin vectores, o el rechazo
     */
    public MovieForm read(MultiValueMap<String, String> form) {
        Map<String, Object> values = new LinkedHashMap<>();
        texts(values, form);
        numbers(values, form);
        lists(values, form);
        try {
            return new MovieForm.Read(json.convertValue(values, Movie.class));
        } catch (InvalidFormatException exception) {
            return new MovieForm.Invalid(rejection(exception, form.getFirst("id")));
        }
    }

    private static ValidationIssue rejection(InvalidFormatException exception, String id) {
        String property = exception.getPath().isEmpty() ? null : exception.getPath().getLast().getPropertyName();
        String field = snakeCase(property);
        return new ValidationIssue(ErrorCode.TYPE_MISMATCH.code(), "formulario", id == null ? "—" : id, field,
                String.valueOf(exception.getValue()), expected(exception.getTargetType()), false);
    }

    private static String expected(Class<?> type) {
        if (Integer.class.equals(type) || Long.class.equals(type)) {
            return "un número entero";
        }
        return "un número";
    }

    /** {@code voteCount} → {@code vote_count}, el nombre del campo en el formulario y en el mapping. */
    private static String snakeCase(String property) {
        return property == null ? "formulario" : property.replaceAll("([A-Z])", "_$1").toLowerCase(Locale.ROOT);
    }

    private static void texts(Map<String, Object> values, MultiValueMap<String, String> form) {
        put(values, "id", form.getFirst("id"));
        put(values, "imdbId", form.getFirst("imdb_id"));
        put(values, "title", form.getFirst("title"));
        put(values, "originalTitle", form.getFirst("original_title"));
        put(values, "originalLanguage", form.getFirst("original_language"));
        put(values, "releaseDate", form.getFirst("release_date"));
        put(values, "status", form.getFirst("status"));
        put(values, "tagline", form.getFirst("tagline"));
        put(values, "overview", form.getFirst("overview"));
        put(values, "plot", form.getFirst("plot"));
        put(values, "rating", form.getFirst("rating"));
        put(values, "homepage", form.getFirst("homepage"));
        put(values, "posterPath", form.getFirst("poster_path"));
        put(values, "backdropPath", form.getFirst("backdrop_path"));
        flags(values, form);
    }

    private static void flags(Map<String, Object> values, MultiValueMap<String, String> form) {
        if (form.containsKey("adult")) {
            values.put("adult", Boolean.TRUE);
        }
        if (form.containsKey("video")) {
            values.put("video", Boolean.TRUE);
        }
    }

    private static void numbers(Map<String, Object> values, MultiValueMap<String, String> form) {
        put(values, "runtime", form.getFirst("runtime"));
        put(values, "popularity", form.getFirst("popularity"));
        put(values, "voteAverage", form.getFirst("vote_average"));
        put(values, "voteCount", form.getFirst("vote_count"));
        put(values, "userScore", form.getFirst("user_score"));
        put(values, "budget", form.getFirst("budget"));
        put(values, "revenue", form.getFirst("revenue"));
    }

    private static void lists(Map<String, Object> values, MultiValueMap<String, String> form) {
        putList(values, "genres", form.getFirst("genres"));
        putList(values, "cast", form.getFirst("cast"));
        putList(values, "productionCompanies", form.getFirst("production_companies"));
        putList(values, "productionCountries", form.getFirst("production_countries"));
        putList(values, "spokenLanguages", form.getFirst("spoken_languages"));
        keywords(values, form.getFirst("keywords"));
        collection(values, form.getFirst("collection"));
    }

    private static void keywords(Map<String, Object> values, String raw) {
        List<Map<String, String>> keywords = new ArrayList<>();
        for (String name : split(raw)) {
            keywords.add(Map.of("name", name));
        }
        if (!keywords.isEmpty()) {
            values.put("keywords", keywords);
        }
    }

    private static void collection(Map<String, Object> values, String name) {
        if (name != null && !name.isBlank()) {
            values.put("belongsToCollection", Map.of("name", name.trim()));
        }
    }

    private static void putList(Map<String, Object> values, String key, String raw) {
        List<String> items = split(raw);
        if (!items.isEmpty()) {
            values.put(key, items);
        }
    }

    private static List<String> split(String raw) {
        List<String> items = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return items;
        }
        for (String part : raw.split(",")) {
            add(items, part);
        }
        return items;
    }

    private static void add(List<String> items, String part) {
        if (!part.isBlank()) {
            items.add(part.trim());
        }
    }

    private static void put(Map<String, Object> values, String key, String raw) {
        if (raw != null && !raw.isBlank()) {
            values.put(key, raw.trim());
        }
    }
}
