package com.bigfito.elastiflix.web.ingest;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;

import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * Alias, tamaño máximo y lote configurados, tal como los muestra «Cargar datos».
 *
 * <p>Así la página y el navegador usan {@code ELASTIFLIX_INGEST_MAX_FILE_SIZE} y
 * {@code ELASTIFLIX_INGEST_BATCH_SIZE} en lugar de 150 MB y 100 escritos a mano.</p>
 */
@Component
public class IngestLimits {

    private final ElastiflixProperties properties;

    /**
     * Usa la configuración del contexto. Una prueba {@code @WebMvcTest} la declara con
     * {@code @EnableConfigurationProperties(ElastiflixProperties.class)}.
     *
     * @param properties configuración de {@code elastiflix.*}
     */
    public IngestLimits(ElastiflixProperties properties) {
        this.properties = properties;
    }

    /**
     * Alias del catálogo.
     *
     * @return por ejemplo {@code elastiflix-reloaded}
     */
    public String alias() {
        return properties.index().alias();
    }

    /**
     * Tamaño máximo del archivo.
     *
     * @return el valor configurado
     */
    public DataSize maxFileSize() {
        return properties.ingest().maxFileSize();
    }

    /**
     * Tamaño máximo para los textos de la página.
     *
     * @return por ejemplo {@code 150 MB}
     */
    public String maxFileLabel() {
        return maxFileSize().toMegabytes() + " MB";
    }

    /**
     * Películas por lote de escritura.
     *
     * @return por ejemplo {@code 100}
     */
    public int batchSize() {
        return properties.ingest().batchSize();
    }
}
