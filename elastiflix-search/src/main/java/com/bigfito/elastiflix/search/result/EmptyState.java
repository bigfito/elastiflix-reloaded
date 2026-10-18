package com.bigfito.elastiflix.search.result;

import java.util.List;

import com.bigfito.elastiflix.search.query.SearchMode;

/**
 * Texto del estado vacío. Es {@code null} en {@link SearchResult} cuando hay resultados.
 *
 * @param title título
 * @param text  explicación, con la consulta entre comillas latinas
 * @param tips  consejos del mockup
 */
public record EmptyState(String title, String text, List<String> tips) {

    private static final String NONE_TITLE = "No encontramos películas";
    private static final String FILTERED_TITLE = "Ningún resultado cumple los filtros";
    private static final String FILTERED_TEXT =
            "La búsqueda sí encontró películas, pero ninguna cumple a la vez todos los filtros aplicados.";
    private static final List<String> FULL_TEXT_TIPS = List.of(
            "Con AND deben aparecer todas las palabras; prueba con OR.",
            "Con match se busca en un solo campo; prueba multi-match.",
            "Revisa la ortografía y los apóstrofos (\u2019 y ').");
    private static final List<String> OTHER_TIPS = List.of(
            "Prueba a describir la trama con otras palabras.",
            "Cambia de modo o de ajustes de búsqueda.");
    private static final List<String> FILTER_TIPS = List.of(
            "Desmarca algún valor o quita un filtro y pulsa APLICAR.",
            "Amplía el rango de fechas de estreno.");

    /**
     * Copia los consejos.
     */
    public EmptyState {
        tips = List.copyOf(tips);
    }

    /**
     * La consulta no encontró películas, ni siquiera sin filtros.
     *
     * @param query texto de la consulta
     * @param mode  modo, para elegir los consejos
     * @return el estado «No encontramos películas»
     */
    public static EmptyState none(String query, SearchMode mode) {
        List<String> tips = FULL_TEXT_TIPS;
        if (mode != SearchMode.FULL_TEXT) {
            tips = OTHER_TIPS;
        }
        String text = "La consulta «" + query + "» no devolvió resultados con los ajustes actuales.";
        return new EmptyState(NONE_TITLE, text, tips);
    }

    /**
     * Había películas, pero los filtros las dejaron fuera.
     *
     * @return el estado «Ningún resultado cumple los filtros»
     */
    public static EmptyState filtered() {
        return new EmptyState(FILTERED_TITLE, FILTERED_TEXT, FILTER_TIPS);
    }
}
