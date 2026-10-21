package com.bigfito.elastiflix.web.layout;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import com.bigfito.elastiflix.core.catalog.CatalogStatus;
import com.bigfito.elastiflix.web.locale.LanguageSwitchLinks;
import com.bigfito.elastiflix.web.locale.SupportedLocale;
import com.bigfito.elastiflix.web.locale.UiMessages;

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
     * Traduce el estado y los parámetros de la petición actual al cromo de la página en el idioma activo.
     *
     * @param status   alias, conteo y si hubo conexión
     * @param request  petición HTTP actual (puede ser {@code null})
     * @param messages catálogo de mensajes
     * @return el cromo de la página con enlaces {@code ES} y {@code EN}
     */
    public static PageShell from(CatalogStatus status, HttpServletRequest request, UiMessages messages) {
        Locale locale = SupportedLocale.current();
        String connection = CatalogTexts.connection(status.connected(), status.errorCode(), locale, messages);
        String index = CatalogTexts.indexLine(status.name(), status.documentCount(), locale, messages);
        String phrase = CatalogTexts.moviePhrase(status.documentCount(), locale, messages);
        String esHref = LanguageSwitchLinks.hrefFor(request, "es");
        String enHref = LanguageSwitchLinks.hrefFor(request, "en");
        return new PageShell(
                connection,
                index,
                status.connected(),
                CatalogTexts.TMDB,
                CatalogTexts.TMDB_ES,
                status.name(),
                phrase,
                locale.getLanguage(),
                esHref,
                enHref);
    }
}
