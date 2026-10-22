package com.bigfito.elastiflix.web.layout;

import java.util.regex.Pattern;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;

import org.springframework.stereotype.Component;

/**
 * Arma la URL de una imagen ({@code poster_path} o {@code backdrop_path}) con el CDN de TMDB.
 *
 * <p>Es el único lugar donde se compone esa URL: alimenta las tarjetas de Resultados y el detalle.</p>
 */
@Component
public class PosterUrls {

    /**
     * Forma de una ruta de TMDB, por ejemplo {@code /8IB2e4r4oVhHnANbnm7O3Tj6tF8.jpg}. Una ruta con comillas,
     * paréntesis, espacios u otra barra podría salir del {@code url('…')} del estilo del fondo y meter CSS.
     */
    private static final Pattern IMAGE_PATH = Pattern.compile("^/[\\w.-]+$");

    private final String baseUrl;

    /**
     * Usa la propiedad {@code elastiflix.poster.base-url}.
     *
     * @param properties configuración de {@code elastiflix.*}
     */
    public PosterUrls(ElastiflixProperties properties) {
        this.baseUrl = properties.poster().baseUrl();
    }

    /**
     * Une el prefijo y la ruta de la imagen. Vacío si no hay ruta o si no tiene la forma de una ruta de TMDB:
     * en los dos casos la página muestra el marcador de imagen faltante.
     *
     * @param imagePath ruta del índice, o {@code null}
     * @return URL, o cadena vacía
     */
    public String url(String imagePath) {
        if (imagePath == null || !IMAGE_PATH.matcher(imagePath).matches()) {
            return "";
        }
        return baseUrl + imagePath;
    }
}
