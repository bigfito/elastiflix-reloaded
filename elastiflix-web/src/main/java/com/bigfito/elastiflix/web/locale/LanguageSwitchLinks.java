package com.bigfito.elastiflix.web.locale;

import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import com.bigfito.elastiflix.web.EncodedQuery;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * Construye los enlaces {@code ES} y {@code EN} del selector de idioma conservando la ruta y los parámetros actuales.
 */
public final class LanguageSwitchLinks {

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
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(pathOf(request));
        appendParameters(builder, request);
        builder.queryParam(SupportedLocale.PARAM_NAME, lang);
        return EncodedQuery.of(builder);
    }

    private static String pathOf(HttpServletRequest request) {
        if (request == null) {
            return "/";
        }
        String candidate = candidatePath(request);
        if (candidate == null || candidate.isBlank() || "/error".equals(candidate)) {
            return "/";
        }
        return candidate;
    }

    private static String candidatePath(HttpServletRequest request) {
        Object forwarded = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (forwarded instanceof String uri && !uri.isBlank() && !"/error".equals(uri)) {
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
