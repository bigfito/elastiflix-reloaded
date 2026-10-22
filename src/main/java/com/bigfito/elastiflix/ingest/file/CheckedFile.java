package com.bigfito.elastiflix.ingest.file;

/**
 * Archivo que ya pasó las comprobaciones de nombre, tamaño y UTF-8.
 *
 * @param fileName nombre original
 * @param kind     json o csv
 */
public record CheckedFile(String fileName, FileKind kind) {
}
