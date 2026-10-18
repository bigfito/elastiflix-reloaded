package com.bigfito.elastiflix.web.home;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Todas las sugerencias del inicio, con las etiquetas fijas del catálogo de 4 322 películas.
 */
@Component
public class SuggestionCatalog {

    /**
     * Full-text, vectorial e híbrida. El primer conjunto es el visible.
     *
     * @return conjuntos que la plantilla pinta todos, aunque oculte los demás
     */
    public List<SuggestionSet> all() {
        List<SuggestionSet> sets = new ArrayList<>();
        sets.addAll(LexicalSuggestions.all());
        sets.addAll(VectorSuggestions.all());
        sets.addAll(HybridSuggestions.all());
        return List.copyOf(sets);
    }
}
