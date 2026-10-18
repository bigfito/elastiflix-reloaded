package com.bigfito.elastiflix.web.layout;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Arma la URL del póster con el CDN de TMDB.
 */
@Component
public class PosterUrls {

    private final String baseUrl;

    /**
     * Usa la propiedad {@code elastiflix.poster.base-url}, o el CDN w342 si no hay contexto.
     *
     * @param properties propiedades, si el contexto las creó
     */
    public PosterUrls(ObjectProvider<ElastiflixProperties> properties) {
        ElastiflixProperties found = properties.getIfAvailable();
        if (found == null) {
            this.baseUrl = ElastiflixProperties.Poster.fallback().baseUrl();
        } else {
            this.baseUrl = found.poster().baseUrl();
        }
    }

    /**
     * Une el prefijo y {@code poster_path}. Vacío si la película no tiene póster.
     *
     * @param posterPath ruta del índice, o {@code null}
     * @return URL, o cadena vacía
     */
    public String url(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) {
            return "";
        }
        return baseUrl + posterPath;
    }
}
