package com.bigfito.elastiflix.core;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * Arranque de Spring para las pruebas de elastiflix-core.
 *
 * <p>Así {@code @SpringBootTest} no depende de elastiflix-web.</p>
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ConfigurationPropertiesScan("com.bigfito.elastiflix.core")
@ComponentScan("com.bigfito.elastiflix.core")
public class TestApplication {
}
