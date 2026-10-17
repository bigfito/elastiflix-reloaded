package com.bigfito.elastiflix.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada de Elastiflix-Movies Java.
 */
@SpringBootApplication(scanBasePackages = "com.bigfito.elastiflix")
@ConfigurationPropertiesScan("com.bigfito.elastiflix")
public class ElastiflixApplication {

    /**
     * Arranca la aplicación.
     *
     * @param args argumentos de la línea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(ElastiflixApplication.class, args);
    }
}
