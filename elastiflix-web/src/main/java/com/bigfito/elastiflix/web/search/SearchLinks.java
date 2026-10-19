package com.bigfito.elastiflix.web.search;

import com.bigfito.elastiflix.web.EncodedQuery;

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

    /**
     * Enlace al detalle de la película conservando los parámetros de la búsqueda.
     *
     * @param id   identificador de la película
     * @param form formulario actual
     * @return ruta hacia el detalle
     */
    public static String detail(String id, SearchForm form) {
        if (form == null) {
            return "/pelicula/" + id;
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/pelicula/" + id);
        query(builder, form);
        filters(builder, form);
        builder.queryParam("pagina", form.page());
        builder.queryParam("tamano", form.pageSize());
        return EncodedQuery.of(builder);
    }

    /**
     * Enlace para volver a la búsqueda desde el detalle, apuntando al ancla de la tarjeta.
     *
     * @param id   identificador de la película
     * @param form formulario recuperado de la query string
     * @return ruta hacia /buscar?...#r-{id}
     */
    public static String returnToSearch(String id, SearchForm form) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/buscar");
        if (form != null) {
            query(builder, form);
            filters(builder, form);
            builder.queryParam("pagina", form.page());
            builder.queryParam("tamano", form.pageSize());
        }
        builder.fragment("r-" + id);
        return EncodedQuery.of(builder);
    }

    private static String build(SearchForm form, int page, String size, boolean clear) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/buscar");
        query(builder, form);
        if (!clear) {
            filters(builder, form);
        }
        builder.queryParam("pagina", page);
        builder.queryParam("tamano", size);
        return EncodedQuery.of(builder);
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
