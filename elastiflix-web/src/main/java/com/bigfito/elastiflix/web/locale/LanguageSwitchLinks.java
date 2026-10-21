package com.bigfito.elastiflix.web.locale;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import com.bigfito.elastiflix.web.EncodedQuery;

import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * Construye los enlaces {@code ES} y {@code EN} del selector de idioma conservando la ruta y los parámetros actuales.
 */
public final class LanguageSwitchLinks {

    private static final String ROOT = "/";
    private static final String ERROR_PATH = "/error";

    private LanguageSwitchLinks() {
    }

    /**
     * Enlace de la página actual con {@code lang=es} o {@code lang=en} y todos los demás parámetros intactos.
     *
     * @param request petición HTTP actual
     * @param lang    código de idioma destino ({@code es} o {@code en})
     * @return ruta codificada lista para el atributo {@code href}
     */
    public static String hrefFor(HttpServletRequest request, String lang) {
        UriComponentsBuilder query = UriComponentsBuilder.newInstance();
        appendParameters(query, request);
        query.queryParam(SupportedLocale.PARAM_NAME, lang);
        return encodedPath(pathOf(request)) + EncodedQuery.of(query);
    }

    /**
     * Vuelve a codificar la ruta segmento por segmento, una sola vez.
     *
     * <p>{@code getRequestURI()} trae la ruta tal como la mandó el navegador: {@code /pelicula/caf%C3%A9}. Si se
     * pasa entera a {@code UriComponentsBuilder}, el {@code %} se codifica otra vez ({@code caf%25C3%25A9}) y el
     * enlace da 404. Por eso cada segmento se decodifica y se codifica completo (también {@code /} y {@code ;}):
     * un {@code %2F} dentro de un id sigue siendo parte del segmento y no se convierte en otra carpeta.</p>
     */
    private static String encodedPath(String rawPath) {
        List<String> encoded = new ArrayList<>();
        for (String segment : decodedSegments(rawPath)) {
            encoded.add(UriUtils.encode(segment, StandardCharsets.UTF_8));
        }
        return ROOT + String.join(ROOT, encoded);
    }

    private static List<String> decodedSegments(String rawPath) {
        List<String> segments = new ArrayList<>();
        try {
            for (String segment : rawPath.split(ROOT)) {
                addSegment(segments, UriUtils.decode(segment, StandardCharsets.UTF_8));
            }
        } catch (IllegalArgumentException malformed) {
            // Un «%» sin dos dígitos hexadecimales: no hay ruta que conservar, el selector lleva a Inicio.
            return List.of();
        }
        return segments;
    }

    private static void addSegment(List<String> segments, String segment) {
        if (!segment.isEmpty()) {
            segments.add(segment);
        }
    }

    private static String pathOf(HttpServletRequest request) {
        if (request == null) {
            return ROOT;
        }
        String candidate = candidatePath(request);
        if (candidate == null || candidate.isBlank() || ERROR_PATH.equals(candidate)) {
            return ROOT;
        }
        return candidate;
    }

    private static String candidatePath(HttpServletRequest request) {
        Object forwarded = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (forwarded instanceof String uri && !uri.isBlank() && !ERROR_PATH.equals(uri)) {
            return uri;
        }
        return request.getRequestURI();
    }

    private static void appendParameters(UriComponentsBuilder builder, HttpServletRequest request) {
        if (request == null) {
            return;
        }
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            appendEntry(builder, entry.getKey(), entry.getValue());
        }
    }

    private static void appendEntry(UriComponentsBuilder builder, String name, String[] values) {
        if (SupportedLocale.PARAM_NAME.equals(name) || values == null) {
            return;
        }
        for (String value : values) {
            builder.queryParam(name, value);
        }
    }
}
