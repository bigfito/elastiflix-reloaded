package com.bigfito.elastiflix.web.ingest;

import jakarta.servlet.MultipartConfigElement;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * Límites del multipart de Tomcat derivados del mismo tope que «Cargar datos».
 *
 * <p>Reemplaza {@code spring.servlet.multipart.max-file-size} y {@code max-request-size} del YAML: Spring
 * Boot no crea su propio {@link MultipartConfigElement} si ya hay uno. Así, subir
 * {@code ELASTIFLIX_INGEST_MAX_FILE_SIZE} sube también el tope de la petición, que antes era 151 MB escrito a
 * mano.</p>
 */
@Configuration
public class UploadMultipartConfig {

    /** Margen de la petición para los otros campos del formulario: separador y encabezado. */
    static final DataSize FORM_MARGIN = DataSize.ofMegabytes(1);

    /**
     * Tope del archivo y de la petición completa para el servlet de Spring MVC.
     *
     * @param limits tamaño máximo configurado del archivo
     * @return archivo hasta {@code maxFileSize}; petición hasta {@code maxFileSize} más 1 MB
     */
    @Bean
    public MultipartConfigElement multipartConfigElement(IngestLimits limits) {
        long maxFile = limits.maxFileSize().toBytes();
        return new MultipartConfigElement("", maxFile, maxFile + FORM_MARGIN.toBytes(), 0);
    }
}
