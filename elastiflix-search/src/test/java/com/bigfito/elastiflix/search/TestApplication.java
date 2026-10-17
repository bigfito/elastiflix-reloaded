package com.bigfito.elastiflix.search;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * Arranque de Spring para las pruebas de elastiflix-search.
 *
 * <p>Escanea este módulo y elastiflix-core, sin depender de elastiflix-web.</p>
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ConfigurationPropertiesScan({"com.bigfito.elastiflix.search", "com.bigfito.elastiflix.core"})
@ComponentScan({"com.bigfito.elastiflix.search", "com.bigfito.elastiflix.core"})
public class TestApplication {
}
