package com.bigfito.elastiflix.ingest.file;

import java.io.InputStream;

/**
 * Archivo que alguien quiere cargar, antes de comprobarlo.
 *
 * <p>El tamaño llega aparte del contenido. Así una prueba de «25 MB + 1 byte» no tiene que
 * construir ese archivo ni leerlo.</p>
 *
 * @param fileName    nombre original, no la ruta temporal
 * @param sizeInBytes bytes declarados
 * @param content     bytes del archivo; solo se leen si el nombre y el tamaño ya pasaron
 */
public record FileCandidate(String fileName, long sizeInBytes, InputStream content) {
}
