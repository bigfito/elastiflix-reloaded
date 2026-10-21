package com.bigfito.elastiflix.web.ingest;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.ingest.file.FileChecker;

import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * Alias, tamaño máximo y lote, tal como los muestran «Cargar datos» y «Acerca de».
 *
 * <p>El tamaño máximo es la constante fija {@link FileChecker#MAX_FILE_SIZE} (25 MB, Requerimiento 3); el alias
 * y el lote son los configurados ({@code ELASTIFLIX_INDEX_ALIAS} y {@code ELASTIFLIX_INGEST_BATCH_SIZE}). Así la
 * página y el navegador no escriben esas cifras a mano.</p>
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
     * @return 25 MB, el de {@link FileChecker#MAX_FILE_SIZE}
     */
    public DataSize maxFileSize() {
        return FileChecker.MAX_FILE_SIZE;
    }

    /**
     * Tamaño máximo para los textos de la página.
     *
     * @return {@code 25 MB}
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
