package com.bigfito.elastiflix.web.layout;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;

/**
 * Arma la cabecera y el pie a partir del estado del catálogo.
 */
public final class PageShells {

    /**
     * Atributo de la petición donde {@link LayoutAdvice} deja el cromo ya armado.
     *
     * <p>Así la página de error de la misma petición no vuelve a leer el catálogo: con Elasticsearch caído,
     * serían dos rondas de timeouts.</p>
     */
    public static final String REQUEST_ATTRIBUTE = PageShell.class.getName();

    private PageShells() {
    }

    /**
     * Traduce el estado a los textos que ven las plantillas.
     *
     * @param status alias, conteo y si hubo conexión
     * @return el cromo de la página
     */
    public static PageShell from(CatalogStatus status) {
        String connection = CatalogTexts.CONNECTED;
        if (!status.connected()) {
            connection = CatalogTexts.disconnected(status.errorCode());
        }
        String index = CatalogTexts.indexLine(status.name(), status.documentCount());
        String phrase = CatalogTexts.moviePhrase(status.documentCount());
        return new PageShell(
                connection, index, status.connected(), CatalogTexts.TMDB, CatalogTexts.TMDB_ES, status.name(), phrase);
    }
}
