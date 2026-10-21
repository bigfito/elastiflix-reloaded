package com.bigfito.elastiflix.web.ingest;

import java.io.IOException;
import java.io.InputStream;

import com.bigfito.elastiflix.ingest.file.CheckedFile;
import com.bigfito.elastiflix.ingest.file.FileCandidate;
import com.bigfito.elastiflix.ingest.file.FileChecker;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Repite en el servidor la comprobación de extensión, tamaño y UTF-8 antes de crear un trabajo.
 */
@Component
public class UploadCheck {

    private final FileChecker checker = new FileChecker();

    /**
     * Comprueba nombre, tamaño y UTF-8. Si algo no cumple, lanza su {@code EFX-ING-*}.
     *
     * @param file archivo subido
     * @return nombre y tipo, json o csv
     * @throws IOException si no se puede leer el contenido
     */
    public CheckedFile requireAccepted(MultipartFile file) throws IOException {
        try (InputStream content = file.getInputStream()) {
            FileCandidate candidate = new FileCandidate(file.getOriginalFilename(), file.getSize(), content);
            return checker.check(candidate);
        }
    }
}
