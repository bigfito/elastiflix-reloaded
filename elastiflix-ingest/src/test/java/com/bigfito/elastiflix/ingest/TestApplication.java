package com.bigfito.elastiflix.ingest;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * Arranque de Spring para las pruebas de elastiflix-ingest.
 *
 * <p>Escanea este módulo y elastiflix-core, sin depender de elastiflix-web.</p>
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ConfigurationPropertiesScan({"com.bigfito.elastiflix.ingest", "com.bigfito.elastiflix.core"})
@ComponentScan({"com.bigfito.elastiflix.ingest", "com.bigfito.elastiflix.core"})
public class TestApplication {
}
