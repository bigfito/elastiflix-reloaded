package com.bigfito.elastiflix.ingest.job;

import java.nio.file.Path;

/**
 * El archivo de un trabajo tal como llegó: nombre original, copia local y tamaño.
 *
 * @param fileName nombre original, sin ruta
 * @param path     archivo local
 * @param size     bytes del archivo subido
 */
record JobFile(String fileName, Path path, long size) {
}
