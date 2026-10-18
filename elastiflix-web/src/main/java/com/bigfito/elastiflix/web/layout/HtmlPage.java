package com.bigfito.elastiflix.web.layout;

/**
 * Marca un controlador que devuelve HTML.
 *
 * <p>El cromo y la página de error solo se aplican a estas clases. Los endpoints JSON de la
 * carga no lo son: si no, cada consulta de estado iría dos veces a Elasticsearch.</p>
 */
public interface HtmlPage {
}
