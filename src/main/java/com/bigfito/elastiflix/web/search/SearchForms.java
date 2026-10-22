package com.bigfito.elastiflix.web.search;

import java.util.List;

import org.springframework.util.MultiValueMap;

/**
 * Lee los parámetros de búsqueda de la URL.
 */
public final class SearchForms {

    private SearchForms() {
    }

    /**
     * Arma el formulario. Los que faltan quedan en el valor por defecto del mockup.
     *
     * @param params query string
     * @return formulario
     */
    public static SearchForm from(MultiValueMap<String, String> params) {
        return new SearchForm(
                text(params, "q", ""),
                text(params, "modo", "fulltext"),
                text(params, "tipo", "match"),
                text(params, "campo", "title"),
                text(params, "operador", "or"),
                text(params, "vector", "plot_256"),
                text(params, "k", "100"),
                text(params, "candidatos", "200"),
                text(params, "pagina", "1"),
                text(params, "tamano", "25"),
                list(params, "adult"),
                list(params, "genero"),
                list(params, "idioma"),
                list(params, "clasificacion"),
                text(params, "desde", ""),
                text(params, "hasta", ""));
    }

    private static String text(MultiValueMap<String, String> params, String name, String fallback) {
        String value = params.getFirst(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static List<String> list(MultiValueMap<String, String> params, String name) {
        List<String> values = params.get(name);
        if (values == null) {
            return List.of();
        }
        return values;
    }
}
