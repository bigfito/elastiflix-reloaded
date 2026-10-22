package com.bigfito.elastiflix.web.locale;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import com.bigfito.elastiflix.web.EncodedQuery;

import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * Construye los enlaces {@code ES} y {@code EN} del selector de idioma conservando la ruta y los parámetros de la
 * URL actual.
 *
 * <p>Solo copia los parámetros de la URL ({@code getQueryString()}), nunca los del cuerpo de un formulario. Si la
 * página es la respuesta de un {@code POST} (por ejemplo, un registro rechazado), esa ruta no se puede abrir con
 * {@code GET}: el selector lleva a la página que la contiene ({@code /cargar}) o a Inicio.</p>
 */
public final class LanguageSwitchLinks {

    private static final String ROOT = "/";
    private static final String ERROR_PATH = "/error";
    private static final String INGEST_PATH = "/cargar";
    private static final String PAIR_SEPARATOR = "&";
    private static final char VALUE_SEPARATOR = '=';
    private static final Pattern MALFORMED_ESCAPE = Pattern.compile("%(?![0-9A-Fa-f]{2})");

    private LanguageSwitchLinks() {
    }

    /**
     * Enlace de la página actual con {@code lang=es} o {@code lang=en} y los demás parámetros de la URL intactos.
     *
     * @param request petición HTTP actual (puede ser {@code null})
     * @param lang    código de idioma destino ({@code es} o {@code en})
     * @return ruta codificada lista para el atributo {@code href}
     */
    public static String hrefFor(HttpServletRequest request, String lang) {
        Origin origin = Origin.of(request);
        UriComponentsBuilder query = UriComponentsBuilder.newInstance();
        if (origin.isGet()) {
            appendQuery(query, origin.query());
        }
        query.queryParam(SupportedLocale.PARAM_NAME, lang);
        return encodedPath(target(origin)) + EncodedQuery.of(query);
    }

    private static String target(Origin origin) {
        String path = origin.path();
        if (origin.isGet()) {
            return path;
        }
        if (INGEST_PATH.equals(path) || path.startsWith(INGEST_PATH + ROOT)) {
            return INGEST_PATH;
        }
        return ROOT;
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
        if (isMalformed(rawPath)) {
            // No hay ruta que conservar: el selector lleva a Inicio.
            return segments;
        }
        for (String segment : rawPath.split(ROOT)) {
            if (!segment.isEmpty()) {
                segments.add(UriUtils.decode(segment, StandardCharsets.UTF_8));
            }
        }
        return segments;
    }

    /**
     * Un {@code %} sin dos dígitos hexadecimales detrás: ni el contenedor ni {@code URLDecoder} lo pueden leer.
     */
    private static boolean isMalformed(String raw) {
        return MALFORMED_ESCAPE.matcher(raw).find();
    }

    /**
     * Copia los pares de la URL, en su orden y con sus repeticiones, salvo {@code lang}.
     *
     * <p>Se decodifican como lo hace el contenedor ({@code +} es un espacio) y {@link EncodedQuery} los vuelve a
     * codificar. Un par con un escape mal formado se omite: el contenedor tampoco lo pudo leer.</p>
     */
    private static void appendQuery(UriComponentsBuilder query, String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return;
        }
        for (String pair : rawQuery.split(PAIR_SEPARATOR)) {
            appendPair(query, pair);
        }
    }

    private static void appendPair(UriComponentsBuilder query, String pair) {
        if (isMalformed(pair)) {
            // «%zz»: el contenedor descartó el parámetro (un 400) y el selector hace lo mismo.
            return;
        }
        int split = pair.indexOf(VALUE_SEPARATOR);
        String name = URLDecoder.decode(split < 0 ? pair : pair.substring(0, split), StandardCharsets.UTF_8);
        String value = split < 0 ? "" : URLDecoder.decode(pair.substring(split + 1), StandardCharsets.UTF_8);
        if (!name.isEmpty() && !SupportedLocale.PARAM_NAME.equals(name)) {
            query.queryParam(name, value);
        }
    }

    /**
     * Método, ruta y parámetros de la página que se está mostrando.
     *
     * <p>En el reenvío {@code /error} del contenedor, la página es la original: sus datos vienen en los atributos
     * {@code jakarta.servlet.error.*} y no en la petición, que ya apunta a {@code /error}.</p>
     *
     * @param method método HTTP
     * @param path   ruta codificada, nunca {@code /error}
     * @param query  parámetros codificados de la URL, o {@code null}
     */
    private record Origin(String method, String path, String query) {

        private static final String GET = "GET";
        private static final String HEAD = "HEAD";

        static Origin of(HttpServletRequest request) {
            if (request == null) {
                return new Origin(GET, ROOT, null);
            }
            if (request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) instanceof String uri && !uri.isBlank()) {
                return forwarded(request, uri);
            }
            return new Origin(request.getMethod(), shown(request.getRequestURI()), request.getQueryString());
        }

        private static Origin forwarded(HttpServletRequest request, String uri) {
            String method = stringOr(request.getAttribute(RequestDispatcher.ERROR_METHOD), request.getMethod());
            String query = stringOr(request.getAttribute(RequestDispatcher.ERROR_QUERY_STRING), null);
            return new Origin(method, shown(uri), query);
        }

        boolean isGet() {
            return GET.equalsIgnoreCase(method) || HEAD.equalsIgnoreCase(method);
        }

        private static String shown(String path) {
            if (path == null || path.isBlank() || ERROR_PATH.equals(path)) {
                return ROOT;
            }
            return path;
        }

        private static String stringOr(Object attribute, String fallback) {
            if (attribute instanceof String value) {
                return value;
            }
            return fallback;
        }
    }
}
