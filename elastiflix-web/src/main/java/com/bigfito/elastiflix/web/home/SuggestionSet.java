package com.bigfito.elastiflix.web.home;

import java.util.List;

/**
 * Sugerencias de una combinación de modo y ajustes.
 *
 * @param key     clave que el JavaScript usa para mostrar u ocultar
 * @param visible si es el conjunto inicial (full-text, match, title, OR)
 * @param links   consultas de ese conjunto
 */
public record SuggestionSet(String key, boolean visible, List<SuggestionLink> links) {

    /**
     * Copia la lista.
     */
    public SuggestionSet {
        links = List.copyOf(links);
    }
}
