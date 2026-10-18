package com.bigfito.elastiflix.web.ingest;

import java.io.IOException;
import java.io.InputStream;

import com.bigfito.elastiflix.core.config.ElastiflixProperties;
import com.bigfito.elastiflix.ingest.file.FileCandidate;
import com.bigfito.elastiflix.ingest.file.FileChecker;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

/**
 * Repite en el servidor la comprobación de extensión, tamaño y UTF-8 antes de crear un trabajo.
 */
@Component
public class UploadCheck {

    private final FileChecker checker = new FileChecker();
    private final DataSize maxFileSize;

    /**
     * Usa el tamaño máximo configurado, o 150 MB.
     *
     * @param properties configuración, si el contexto la creó
     */
    public UploadCheck(ObjectProvider<ElastiflixProperties> properties) {
        ElastiflixProperties found = properties.getIfAvailable();
        if (found == null) {
            this.maxFileSize = ElastiflixProperties.Ingest.fallback().maxFileSize();
        } else {
            this.maxFileSize = found.ingest().maxFileSize();
        }
    }

    /**
     * Rechaza el archivo con el código de la carga si no es .json o .csv válido.
     *
     * @param file archivo subido
     * @throws IOException si no se puede abrir
     */
    public void requireAccepted(MultipartFile file) throws IOException {
        try (InputStream content = file.getInputStream()) {
            FileCandidate candidate = new FileCandidate(file.getOriginalFilename(), file.getSize(), content);
            checker.check(candidate, maxFileSize);
        }
    }
}
