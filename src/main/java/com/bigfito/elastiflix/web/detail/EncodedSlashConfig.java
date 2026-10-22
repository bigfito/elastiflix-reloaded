package com.bigfito.elastiflix.web.detail;

import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Deja pasar una barra codificada ({@code %2F}) dentro de un segmento de la ruta, como en {@code /pelicula/a%2Fb}.
 *
 * <p>El registro individual acepta cualquier {@code id} de texto, y el enlace al detalle lo codifica como un solo
 * segmento. Tomcat rechaza {@code %2F} por defecto con un 400; con {@code passthrough} la deja codificada y Spring
 * la entrega decodificada en la variable {@code {id}}, sin partir la ruta.</p>
 */
@Configuration
public class EncodedSlashConfig {

    private static final String PASS_THROUGH = "passthrough";

    /**
     * Configura el conector de Tomcat.
     *
     * @return el ajuste de la fábrica del servidor
     */
    @Bean
    public WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> encodedSlashPassThrough() {
        return factory -> factory.addConnectorCustomizers(
                connector -> connector.setEncodedSolidusHandling(PASS_THROUGH));
    }
}
