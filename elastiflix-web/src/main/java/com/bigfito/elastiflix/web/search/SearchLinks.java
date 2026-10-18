package com.bigfito.elastiflix.web.search;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * Enlaces de la página de resultados que conservan la consulta y los filtros.
 */
public final class SearchLinks {

    private SearchLinks() {
    }

    /**
     * La misma búsqueda en otra página.
     *
     * @param form formulario actual
     * @param page página desde 1
     * @return ruta
     */
    public static String page(SearchForm form, int page) {
        return build(form, page, form.pageSize(), false);
    }

    /**
     * Cambia el tamaño y vuelve a la página 1.
     *
     * @param form formulario actual
     * @param size 25, 50 o 100
     * @return ruta
     */
    public static String size(SearchForm form, int size) {
        return build(form, 1, Integer.toString(size), false);
    }

    /**
     * Quita los filtros y vuelve a la página 1.
     *
     * @param form formulario actual
     * @return ruta
     */
    public static String cleared(SearchForm form) {
        return build(form, 1, form.pageSize(), true);
    }

    private static String build(SearchForm form, int page, String size, boolean clear) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/buscar");
        query(builder, form);
        if (!clear) {
            filters(builder, form);
        }
        builder.queryParam("pagina", page);
        builder.queryParam("tamano", size);
        return builder.build().encode().toUriString();
    }

    private static void query(UriComponentsBuilder builder, SearchForm form) {
        builder.queryParam("q", form.query())
                .queryParam("modo", form.mode())
                .queryParam("tipo", form.type())
                .queryParam("campo", form.field())
                .queryParam("operador", form.operator())
                .queryParam("vector", form.vector())
                .queryParam("k", form.k())
                .queryParam("candidatos", form.candidates());
    }

    private static void filters(UriComponentsBuilder builder, SearchForm form) {
        form.adult().forEach(value -> builder.queryParam("adult", value));
        form.genres().forEach(value -> builder.queryParam("genero", value));
        form.languages().forEach(value -> builder.queryParam("idioma", value));
        form.ratings().forEach(value -> builder.queryParam("clasificacion", value));
        if (!form.from().isBlank()) {
            builder.queryParam("desde", form.from());
        }
        if (!form.to().isBlank()) {
            builder.queryParam("hasta", form.to());
        }
    }
}
