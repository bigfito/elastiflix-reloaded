package com.bigfito.elastiflix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada de Elastiflix-Movies Java.
 *
 * <p>Está en el paquete raíz para que Spring escanee sus cuatro partes: {@code core}, {@code search},
 * {@code ingest} y {@code web}.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
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
