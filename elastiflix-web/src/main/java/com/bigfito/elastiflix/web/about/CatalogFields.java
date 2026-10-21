package com.bigfito.elastiflix.web.about;

import java.util.List;

/**
 * Todos los campos del mapping {@code elastiflix-reloaded}, como los describe «Acerca de» › «El catálogo»
 * (Requerimiento 5).
 *
 * <p>Los grupos son los del formulario de «Cargar datos»; los vectores van con la sinopsis, de donde salen. Cada
 * subcampo de un objeto anidado tiene su fila. {@code CatalogFieldsTest} compara esta lista con
 * {@code elasticsearch/elastiflix-reloaded-mapping.json}: si el mapping cambia, la prueba lo dice.</p>
 */
public final class CatalogFields {

    private static final String KEYWORD = "keyword";
    private static final String TEXT = "text";
    private static final String FLOAT = "float";
    private static final String INTEGER = "integer";
    private static final String LONG = "long";
    private static final String BOOLEAN = "boolean";
    private static final String NESTED = "nested";
    private static final String VECTOR = "dense_vector";

    private static final String DETAIL = "detail";
    private static final String CARD = "card";
    private static final String FILTER = "filter";
    private static final String FILTER_CARD = "filterCard";
    private static final String LEXICAL = "lexical";
    private static final String VECTORS = "vector";

    /** Los cinco grupos, en el orden de la página. */
    public static final List<CatalogGroup> GROUPS = List.of(
            group("ingest.single.section.id",
                    field("id", KEYWORD, DETAIL),
                    field("imdb_id", KEYWORD, DETAIL),
                    field("title", TEXT, LEXICAL),
                    field("original_title", TEXT, LEXICAL),
                    field("original_language", KEYWORD, DETAIL)),
            group("ingest.single.section.plot",
                    field("plot", TEXT, "plot"),
                    field("overview", TEXT, DETAIL),
                    field("tagline", KEYWORD, DETAIL),
                    field("plot_256", VECTOR, VECTORS),
                    field("plot_512", VECTOR, VECTORS),
                    field("plot_1024", VECTOR, VECTORS)),
            group("ingest.single.section.rating",
                    field("genres", KEYWORD, FILTER_CARD),
                    field("keywords", NESTED, DETAIL),
                    field("keywords.id", LONG, DETAIL),
                    field("keywords.name", TEXT, DETAIL),
                    field("rating", KEYWORD, FILTER),
                    field("release_date", "date", FILTER_CARD),
                    field("runtime", INTEGER, CARD),
                    field("status", KEYWORD, DETAIL),
                    field("adult", BOOLEAN, FILTER),
                    field("video", BOOLEAN, DETAIL)),
            group("ingest.single.section.metrics",
                    field("popularity", FLOAT, DETAIL),
                    field("vote_average", FLOAT, CARD),
                    field("vote_count", INTEGER, CARD),
                    field("user_score", FLOAT, DETAIL),
                    field("budget", FLOAT, DETAIL),
                    field("revenue", FLOAT, DETAIL)),
            group("ingest.single.section.cast",
                    field("cast", KEYWORD, DETAIL),
                    field("production_companies", KEYWORD, DETAIL),
                    field("production_countries", KEYWORD, DETAIL),
                    field("spoken_languages", KEYWORD, FILTER),
                    field("poster_path", TEXT, CARD),
                    field("backdrop_path", KEYWORD, DETAIL),
                    field("homepage", KEYWORD, DETAIL),
                    field("belongs_to_collection", NESTED, DETAIL),
                    field("belongs_to_collection.id", LONG, DETAIL),
                    field("belongs_to_collection.name", TEXT, DETAIL),
                    field("belongs_to_collection.poster_path", TEXT, DETAIL),
                    field("belongs_to_collection.backdrop_path", TEXT, DETAIL)));

    private CatalogFields() {
    }

    private static CatalogGroup group(String titleKey, CatalogField... fields) {
        return new CatalogGroup(titleKey, List.of(fields));
    }

    private static CatalogField field(String name, String type, String use) {
        return new CatalogField(name, type, "about.catalog.f." + name, "about.catalog.use." + use);
    }
}
