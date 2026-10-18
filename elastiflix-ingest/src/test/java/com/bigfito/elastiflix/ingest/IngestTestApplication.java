package com.bigfito.elastiflix.ingest;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * Arranque de Spring para las pruebas de elastiflix-ingest.
 *
 * <p>No usa elastiflix-web. El alias lo pone cada prueba de integración.</p>
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ConfigurationPropertiesScan("com.bigfito.elastiflix.core")
@ComponentScan({"com.bigfito.elastiflix.core", "com.bigfito.elastiflix.ingest"})
public class IngestTestApplication {
}
