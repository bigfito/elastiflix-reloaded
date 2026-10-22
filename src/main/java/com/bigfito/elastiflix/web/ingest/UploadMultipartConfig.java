package com.bigfito.elastiflix.web.ingest;

import jakarta.servlet.MultipartConfigElement;

import com.bigfito.elastiflix.ingest.file.FileChecker;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * Límites del multipart de Tomcat derivados del mismo tope que «Cargar datos».
 *
 * <p>Reemplaza {@code spring.servlet.multipart.max-file-size} y {@code max-request-size} del YAML: Spring
 * Boot no crea su propio {@link MultipartConfigElement} si ya hay uno. Los dos topes salen de
 * {@link FileChecker#MAX_FILE_SIZE}: el archivo hasta 25 MB y la petición hasta 26 MB.</p>
 */
@Configuration
public class UploadMultipartConfig {

    /** Margen de la petición para los otros campos del formulario: separador y encabezado. */
    static final DataSize FORM_MARGIN = DataSize.ofMegabytes(1);

    /**
     * Tope del archivo y de la petición completa para el servlet de Spring MVC.
     *
     * @return archivo hasta {@link FileChecker#MAX_FILE_SIZE}; petición hasta ese tope más 1 MB
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        long maxFile = FileChecker.MAX_FILE_SIZE.toBytes();
        return new MultipartConfigElement("", maxFile, maxFile + FORM_MARGIN.toBytes(), 0);
    }
}
