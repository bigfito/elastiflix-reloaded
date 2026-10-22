package com.bigfito.elastiflix.ingest.file;

import org.springframework.util.unit.DataSize;

import com.bigfito.elastiflix.core.error.ErrorCode;
import com.bigfito.elastiflix.core.error.IngestValidationException;

/**
 * Repite en el servidor las comprobaciones de extensión, tamaño y UTF-8.
 *
 * <p>El orden es el del diseño: primero el nombre, después el tamaño y al final el contenido.
 * Un archivo demasiado grande no se lee.</p>
 */
public final class FileChecker {

    /**
     * Tamaño máximo de un archivo de carga: 25 MB, es decir, 26 214 400 bytes.
     *
     * <p>Es fijo y es el único origen del tope: de aquí salen la comprobación del navegador, la del servidor, el
     * multipart de la web y los textos de las páginas.</p>
     */
    public static final DataSize MAX_FILE_SIZE = DataSize.ofMegabytes(25);

    private static final String JSON = "json";
    private static final String CSV = "csv";
    private static final int ONE_DOT = 1;

    /**
     * Comprueba el archivo y devuelve su tipo.
     *
     * @param candidate nombre, tamaño y contenido
     * @return archivo aceptado
     */
    public CheckedFile check(FileCandidate candidate) {
        String name = fileName(candidate.fileName());
        FileKind kind = kindOf(name);
        requireSize(candidate.sizeInBytes());
        Utf8Decoder.requireUtf8(candidate.content());
        return new CheckedFile(name, kind);
    }

    private static String fileName(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IngestValidationException(ErrorCode.BAD_EXTENSION);
        }
        int slash = Math.max(raw.lastIndexOf('/'), raw.lastIndexOf('\\'));
        if (slash >= 0) {
            return raw.substring(slash + 1);
        }
        return raw;
    }

    private static FileKind kindOf(String name) {
        int dots = countDots(name);
        if (dots > ONE_DOT) {
            throw new IngestValidationException(ErrorCode.DOUBLE_EXTENSION);
        }
        if (dots < ONE_DOT) {
            throw new IngestValidationException(ErrorCode.BAD_EXTENSION);
        }
        return suffix(name);
    }

    private static int countDots(String name) {
        int dots = 0;
        for (int index = 0; index < name.length(); index++) {
            if (name.charAt(index) == '.') {
                dots++;
            }
        }
        return dots;
    }

    private static FileKind suffix(String name) {
        String extension = name.substring(name.lastIndexOf('.') + 1);
        if (extension.equalsIgnoreCase(JSON)) {
            return FileKind.JSON;
        }
        if (extension.equalsIgnoreCase(CSV)) {
            return FileKind.CSV;
        }
        throw new IngestValidationException(ErrorCode.BAD_EXTENSION);
    }

    private static void requireSize(long sizeInBytes) {
        if (sizeInBytes > MAX_FILE_SIZE.toBytes()) {
            throw new IngestValidationException(ErrorCode.FILE_TOO_LARGE);
        }
    }
}
