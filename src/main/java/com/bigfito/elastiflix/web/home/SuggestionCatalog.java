package com.bigfito.elastiflix.web.home;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bigfito.elastiflix.web.locale.SupportedLocale;
import com.bigfito.elastiflix.web.locale.UiMessages;

import org.springframework.stereotype.Component;

/**
 * Todas las sugerencias del inicio según el idioma activo.
 */
@Component
public class SuggestionCatalog {

    private final UiMessages messages;

    /**
     * Crea el catálogo de sugerencias.
     *
     * @param messages etiquetas en los dos idiomas
     */
    public SuggestionCatalog(UiMessages messages) {
        this.messages = messages;
    }

    /**
     * Full-text, vectorial e híbrida en el idioma activo de la petición. El primer conjunto es el visible.
     *
     * @return conjuntos que la plantilla pinta todos, aunque oculte los demás
     */
    public List<SuggestionSet> all() {
        return all(SupportedLocale.current());
    }

    /**
     * Full-text, vectorial e híbrida en el idioma indicado. El primer conjunto es el visible.
     *
     * @param locale idioma destino ({@code es} o {@code en})
     * @return conjuntos que la plantilla pinta todos, aunque oculte los demás
     */
    public List<SuggestionSet> all(Locale locale) {
        SuggestionTags tags = new SuggestionTags(messages, SupportedLocale.normalize(locale));
        List<SuggestionSet> sets = new ArrayList<>();
        sets.addAll(LexicalSuggestions.all(tags));
        sets.addAll(VectorSuggestions.all(tags));
        sets.addAll(HybridSuggestions.all(tags));
        return List.copyOf(sets);
    }
}
