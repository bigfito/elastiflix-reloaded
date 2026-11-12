package com.bigfito.elastiflix.web.about;

import java.util.List;
import java.util.Set;

/**
 * Todos los campos del mapping {@code elastiflix-reloaded}, como los describe «Acerca de» › «El catálogo».
 *
 * <p>Los grupos son los del formulario de «Cargar datos», más la procedencia; los vectores van con la sinopsis,
 * de donde salen. Cada
 * subcampo de un objeto anidado tiene su fila. {@code CatalogFieldsTest} compara esta lista con
 * {@code elasticsearch/elastiflix-reloaded-mapping.json}: si el mapping cambia, la prueba lo dice.</p>
 *
 * <p>Las columnas de obligatorios siguen las reglas del validador, las mismas en append y en bulk:
 * {@code id}, {@code title} y {@code plot} son obligatorios en los dos formatos, los vectores se ignoran si vienen y,
 * en CSV, los subcampos van dentro del JSON de la celda de su objeto. {@code CatalogRequirementsTest} las compara con
 * el validador real.</p>
 */
public final class CatalogFields {

    private static final String KEYWORD = "keyword";
    private static final String TEXT = "text";
    private static final String FLOAT = "float";
    private static final String INTEGER = "integer";
    private static final String LONG = "long";
    private static final String BOOLEAN = "boolean";
    private static final String NESTED = "nested";
    private static final String OBJECT = "object";
    private static final String VECTOR = "dense_vector";

    private static final String DETAIL = "detail";
    private static final String CARD = "card";
    private static final String FILTER = "filter";
    private static final String FILTER_CARD = "filterCard";
    private static final String LEXICAL = "lexical";
    private static final String VECTORS = "vector";
    private static final String PROVENANCE = "provenance";

    private static final Set<String> REQUIRED = Set.of("id", "title", "plot");
    private static final String REQ = "about.catalog.req.";

    /** Los seis grupos, en el orden de la página. */
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
                    field("belongs_to_collection.backdrop_path", TEXT, DETAIL)),
            group("about.catalog.group.enrichment",
                    field("enrichment", OBJECT, PROVENANCE),
                    field("enrichment.backdrop_path", KEYWORD, PROVENANCE),
                    field("enrichment.belongs_to_collection", KEYWORD, PROVENANCE),
                    field("enrichment.budget", KEYWORD, PROVENANCE),
                    field("enrichment.cast", KEYWORD, PROVENANCE),
                    field("enrichment.genres", KEYWORD, PROVENANCE),
                    field("enrichment.homepage", KEYWORD, PROVENANCE),
                    field("enrichment.imdb_id", KEYWORD, PROVENANCE),
                    field("enrichment.keywords", KEYWORD, PROVENANCE),
                    field("enrichment.overview", KEYWORD, PROVENANCE),
                    field("enrichment.plot", KEYWORD, PROVENANCE),
                    field("enrichment.poster_path", KEYWORD, PROVENANCE),
                    field("enrichment.production_companies", KEYWORD, PROVENANCE),
                    field("enrichment.production_countries", KEYWORD, PROVENANCE),
                    field("enrichment.rating", KEYWORD, PROVENANCE),
                    field("enrichment.revenue", KEYWORD, PROVENANCE),
                    field("enrichment.runtime", KEYWORD, PROVENANCE),
                    field("enrichment.spoken_languages", KEYWORD, PROVENANCE),
                    field("enrichment.tagline", KEYWORD, PROVENANCE),
                    field("enrichment.user_score", KEYWORD, PROVENANCE),
                    field("enrichment.vote_average", KEYWORD, PROVENANCE),
                    field("enrichment.vote_count", KEYWORD, PROVENANCE)));

    private CatalogFields() {
    }

    private static CatalogGroup group(String titleKey, CatalogField... fields) {
        return new CatalogGroup(titleKey, List.of(fields));
    }

    private static CatalogField field(String name, String type, String use) {
        return new CatalogField(name, type, "about.catalog.f." + name, "about.catalog.use." + use,
                REQ + requirement(name, type, false), REQ + requirement(name, type, true));
    }

    /**
     * Qué dice la celda de obligatorio: {@code yes}, {@code ignored} (vectores, que se generan), {@code inCell}
     * (subcampo de un objeto, en la celda JSON de un CSV) o {@code no}.
     */
    private static String requirement(String name, String type, boolean csv) {
        if (REQUIRED.contains(name)) {
            return "yes";
        }
        if (VECTOR.equals(type)) {
            return "ignored";
        }
        return csv && name.contains(".") ? "inCell" : "no";
    }
}
